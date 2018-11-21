package com.blueline.netproxy.mode;

import java.util.ArrayList;
import java.util.List;

public class RuleMapping {

    int id;



    String name;
    String protocol;
    String host;
    int port;
    String path;
    String realHost;
    int realPort;
    String realPath;
    List<String> whiteList=new ArrayList<>();

    public RuleMapping(){}
    public RuleMapping(int id, String protocol, String host, int port, String path, String realHost, int realPort, String realPath) {
        this.id = id;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.realHost = realHost;
        this.realPort = realPort;
        this.realPath = realPath;
    }

    public static RuleMapping getRule(int id, String protocol, String host, int port, String path, String realHost, int realPort, String realPath) {
        return new RuleMapping(id, protocol, host, port, path, realHost, realPort, realPath);
    }

    public static RuleMapping getRule(int id, String protocol, String host, int port, String realHost, int realPort) {
        return new RuleMapping(id, protocol, host, port, null, realHost, realPort, null);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRealHost() {
        return realHost;
    }

    public void setRealHost(String realHost) {
        this.realHost = realHost;
    }

    public String getRealPath() {
        return realPath;
    }

    public void setRealPath(String realPath) {
        this.realPath = realPath;
    }

    public int getRealPort() {
        return realPort;
    }

    public void setRealPort(int realPort) {
        this.realPort = realPort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public String toString() {
        return "RuleMapping{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                ", realHost='" + realHost + '\'' +
                ", realPort=" + realPort +
                ", realPath='" + realPath + '\'' +
                ", whiteList=" + whiteList +
                '}';
    }
}