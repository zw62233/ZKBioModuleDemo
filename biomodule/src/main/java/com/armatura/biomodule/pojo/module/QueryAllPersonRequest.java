package com.armatura.biomodule.pojo.module;

/**
 * Created by Magic on 2020/9/8
 */
public class QueryAllPersonRequest {
    /**
     * page index
     * not necessary
     */
    private int pageIndex = 1;
    /**
     * page size
     * not necessary
     * if page size =0,the query all
     */
    private int pageSize = 20;

    public QueryAllPersonRequest(int pageIndex, int pageSize) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    public QueryAllPersonRequest() {
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
