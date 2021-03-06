package com.trace.fabric.fabtrace;


import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.trace.fabric.fabtrace.datatype.ProcessInfo;
import com.trace.fabric.fabtrace.datatype.TraceInfo;
import com.trace.fabric.fabtrace.datatype.TraceManagerInfo;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jbk-xiao
 * @program fabtrace
 * @packagename com.trace.fabric.fabtrace
 * @Description
 * @create 2021-02-04-11:21
 */
@Contract(
        name = "FabTrace",
        info = @Info(
                title = "FabTrace contract",
                description = "The process of produce goods.",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "media@example.com",
                        name = "fab trace",
                        url = "http://process.trace.com")))
@Default
public final class FabTrace implements ContractInterface {
    private final Gson gson = new Gson();

    private enum FabTraceError {
        //
        ID_NOT_FOUND,
        PRODUCT_HAVE_LEFT,
        WRONG_PROCESS
    }

    enum InitData {
        //
        TIME(1611970163415L), DURATION(432000L), PROCESS_COUNT(4), ID_END(13);
        private final long value;

        InitData(final long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }
    }

    @Transaction()
    public void initLedger(final Context context) {
        ChaincodeStub stub = context.getStub();
        long time;
        time = InitData.TIME.value;
        long duration;
        duration = InitData.DURATION.value;
        TraceInfo info;
        info = new TraceInfo("16119701634150000", "520102000400793", "????????????", "275g", "?????????",
                (int) InitData.PROCESS_COUNT.value);
        time += duration;
        info.addProcessInfo("??????????????????", "??????", time + "", "??????");
        time += duration;
        info.addProcessInfo("??????", "??????", time + "", "??????");
        info.getProcess().get(1).addProcedureInfo("???????????????", "?????????", time + "");
        time += duration;
        info.getProcess().get(1).addProcedureInfo("??????", "?????????", time + "");
        time += duration;
        info.getProcess().get(1).addProcedureInfo("??????", "?????????", time + "");
        time += duration;
        info.getProcess().get(1).addProcedureInfo("????????????", "?????????", time + "");
        time += duration;
        info.addProcessInfo("?????????", "??????", time + "", "??????");
        time += duration;
        info.addProcessInfo("???????????????", "??????", time + "", "??????");
        stub.putStringState(info.getId(), gson.toJson(info));
        info = new TraceInfo("16119701634151111", "520102000400793", "????????????", "275g", "?????????",
                (int) InitData.PROCESS_COUNT.value);
        time += duration;
        info.addProcessInfo("??????????????????", "??????", time + "", "?????????");
        time += duration;
        info.addProcessInfo("??????", "??????", time + "", "??????");
        time += duration;
        info.addProcessInfo("?????????", "??????", time + "", "??????");
        time += duration;
        info.addProcessInfo("???????????????", "??????", time + "", "??????");
        stub.putStringState(info.getId(), gson.toJson(info));
        info = new TraceInfo("16119701634152222", "520102000400793", "????????????", "275g", "?????????",
                (int) InitData.PROCESS_COUNT.value);
        time += duration;
        info.addProcessInfo("??????????????????", "??????", time + "", "");
        time += duration;
        info.addProcessInfo("??????", "??????", time + "", "");
        time += duration;
        info.addProcessInfo("?????????", "??????", time + "", "");
        time += duration;
        info.addProcessInfo("???????????????", "??????", time + "", "");
        stub.putStringState(info.getId(), gson.toJson(info));
        TraceInfo dueInfo = new TraceInfo("123456", "520102000400793", "????????????", "275g", "?????????",
                (int) InitData.PROCESS_COUNT.value);
        dueInfo.addProcessInfo("??????", "??????", InitData.TIME.value + "", "");
        dueInfo.addProcessInfo("??????", "??????", InitData.TIME.value + "", "");
        stub.putStringState(dueInfo.getId(), gson.toJson(dueInfo));
    }

    @Transaction()
    public String createFood(final Context context, final String id, final String com, final String foodName,
                             final String specification, final String category, final Integer processCount) {
        ChaincodeStub stub = context.getStub();
        TraceInfo info = new TraceInfo(id, com, foodName, specification, category, processCount);
        String infoStr = gson.toJson(info);
        stub.putStringState(id, infoStr);
        return infoStr;
    }

    @Transaction()
    public String addProcess(final Context context, final String id, final String name, final String master,
                             final String time, final String location) {
        ChaincodeStub stub = context.getStub();
        TraceInfo info;

        String traceState = stub.getStringState(id);
        if (traceState.isEmpty()) {
            String errorMessage = String.format("?????????id???%s????????????", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, FabTraceError.ID_NOT_FOUND.toString());
        } else {
            info = gson.fromJson(traceState, TraceInfo.class);
            info.addProcessInfo(name, master, time, location);
        }
        traceState = gson.toJson(info);
        stub.putStringState(id, traceState);
        return traceState;
    }

    @Transaction()
    public String addProcedure(final Context context, final String id, final String pname,
                               final String pmaster, final String ptime) {
        ChaincodeStub stub = context.getStub();
        TraceInfo info;
        String traceState = stub.getStringState(id);
        if (traceState.isEmpty()) {
            String errorMessage = String.format("?????????id???%s????????????", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, FabTraceError.ID_NOT_FOUND.toString());
        }
        info = gson.fromJson(traceState, TraceInfo.class);
        if (info.getProcess().size() < 2) {
            String errorMessage = String.format("id???%s??????????????????????????????", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, FabTraceError.WRONG_PROCESS.toString());
        }
        info.getProcess().get(1).addProcedureInfo(pname, pmaster, ptime);
        traceState = gson.toJson(info);
        stub.putStringState(id, traceState);
        return traceState;
    }

    @Transaction()
    public String queryInfoByID(final Context context, final String id) {
        ChaincodeStub stub = context.getStub();
        TraceInfo info;

        String traceState = stub.getStringState(id);
        if (traceState.isEmpty()) {
            String errorMessage = String.format("?????????????????????%s???????????????", id);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, FabTraceError.ID_NOT_FOUND.toString());
        } else {
            info = gson.fromJson(traceState, TraceInfo.class);
        }
        return gson.toJson(info);
    }

    @Transaction()
    public String queryInfosByIDs(final Context context, final String... ids) {
        ChaincodeStub stub = context.getStub();
        List<TraceInfo> queryResults = new ArrayList<>();
        TraceInfo info;
        String traceState;
        for (String id : ids) {
            traceState = stub.getStringState(id);
            if (traceState.isEmpty()) {
                info = new TraceInfo(id, "", "", "", "", 0);
            } else {
                info = gson.fromJson(traceState, TraceInfo.class);
            }
            queryResults.add(info);
        }
        return gson.toJson(queryResults);
    }

    @Transaction()
    public String managerQueryInfos(final Context context, final String... ids) {
        ChaincodeStub stub = context.getStub();
        List<TraceManagerInfo> queryResults = new ArrayList<>();
        TraceInfo info;
        TraceManagerInfo managerInfo;
        String traceState;
        List<ProcessInfo> list;
        ProcessInfo latestProcess;
        for (String id : ids) {
            traceState = stub.getStringState(id);
            if (traceState.isEmpty()) {
                managerInfo = new TraceManagerInfo(id, "", "");
            } else {
                info = gson.fromJson(traceState, TraceInfo.class);
                list = info.getProcess();
                latestProcess = list.get(list.size() - 1);
                managerInfo = new TraceManagerInfo(id, latestProcess.getName(), latestProcess.getTime());
            }
            queryResults.add(managerInfo);
        }
        return gson.toJson(queryResults);
    }

    @Transaction()
    public String queryHistoryByID(final Context context, final String id) {
        ChaincodeStub stub = context.getStub();
        String s = stub.getTxTimestamp().toString();
        QueryResultsIterator<KeyModification> historyForKey = stub.getHistoryForKey(id);
        s += Iterables.toString(historyForKey);
        return s;
    }

    @Transaction()
    public String queryInfoByEndRange(final Context context, final String end) {
        String start = end.substring(0, (int) InitData.ID_END.value) + "000000000000";
        return queryInfoByRange(context, start, end);
    }

    @Transaction()
    public String queryInfoByRange(final Context context, final String start, final String end) {
        ChaincodeStub stub = context.getStub();
        List<TraceInfo> queryResults = new ArrayList<>();
        QueryResultsIterator<KeyValue> results = stub.getStateByRange(start, end);
        for (KeyValue result: results) {
            TraceInfo info = gson.fromJson(result.getStringValue(), TraceInfo.class);
            queryResults.add(info);
        }
        return gson.toJson(queryResults);
    }

    @Transaction()
    public String queryAllInfo(final Context context) {
        ChaincodeStub stub = context.getStub();
        List<TraceInfo> queryResults = new ArrayList<>();

        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");
        for (KeyValue result: results) {
            TraceInfo info = gson.fromJson(result.getStringValue(), TraceInfo.class);
            queryResults.add(info);
        }

        return gson.toJson(queryResults);
    }
}
