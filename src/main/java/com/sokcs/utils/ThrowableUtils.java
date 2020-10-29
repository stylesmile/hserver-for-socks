package com.sokcs.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ThrowableUtils {
    public static void message(Class<?> aClass, Throwable cause) {
        log.error("发生异常的类：{}, 异常信息：{}", aClass.getSimpleName(), cause.getMessage());
    }

    public static void message(String desc, Class<?> aClass, Throwable cause) {
        log.error("说明：{}，发生异常的类：{}, 异常信息：{}", desc, aClass.getSimpleName(), cause.getMessage());
    }
}
