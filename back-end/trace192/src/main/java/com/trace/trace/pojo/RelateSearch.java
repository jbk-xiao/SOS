package com.trace.trace.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.trace.trace.entity.RelateSearchData;
import lombok.Data;

@Data
public class RelateSearch {
    @Expose
    @SerializedName("keyword")
    private String keyword;
    @Expose
    @SerializedName("period")
    private String period;
    @Expose
    @SerializedName("data")
    private RelateSearchData[] relateSearchData;

    public RelateSearch(String keyword, String period) {
        this.keyword = keyword;
        this.period = period;
    }
}
