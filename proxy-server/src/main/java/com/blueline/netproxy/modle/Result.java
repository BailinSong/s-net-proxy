package com.blueline.netproxy.modle;

/**
 * @author Baili
 */
public class Result {
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Result{" +
                "state='" + state + '\'' +
                '}';
    }

    String state;
}
