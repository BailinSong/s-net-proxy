package com.blueline.netproxy.service;

/**
 * @author Baili
 */
public interface IUserService {
    /**
     * Verify that the user and password are correct
     * @param user User Name
     * @param pwd User password
     * @return is it right or not
     */
    boolean verify(String user, String pwd);
}
