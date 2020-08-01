package com.example.springboot.entity;

import java.io.Serializable;

public class ReqQuery implements Serializable {
    private String userName;
    private String channelName;
    private String chainCode;
    private String fnc;
    private String[] args;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChainCode() {
        return chainCode;
    }

    public void setChainCode(String chainCode) {
        this.chainCode = chainCode;
    }

    public String getFnc() {
        return fnc;
    }

    public void setFnc(String fnc) {
        this.fnc = fnc;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }
}
