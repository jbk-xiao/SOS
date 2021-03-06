package com.trace.trace.service;

import com.trace.trace.dao.MongoDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SearchGraph {
    private final MongoDao mongodbDao;

    public SearchGraph(MongoDao mongodbDao) {
        this.mongodbDao = mongodbDao;
    }

    public String searchGraphByKind(String kind) {
        return mongodbDao.getGraphByKind(kind);
    }

    public String searchGraphByBrand(String brand) {
        return mongodbDao.getGraphByBrand(brand);
    }
}
