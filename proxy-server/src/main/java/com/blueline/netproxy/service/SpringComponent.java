package com.blueline.netproxy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;



/**
 * @author Baili
 */
@Component
public class SpringComponent implements ApplicationContextAware {

    Logger logger= LoggerFactory.getLogger(SpringComponent.class);

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(SpringComponent.applicationContext == null) {
            SpringComponent.applicationContext = applicationContext;
        }
        logger.info("Config: applicationContext successful ,in a normal class can be called SpringUtils.getAppContext() get applicationContext object,applicationContext="+ SpringComponent.applicationContext);
    }


    private static ApplicationContext getApplicationContext() {
        return applicationContext;
    }


    public static Object getBean(String name){
        return getApplicationContext().getBean(name);
    }


    static <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }


    public static <T> T getBean(String name,Class<T> clazz){
        return getApplicationContext().getBean(name, clazz);
    }

    public static <T> T getProperty(String name,Class<T> clazz,T def){
        return getApplicationContext().getEnvironment().getProperty(name,clazz,def);
    }

}