package com.github.ontio.model;

public class Current {
    private Integer height;

    private Integer txncount;

    private Integer ontidcount;

    private Integer nonontidtxncount;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getTxncount() {
        return txncount;
    }

    public void setTxncount(Integer txncount) {
        this.txncount = txncount;
    }

    public Integer getOntidcount() {
        return ontidcount;
    }

    public void setOntidcount(Integer ontidcount) {
        this.ontidcount = ontidcount;
    }

    public Integer getNonontidtxncount() {
        return nonontidtxncount;
    }

    public void setNonontidtxncount(Integer nonontidtxncount) {
        this.nonontidtxncount = nonontidtxncount;
    }
}