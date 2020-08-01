package com.example.springboot.utils;

import com.jason.fabric.pool.FabricGatewayPool;
import com.jason.fabric.pool.api.FabricConnection;
import org.apache.commons.pool2.ObjectPool;

public class FabricCacheUtil {

    /**
     * 获取fabric 缓存网络
     * @return
     * @throws Exception
     */
    public static FabricConnection getConnect(String userName,String channel) throws Exception {
        ObjectPool<FabricConnection>  fGWP = new FabricGatewayPool(userName, channel);
        return fGWP.borrowObject();
    }
}
