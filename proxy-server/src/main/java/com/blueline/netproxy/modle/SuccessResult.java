package com.blueline.netproxy.modle;

/**
 * @author Baili
 */
public class SuccessResult extends Result {
    @Override
    public String toString() {
        return "SuccessResult{" +
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

    public SuccessResult(Object data) {
        state = "0";
        this.data = data;
    }


}
