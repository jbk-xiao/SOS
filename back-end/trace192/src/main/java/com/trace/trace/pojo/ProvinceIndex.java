package com.trace.trace.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.trace.trace.entity.ProvinceIndexData;
import lombok.Data;

@Data
public class ProvinceIndex {
    @Expose
    @SerializedName("keyword")
    private String keyword;
    @Expose
    @SerializedName("period")
    private String period;
    @Expose
    @SerializedName("data")
    private ProvinceIndexData[] provinceIndexData;

    public ProvinceIndex(String keyword, String period) {
        this.keyword = keyword;
        this.period = period;
    }
}
