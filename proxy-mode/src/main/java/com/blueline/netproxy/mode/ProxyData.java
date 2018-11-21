package com.blueline.netproxy.mode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * @author Baili
 */
public class ProxyData {
    byte type;
    int rule;
    InetSocketAddress remoteAddress;
    byte[] data;

    public static ProxyData build(byte type, int rule, InetSocketAddress remoteAddress, byte[] bytes) {
        ProxyData proxyData = new ProxyData();
        proxyData.type = type;
        proxyData.rule = rule;
        proxyData.remoteAddress = remoteAddress;
        proxyData.data = bytes;
        return proxyData;
    }

    public static ProxyData build(byte[] bytes) {
        ProxyData proxyData = new ProxyData();
        try {
            proxyData.type = bytes[0];
            proxyData.rule = bytes2Int(bytes, 1);
            proxyData.remoteAddress = new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(bytes, 5, 9)), bytes2Int(bytes, 9));
            proxyData.data = new byte[bytes2Int(bytes, 13)];
            System.arraycopy(bytes, 17, proxyData.data, 0, proxyData.data.length);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return proxyData;
    }

    public static int bytes2Int(byte[] bytes, int offset) {
        int num = bytes[offset + 3] & 0xFF;
        num |= ((bytes[offset + 2] << 8) & 0xFF00);
        num |= ((bytes[offset + 1] << 16) & 0xFF0000);
        num |= ((bytes[offset + 0] << 24) & 0xFF0000);
        return num;
    }

    public static byte[] int2ByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    public byte[] getBytes() {
        byte[] data = new byte[1 + 4 + 4 + 4 + 4 + this.data.length];
        data[0] = type;
        System.arraycopy(int2ByteArray(rule), 0, data, 1, 4);
        System.arraycopy(remoteAddress.getAddress().getAddress(), 0, data, 5, 4);
        System.arraycopy(int2ByteArray(remoteAddress.getPort()), 0, data, 9, 4);
        System.arraycopy(int2ByteArray(this.data.length), 0, data, 13, 4);
        System.arraycopy(this.data, 0, data, 17, this.data.length);
        return data;
    }

    public byte[] getData() {
        return data;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public int getRule() {
        return rule;
    }

    public byte getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ProxyData{" +
                "type=" + type +
                ", rule=" + rule +
                ", remoteAddress=" + remoteAddress +
                ", data=" + Arrays.toString(data) +
                '}';
    }

}
