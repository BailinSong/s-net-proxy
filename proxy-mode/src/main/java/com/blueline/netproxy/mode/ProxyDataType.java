package com.blueline.netproxy.mode;

public class ProxyDataType {
    public static final byte LOGIN = 1;
    public static final byte PROXY_DATA = 0;
    public static final byte RULES_DATA = -1;
    public static final byte ENDPOINT_OPEN = 2;
    public static final byte ENDPOINT_CLOSE = -2;
    public static final byte PING = 3;
    public static final byte PONG = -3;
}
