package com.xxl.job.admin.platform.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ice2Faith
 * @date 2024/7/31 17:06
 * @desc
 */
public class JvmUtil {
    private static final AtomicReference<String> PID = new AtomicReference<>();
    private static final AtomicReference<String> START_USER = new AtomicReference<>();
    private static final AtomicReference<Boolean> IS_DEBUG = new AtomicReference<>();
    private static final AtomicReference<Boolean> IS_AGENT = new AtomicReference<>();
    private static final AtomicReference<Boolean> IS_NO_VERIFY = new AtomicReference<>();

    public static RuntimeMXBean getRuntimeMXBean() {
        return ManagementFactory.getRuntimeMXBean();
    }

    public static String getPid() {
        return PID.updateAndGet(v -> {
            if (v != null) {
                return v;
            }
            String name = getRuntimeMXBean().getName();
            String[] arr = name.split("@", 2);
            if (arr.length == 2) {
                return arr[0];
            }
            return "-1";
        });
    }

    public static String getStartUser() {
        return START_USER.updateAndGet(v -> {
            if (v != null) {
                return v;
            }
            String name = getRuntimeMXBean().getName();
            String[] arr = name.split("@", 2);
            if (arr.length == 2) {
                return arr[1];
            }
            return "";
        });
    }

    public static List<String> getInputArguments() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments();
    }

    public static boolean isDebug() {
        return IS_DEBUG.updateAndGet(v -> {
            if (v != null) {
                return v;
            }
            List<String> args = getInputArguments();
            for (String arg : args) {
                if (arg.startsWith("-Xrunjdwp") || arg.startsWith("-agentlib:jdwp")) {
                    return true;
                }
            }
            return false;
        });
    }

    public static boolean isAgent() {
        return IS_AGENT.updateAndGet(v -> {
            if (v != null) {
                return v;
            }
            List<String> args = getInputArguments();
            for (String arg : args) {
                if (arg.startsWith("-javaagent:")) {
                    return true;
                }
            }
            return false;
        });
    }

    public static boolean isNoVerify() {
        return IS_NO_VERIFY.updateAndGet(v -> {
            if (v != null) {
                return v;
            }
            List<String> args = getInputArguments();
            for (String arg : args) {
                if ("-noverify".equals(arg) || "-Xverify:none".equals(arg)) {
                    return true;
                }
            }
            return false;
        });
    }
}
