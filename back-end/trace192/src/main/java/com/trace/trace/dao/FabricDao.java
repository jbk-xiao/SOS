package com.trace.trace.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.trace.trace.config.RedisIndexConfig;
import com.trace.trace.pojo.FabMediaInfo;
import com.trace.trace.pojo.ProcedureInfo;
import com.trace.trace.pojo.ProcessInfo;
import com.trace.trace.pojo.TraceInfo;
import com.trace.trace.pojo.TraceManagerInfo;
import com.trace.trace.util.CreateTraceCode;
import com.trace.trace.util.JedisUtil;
import com.trace.trace.util.QRCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Repository
public class FabricDao {
    private final JedisUtil jedisUtil;
    private final Network network;
    private final QRCodeUtil qrCodeUtil;
    private final CreateTraceCode createTraceCode;

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Map<String, Integer> dbMap;
    Set<String> databaseSet;

    @Value("${media.video.path}")
    String path;

    @Autowired
    public FabricDao(RedisIndexConfig redisIndexConfig, JedisUtil jedisUtil, Network network, QRCodeUtil qrCodeUtil,
                     CreateTraceCode createTraceCode) {
        this.jedisUtil = jedisUtil;
        this.network = network;
        this.qrCodeUtil = qrCodeUtil;
        this.createTraceCode = createTraceCode;
        dbMap = redisIndexConfig.getMap();
        databaseSet = dbMap.keySet();
    }

    /**
     * 接收文件类型、文件名称、文件md5码三个参数，将其存储至fabric。
     *
     * @param filetype mp4, mkv, jpg
     * @param filename 带有扩展名的文件名称
     * @param md5code  md5码
     */
    public void saveMedia(String filetype, String filename, String md5code) {
        Contract contract = network.getContract(FabricInfo.MEDIA_CC.value);

        String type = "jpg".equals(filetype) ? 1 + "" : 2 + "";
        String checkTime = simpleDateFormat.format(System.currentTimeMillis());

        try {
            contract.submitTransaction("addMedia", type, filename, md5code, checkTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用区块链中数据查看文件自生成以来是否被修改过。
     *
     * @param filename 带有扩展名的文件basename。
     * @return boolean
     */
    public boolean isModified(String filename) {
        Gson gson = new Gson();
        try {
            String currentCode = DigestUtils.md5DigestAsHex(new FileInputStream(path + File.separator + filename));
            Contract contract = network.getContract(FabricInfo.MEDIA_CC.value);

            String result = new String(contract.evaluateTransaction("queryMedia", filename));

            FabMediaInfo mediaInfo = gson.fromJson(result, FabMediaInfo.class);
            return currentCode.equals(mediaInfo.getMd5code());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 查询fabric，返回溯源信息，包括大阶段和在工厂内的阶段。
     *
     * @param originId 唯一溯源码
     * @return 溯源信息json。考虑是否通过公司名称在redis中查询公司其它信息并加入traceInfo对象
     */
    public TraceInfo getInfoByOriginId(String originId) {
        Jedis jedis = jedisUtil.getClient();
        TraceInfo traceInfo = new TraceInfo(originId);
        String traceInfoStr = "{}";
        String processName;
        String picturePrefix = FabricInfo.PICTURE_PREFIX.value;
        String pictureNoFound = FabricInfo.PICTURE_NO_FOUND.value;
        StringBuilder sb;
        try {
            Contract contract = network.getContract(FabricInfo.TRACE_CC.value);
            traceInfoStr = new String(contract.evaluateTransaction("queryInfoByID", originId));
            traceInfo = gson.fromJson(traceInfoStr, TraceInfo.class);
            String id = traceInfo.getId();
            String picture = pictureNoFound;
            String latestPic;
            for (ProcessInfo process : traceInfo.getProcess()) {
                processName = process.getName();
                for (ProcedureInfo procedure : process.getProcedure()) {
                    sb = new StringBuilder(processName);
                    sb.append("_").append(procedure.getName());
                    if (databaseSet.contains(sb.toString())) {
                        jedis.select(dbMap.get(sb.toString()));
                        latestPic = jedis.lindex(id, 0);
                        picture = (latestPic == null) ? pictureNoFound : latestPic;
                    }
                    procedure.setPicture(picturePrefix + picture);
                }
                if (databaseSet.contains(processName)) {
                    jedis.select(dbMap.get(processName));
                    latestPic = jedis.lindex(id, 0);
                    picture = (latestPic == null) ? pictureNoFound : latestPic;
                }
                process.setPicture(picturePrefix + picture);
            }
//            traceInfoStr = gson.toJson(traceInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        jedis.close();
        return traceInfo;
    }

    /**
     * 接收产品基本信息+第一个process的所有信息，利用产品基本信息生成id并使用id和基本信息在区块链中创建产品
     * 而后调用 addProcess 方法存储第一个process的信息。
     *
     * @param foodType     油辣椒酱-275g-辣椒酱(foodName-specification-category)
     * @param com          公司名称
     * @param processCount 4
     * @param name         菜籽油生产地
     * @param master       负责人名称
     * @param location     该process所在城市
     * @return traceInfo，带有qrCode字段。
     * @see #addProcess(String id, String name, String master, String location)
     */
    public String addFirstProcess(String foodType, String com, Integer processCount, String name, String master,
                                  String location) {
        //生成溯源码
        String id = createTraceCode.getTraceCode(foodType, com);

        //将生成的溯源码id存入redis4号库，从左侧存入，因此较小索引为较新的id
        Jedis jedis = jedisUtil.getClient();
        jedis.select(4);
        jedis.lpush(id.substring(0, 13), id);
        jedis.close();

        String[] food = foodType.split("-");
        //依据生成的id和拆分过的食品基本信息调用区块链的createFood方法生成新的食品记录
        try {
            Contract contract = network.getContract(FabricInfo.TRACE_CC.value);
            contract.submitTransaction("createFood", id, com, food[0], food[1], food[2], processCount + "");
        } catch (Exception e) {
            log.error(e.toString());
        }
        //调用addProcess方法将首个process加入区块链
        String infoStr = addProcess(id, name, master, location);
        TraceInfo info = gson.fromJson(infoStr, TraceInfo.class);
        //依据指定名称生成二维码并补全链接
        String qrName = id + "inner";
        qrCodeUtil.addCode("http://180.76.249.27/Trace/TRACE/#/AddProcess/" + id, qrName);
        info.setQrCode(FabricInfo.PICTURE_PREFIX.value + qrName + ".png");
        return gson.toJson(info);
    }

    /**
     * 接收产品某个具体process的信息，将其存入fabric。
     *
     * @param id       唯一溯源码
     * @param name     process名称
     * @param master   该process负责人
     * @param location 该process所在城市
     * @return 溯源码为id批次的产品所对应的TraceInfo的json字符串。包含二维码链接。
     */
    public String addProcess(String id, String name, String master, String location) {
        String infoStr = "";
        try {
            Contract contract = network.getContract(FabricInfo.TRACE_CC.value);

            byte[] bytes = contract.submitTransaction("addProcess", id, name, master,
                    simpleDateFormat.format(System.currentTimeMillis()), location);
            infoStr = new String(bytes);
            TraceInfo info = gson.fromJson(infoStr, TraceInfo.class);
            if (info.getProcessCount() == info.getProcess().size()) {
                qrCodeUtil.addCode("http://180.76.249.27/Trace/TRACE/#/Origin?query=" + id, id);
                info.setQrCode(FabricInfo.PICTURE_PREFIX.value + id + ".png");
                infoStr = gson.toJson(info);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return infoStr;
    }

    /**
     * 接收产品某个具体procedure信息，将其存入fabric。
     *
     * @param id     产品唯一溯源码
     * @param name   procedure名称
     * @param master procedure负责人
     * @return 溯源码为id批次的产品所对应的TraceInfo的json字符串。包含二维码链接。
     */
    public String addProcedure(String id, String name, String master) {
        String info = "";
        try {
            Contract contract = network.getContract(FabricInfo.TRACE_CC.value);
            info = new String(contract.submitTransaction("addProcedure", id, name, master,
                    simpleDateFormat.format(System.currentTimeMillis()) + ""));
        } catch (Exception e) {
            log.error(e.toString());
        }

        Jedis jedis = jedisUtil.getClient();
        int db = dbMap.get("工厂_" + name);
        jedis.select(db);
        jedis.lpush("latestCode", id);
        jedis.close();
        return info;
    }

    /**
     * 依据传入的id获取管理员界面产品列表，即TraceManagerList的列表。
     *
     * @param ids id列表
     * @return List<TraceManagerInfo>
     */
    public List<TraceManagerInfo> getManagerInfoList(List<String> ids) {
        StringBuilder idList = new StringBuilder("[");
        for (String id : ids) {
            idList.append(id).append(",");
        }
        int end = idList.lastIndexOf(",");
        if (end == -1) {
            idList.append("]");
        } else {
            idList.replace(end, end, "]");
        }
        String infoList = "";
        try {
            Contract contract = network.getContract(FabricInfo.TRACE_CC.value);
            infoList = new String(contract.evaluateTransaction("managerQueryInfos", idList.toString()));
        } catch (Exception e) {
            log.error(e.toString());
        }
        return gson.fromJson(infoList, new TypeToken<List<TraceManagerInfo>>() {
        }.getType());
    }

    private enum FabricInfo {
        //
        MEDIA_CC("fabmedia"),
        TRACE_CC("fabtrace"),
        PICTURE_PREFIX("http://121.46.19.26:8511/getPicture/"),
        PICTURE_NO_FOUND("picture_no_found.jpg");

        private final String value;

        FabricInfo(final String value) {
            this.value = value;
        }
    }
}
