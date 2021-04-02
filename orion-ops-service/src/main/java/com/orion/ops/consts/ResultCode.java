package com.orion.ops.consts;

import com.orion.lang.wrapper.CodeInfo;

/**
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2021/4/2 9:48
 */
public enum ResultCode implements CodeInfo {

    /**
     * 未认证
     */
    UNAUTHORIZED() {
        @Override
        public int code() {
            return 700;
        }

        @Override
        public String message() {
            return "未认证";
        }
    },

    /**
     * 不能访问终端
     */
    TERMINAL_UN_ACCESS() {
        @Override
        public int code() {
            return 710;
        }

        @Override
        public String message() {
            return "无权连接终端";
        }
    },

    ;

}
