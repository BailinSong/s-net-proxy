package com.blueline.netproxy.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.blueline.netproxy.mode.RuleMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Baili
 */
@Service
public class RuleService {


    static Logger logger = LoggerFactory.getLogger(RuleService.class);

    private static Map<String, ConcurrentHashMap<Integer, RuleMapping>> rules = new ConcurrentHashMap<>();

    private static LinkedHashMap<String, RuleMapping> routingTable = new LinkedHashMap<>();
    private final String prefix = "/";
    private final String splitterChar = ":";

    @Value("${rules.path}")
    String dataFile;

    @Autowired
    IProxyServer proxyServer;

    @PostConstruct
    public void flushTable() {
        if (rules.size() == 0) {
            String jsonText = readFile(dataFile);

            try {
                rules = JSON.parseObject(jsonText, new TypeReference<ConcurrentHashMap<String, ConcurrentHashMap<Integer, RuleMapping>>>() {
                });
            }catch (Exception e){
                logger.warn(rules.toString(),e);
            }
            logger.debug(rules.toString());

        }
        writeFile(dataFile, JSON.toJSONString(rules, true));
        routingTable = generateRoutingTable();
    }

    private LinkedHashMap<String, RuleMapping> generateRoutingTable() {

        final LinkedHashMap<String, RuleMapping> tempTable = new LinkedHashMap<>();

        Map<String, RuleMapping> temp = new LinkedHashMap<>();
        rules.forEach(
                (k, v) -> v.forEach(
                        (id, rule) -> {
                            String key = prefix + rule.getHost() + splitterChar + rule.getPort();
                            if ("http".equalsIgnoreCase(rule.getProtocol())&&rule.getPath()!=null) {

                                if (rule.getPath().startsWith(prefix)) {
                                    key += rule.getPath();
                                } else {
                                    key += prefix + rule.getPath();
                                }
                            }
                            temp.put(key, rule);
                        }
                )
        );

        temp.entrySet().stream().sorted((o1, o2) -> {
            if (o1.getKey().length() == o2.getKey().length()) {
                return 0;
            }
            return o1.getKey().length() > o2.getKey().length() ? 1 : -1;
        }).forEach(x ->
                tempTable.put(x.getKey(), x.getValue())
        );

        return tempTable;

    }

    RuleMapping getRule(SocketAddress address) {
        return getRule(address, null);
    }

    RuleMapping getRule(SocketAddress address, String path) {

        String key = address.toString();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith(prefix)) {
                key += path;
            } else {
                key += prefix + path;
            }

        }
        for (Map.Entry<String, RuleMapping> mappingEntry : routingTable.entrySet()) {
            if (key.startsWith(mappingEntry.getKey())) {
                return mappingEntry.getValue();
            }
        }
        key = "/0.0.0.0:" + ((InetSocketAddress) address).getPort();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith(prefix)) {
                key += path;
            } else {
                key += prefix + path;
            }

        }
        for (Map.Entry<String, RuleMapping> mappingEntry : routingTable.entrySet()) {
            if (key.startsWith(mappingEntry.getKey())) {
                return mappingEntry.getValue();
            }
        }
        return null;
    }


    ConcurrentHashMap<Integer, RuleMapping> getProxyInfo(String user, String protocol) {
        try {
            ConcurrentHashMap<Integer, RuleMapping> fullInfoMap = getProxyInfo(user);
            Map<Integer, RuleMapping> infoMap =
                    fullInfoMap.entrySet()
                            .stream()
                            .filter(x -> x.getValue().getProtocol().equalsIgnoreCase(protocol))
                            .collect(Collectors.
                                    toMap(Map.Entry::getKey, Map.Entry::getValue));
            fullInfoMap = new ConcurrentHashMap<>(infoMap);
            return fullInfoMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public ConcurrentHashMap<Integer, RuleMapping> getProxyInfo(String user) {
        ConcurrentHashMap<Integer, RuleMapping> temp = rules.get(user);
        if(temp == null) {
            return new ConcurrentHashMap<>(0);
        }else {
            return rules.get(user);
        }
    }

    public synchronized void putProxyInfo(String user, RuleMapping ruleMapping) {

        ConcurrentHashMap<Integer, RuleMapping> rule = rules.get(user);
        if (rule != null) {
            rule.put(ruleMapping.getId(), ruleMapping);
        } else {
            rule = new ConcurrentHashMap<>(1);
            rule.put(ruleMapping.getId(), ruleMapping);
            rules.put(user, rule);
        }
        flushTable();
        proxyServer.disconnect(user);

    }

    public synchronized void deleteProxyInfo(String user, int id) {

        ConcurrentHashMap<Integer, RuleMapping> rule = rules.get(user);
        if (rule != null) {
            rule.remove(id);
            flushTable();
        }
        proxyServer.disconnect(user);

    }


    private static void writeFile(String path, String str) {

        BufferedWriter writer = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8));
            writer.write(str);

        } catch (
                IOException e) {
            logger.warn("File write failed " + path, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.warn("File close failed " + path, e);
                }
            }
        }

    }

    private static String readFile(String path) {
        BufferedReader reader = null;
        StringBuilder result = new StringBuilder();
        try {
            File jsonFile=new File(path);
            if(!jsonFile.exists()){
                jsonFile.createNewFile();
            }
            FileInputStream fileInputStream = new FileInputStream(path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            reader = new BufferedReader(inputStreamReader);
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                result.append(tempString);
            }
            reader.close();
        } catch (IOException e) {

            logger.warn("File read failed " + path, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("File close failed " + path, e);
                }
            }
        }
        return result.toString();
    }


}
