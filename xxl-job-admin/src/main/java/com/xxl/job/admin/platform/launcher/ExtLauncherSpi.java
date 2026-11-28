package com.xxl.job.admin.platform.launcher;

/**
 * @author Ice2Faith
 * @date 2026/8/24 10:28
 * @desc
 */
@FunctionalInterface
public interface ExtLauncherSpi {
    void premain(Class<?> mainClass, String[] args);
}
