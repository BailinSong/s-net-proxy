package com.blueline.netproxy.modle;

/**
 * @author Baili
 */
public class FailureResult extends Result {
    @Override
    public String toString() {
        return "FailureResult{" +
                "data=" + data +
                ", state='" + state + '\'' +
                '}';
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    Object data;

    public FailureResult(String errCode, Object data) {
        state = errCode;
        this.data = data;
    }


}
