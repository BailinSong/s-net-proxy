package com.blueline.netproxy.checker;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class IpChecker {

    /**
     *
     * @param requestIp 123.123.123.123
     * @param whiteIp [ 123.123.123.123\32 , 123.123.123.0\24 ,222.222.222.0\24]
     * @return true
     */
    public static  boolean judgeOr( String requestIp, List<String> whiteIp)
    {
        boolean result=false;
        for (String ip : whiteIp) {
            result|=judge(requestIp,ip);
        }
        return result;
    }
    /**
     *
     * @param requestIp 123.123.123.123
     * @param whiteIp [ 123.123.123.123\32 , 123.123.123.0\24 ,222.222.222.0\24]
     * @return false
     */
    public static  boolean judgeAnd( String requestIp, List<String> whiteIp)
    {
        boolean result=true;
        for (String ip : whiteIp) {
            result&=judge(requestIp,ip);
        }
        return result;
    }

    public static  boolean judge(String requestIp, String whiteIp)
    {
       String [] whiteIpInfo= whiteIp.split("\\\\");
        int mask=32;
        if(whiteIpInfo.length==2){
           try {
               mask = Integer.parseInt(whiteIpInfo[1]);
           }catch (Exception e){
           }
        }
        return judge(requestIp,whiteIpInfo[0],mask);


    }

    private static boolean judge(String requestIp, String whiteIp, int mask)
    {
        int rip = ip2Int(requestIp);
        int wip = ip2Int(whiteIp);
        int result = rip ^ wip;
        int m = 0;
        for (int i = 0; i < 32; i ++)
        {
            if (31 - i >= mask) {
                m = m | (1 << i);
            }
        }
        int n = m | result;
        return (m == n);
    }

    private static int ip2Int(String s)
    {
        String[] arr = s.split("\\.");
        int len = arr.length;
        if (len == 1)
        {
            return Integer.parseInt(s);
        }
        int[] arrInt = new int[len];
        for (int i = 0; i < len; i ++)
        {
            arrInt[i] = ip2Int(arr[i]);
        }
        int total = 0;
        for (int i = 0; i < len; i ++)
        {
            total = total * 256 + arrInt[i];
        }
        return total;
    }
}
