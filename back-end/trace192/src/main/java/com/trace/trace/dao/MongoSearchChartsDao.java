package com.trace.trace.dao;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.trace.trace.util.MongoDBUtil;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Component;

import static com.mongodb.client.model.Filters.regex;

/**
 * @author jbk-xiao
 * @program trace192
 * @packagename com.trace.trace.dao
 * @Description
 * @create 2021-02-23-17:43
 */
@Slf4j
@Component
public class MongoSearchChartsDao {

    public String getPredictData(String companyName) {
        StringBuilder result = new StringBuilder();
        MongoClient mongoClient;
        mongoClient = MongoDBUtil.getConn();
        MongoCollection<Document> collection = mongoClient.getDatabase("trace")
                .getCollection("predict");
        for (Document document : collection.find(regex("company_name", companyName))) {
            String docJson = document.toJson();
            result.append(docJson);
        }
        return result.toString();
    }

    public String getNewsData(String companyName) {
        StringBuilder result = new StringBuilder();
        MongoClient mongoClient;
        mongoClient = MongoDBUtil.getConn();
        MongoCollection<Document> collection = mongoClient.getDatabase("trace")
                .getCollection("news");
        for (Document document : collection.find(regex("company_name", companyName))) {
            String docJson = document.toJson();
            result.append(docJson);
        }
        return result.toString();
    }
}
