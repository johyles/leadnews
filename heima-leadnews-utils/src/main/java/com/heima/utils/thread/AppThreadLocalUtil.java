package com.heima.utils.thread;

import com.heima.model.user.pojos.ApUser;
import com.heima.model.wemedia.pojos.WmUser;

public class AppThreadLocalUtil {

    private final static ThreadLocal<ApUser> App_USER_THREAD_LOCAL = new ThreadLocal<>();

    // 存入线程
    public static void setUser(ApUser apUser){
        App_USER_THREAD_LOCAL.set(apUser);
    }
    //从线程中获取
    public static ApUser getUser(){
        return App_USER_THREAD_LOCAL.get();
    }

    //清理
    public static void clear(){
        App_USER_THREAD_LOCAL.remove();
    }
}
