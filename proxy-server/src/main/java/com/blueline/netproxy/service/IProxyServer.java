package com.blueline.netproxy.service;

/**
 * @author Baili
 */
public interface IProxyServer {
    void disconnect(String user);

    void start();
}
