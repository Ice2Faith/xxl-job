package com.xxl.job.admin.platform.launcher;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Manifest;

/**
 * @author Ice2Faith
 * @date 2025/7/24 8:35
 * @desc 自定义类加载器
 * 优先使用本加载器的classpath资源
 * 本加载器没有的情况再使用默认加载器
 * 始终保证本加载器classpath的优先级更高
 */
public class ExtClasspathClassLoader extends URLClassLoader {
    public ExtClasspathClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public URL findResource(String name) {
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }

    @Override
    public URL getResource(String name) {
        // 根据源码，findResource 默认就是只加载本加载器的内容
        try {
            URL url = findResource(name);
            if (url != null) {
                return url;
            }
        } catch (Throwable e) {

        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        try {
            // 根据源码，findResources 默认就是只加载本加载器的内容
            Enumeration<URL> current = findResources(name);
            // 合并默认加载器内容，并保证本加载器的优先级
            Enumeration<URL> resources = super.getResources(name);
            Set<String> unqSet = new HashSet<>();
            Set<URL> set = new LinkedHashSet<>();
            while (current.hasMoreElements()) {
                URL item = current.nextElement();
                String str = item.toString();
                if (!unqSet.contains(str)) {
                    set.add(item);
                }
                unqSet.add(str);
            }
            while (resources.hasMoreElements()) {
                URL item = resources.nextElement();
                String str = item.toString();
                if (!unqSet.contains(str)) {
                    set.add(item);
                }
                unqSet.add(str);
            }
            return Collections.enumeration(set);
        } catch (Throwable e) {

        }
        return super.getResources(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // jdk 部分就直接使用默认逻辑即可
            if (name.startsWith("java.")
                    || name.startsWith("jdk.")
                    || name.startsWith("sun.")
                    || name.startsWith("com.sun.")
                    || name.startsWith("javax.")
                    || name.startsWith("jakarta.")) {
                return super.loadClass(name, resolve);
            }
            try {
                // 根据源码，findClass 默认就是只加载本加载器的内容
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            } catch (Throwable e) {
                // 发生任何异常，都交给默认加载器
                return super.loadClass(name, resolve);
            }
        }
    }

    @Override
    protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        return super.definePackage(name, man, url);
    }

    @Override
    protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }
}
