package com.orion.ops.handler.scheduler;

import com.orion.ops.consts.Const;
import com.orion.ops.handler.scheduler.task.ReleaseTask;
import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 * 任务类型
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2022/2/14 10:16
 */
@AllArgsConstructor
public enum TaskType {

    /**
     * 发布任务
     */
    RELEASE(id -> new ReleaseTask((Long) id)) {
        @Override
        public String getKey(Object params) {
            return Const.RELEASE + "-" + params;
        }
    },

    ;

    private final Function<Object, Runnable> factory;

    /**
     * 创建任务
     *
     * @param params params
     * @return task
     */
    public Runnable create(Object params) {
        return factory.apply(params);
    }

    /**
     * 获取 key
     *
     * @param params params
     * @return key
     */
    public abstract String getKey(Object params);

}