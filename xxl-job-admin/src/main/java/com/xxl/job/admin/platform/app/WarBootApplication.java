package com.xxl.job.admin.platform.app;

import com.xxl.job.admin.platform.slf4j.Slf4jPrintStream;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @author Ice2Faith
 * @date 2024/7/12 21:57
 * @desc 继承 SpringBootServletInitializer 并重写configure方法，使得指向类指向启动类，则可以再war包中启动
 * * 在war包中启动，pom.xml需要starter-web排除tomcat
 * * 另外打包方式改为war
 */
public class WarBootApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        Slf4jPrintStream.redirectSysoutSyserr();
        return builder.sources(this.getClass())
                .listeners(BaseBootApplication.getStartedListener(null, this.getClass()));
    }


    public static void startup(Class<?> mainClass, String[] args) {
        startup(null, mainClass, args);
    }

    public static void startup(WebApplicationType webType, Class<?> mainClass, String[] args) {
        BaseBootApplication.startup(webType, mainClass, args);
    }
}
