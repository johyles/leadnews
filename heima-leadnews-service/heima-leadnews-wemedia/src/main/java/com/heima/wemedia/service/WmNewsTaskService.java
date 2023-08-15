package com.heima.wemedia.service;

import java.util.Date;

/**
 * @Description: 发布文章集成添加延迟队列接口
 */
public interface WmNewsTaskService {

    /**
     * 添加任务到延迟队列中
     * @param id  文章的id
     * @param publishTime  发布的时间  可以做为任务的执行时间
     */
    public void addNewsToTask(Integer id, Date publishTime);

    /**
     * 消费任务审核文章
     */
    public void scanNewsByTask();
}
