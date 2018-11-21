package com.blueline.netproxy.service;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Baili
 */
@Service
public class UserService implements IUserService {

    @Value("${users}")
    String usersData;
    private Map<String,String> users;

    @PostConstruct
    public void init(){

        users=JSON.parseObject(usersData,ConcurrentHashMap.class);

    }
    
    @Override
    public boolean verify(String user, String pwd){
        return pwd.equals(users.get(user));
    }

}
