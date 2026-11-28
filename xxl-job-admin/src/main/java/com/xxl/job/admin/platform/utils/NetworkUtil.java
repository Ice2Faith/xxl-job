package com.xxl.job.admin.platform.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author Ice2Faith
 * @date 2025/11/13 19:30
 * @desc
 */
public class NetworkUtil {
    // 常见虚拟网卡关键字（强虚拟）
    private static final String[] VIRTUAL_NAMES = {
            "docker", "cni", "cali", "flannel", "weave", "podman", "kube-ipvs", "cilium",
            "virbr", "vnet", "vboxnet", "vmnet", "vmx", "vmware", "vethernet", "xenbr", "vif",
            "dummy",
            "macvlan", "ipvlan"
    };

    // 弱虚拟网卡（可能误报或 VPN 类）
    private static final String[] VIRTUAL_WEAK_NAMES = {
            "br-", "vwnet",
            "tun", "tap", "utun", "wg",
            "zt", "tailscale", "ts", "openvpn", "ovpn", "ipsec",
            "bond", "team",
            "vxlan", "geneve", "gre", "gretap", "ipip", "sit", "erspan"
    };

    /**
     * 判断是否为强虚拟网卡
     */
    public static boolean isVirtualInterface(String name) {
        String lower = name.toLowerCase();
        for (String kw : VIRTUAL_NAMES) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为弱虚拟网卡
     */
    public static boolean isVirtualWeakInterface(String name) {
        String lower = name.toLowerCase();
        for (String kw : VIRTUAL_WEAK_NAMES) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * IP 条目，包含网卡名、IP 地址及虚拟标识
     */
    @Data
    @NoArgsConstructor
    public static class IpEntry {
        protected NetworkInterface networkInterface;
        protected InetAddress inetAddress;

        protected String interfaceName;
        protected String ip;
        protected boolean ipv6;
        protected boolean virtual;
        protected boolean weakVirtual;

        public IpEntry(NetworkInterface networkInterface, InetAddress inetAddress) {
            this.networkInterface = networkInterface;
            this.inetAddress = inetAddress;
            this.resolveProperties();
        }

        public void resolveProperties() {
            if (networkInterface != null) {
                interfaceName = networkInterface.getDisplayName();
            }
            if (inetAddress != null) {
                ip = inetAddress.getHostAddress();
                ipv6 = inetAddress instanceof Inet6Address;
            }
            if (interfaceName != null) {
                virtual = isVirtualInterface(interfaceName);
                weakVirtual = isVirtualWeakInterface(interfaceName);
            }
        }
    }

    /**
     * 获取系统当前正在使用的出口 IP（最可靠）
     */
    public static String getPreferredIp() {
        // 尝试通过 UDP 连接 8.8.8.8:80 获取本地地址
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 80);
            socket.setSoTimeout(2000);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address) {
                return local.getHostAddress();
            }
        } catch (Exception e) {
            // 忽略异常，继续降级方案
        }

        // 降级方案：取 getAllIpList 排序后的第一个 IP
        List<IpEntry> entries = getUsefulAddresses();
        if (!entries.isEmpty()) {
            return entries.get(0).getIp();
        }
        return null;
    }

    /**
     * 获取第一个有用的网卡地址
     */
    public static IpEntry getFirstUsefulAddress() {
        List<IpEntry> list = getUsefulAddresses();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 获取所有 up 状态、非 loopback 的 IPv4 地址（包含虚拟网卡），
     * 返回按 [非虚拟 → 弱虚拟 → 强虚拟] 排序的列表，同组内按网卡名排序。
     */
    public static List<IpEntry> getUsefulAddresses() {
        List<IpEntry> ret = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                try {
                    if (!networkInterface.isUp()) {
                        continue;
                    }
                    if (networkInterface.isLoopback()) {
                        continue;
                    }
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        try {
                            if (inetAddress.isMulticastAddress()) {
                                continue;
                            }
                            if (inetAddress.isLoopbackAddress()) {
                                continue;
                            }
                            ret.add(new IpEntry(networkInterface, inetAddress));
                        } catch (Exception e) {

                        }
                    }
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {

        }

        // 排序规则：非虚拟网卡在前，非弱虚拟网卡在前，网卡名称正序，ipv4在前
        ret.sort(Comparator
                .comparing(IpEntry::isVirtual)
                .thenComparing(IpEntry::isWeakVirtual)
                .thenComparing(IpEntry::getInterfaceName)
                .thenComparing(IpEntry::isIpv6)
        );
        return ret;
    }

    /**
     * 判断是否是内网网段的IPV4地址
     *
     * @param ip 地址
     * @return
     */
    public static boolean isPrivateIpv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] arr = ip.split("\\.");
        List<Integer> parts = Arrays.stream(arr)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (Exception e) {
                    }
                    return null;
                }).collect(Collectors.toList());
        if (parts.size() != 4 && parts.stream().anyMatch(v -> v == null || v < 0 || v > 255)) {
            return false;
        }
        int a = parts.get(0);
        int b = parts.get(1);


        // 10.0.0.0/8
        if (a == 10) {
            return true;
        }

        // 172.16.0.0/12
        if (a == 172 && b >= 16 && b <= 31) {
            return true;
        }

        // 192.168.0.0/16
        if (a == 192 && b == 168) {
            return true;
        }

        // 127.0.0.0/8
        if (a == 127) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否内内网地址
     *
     * @param hostname
     * @return
     */
    public static boolean isIntranetHost(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return false;
        }

        // IPv6 形式
        if (hostname.startsWith("[")
                && hostname.endsWith("]")) {
            String ip = hostname.substring(1, hostname.length() - 1);
            if (ip.equals("::1")) {
                return true;
            }
            if (ip.startsWith("fe80:")) {
                return true;
            } // 链路本地
            if (ip.startsWith("fc") || ip.startsWith("fd")) {
                return true;
            } // ULA
            return false;
        }

        // 普通 IPv4
        if (hostname.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return isPrivateIpv4(hostname);
        }

        // 本机地址
        if (isLocalHost(hostname)) {
            return true;
        }

        return false;
    }

    protected static CopyOnWriteArrayList<String> localHostNameList = new CopyOnWriteArrayList<>();
    protected static AtomicLong localHostNameExpireTs = new AtomicLong(0);
    protected static ReentrantLock lock = new ReentrantLock();

    /**
     * 获取本机的所有主机名或地址
     *
     * @return
     */
    public static List<String> getLocalHostNames() {
        if (System.currentTimeMillis() < localHostNameExpireTs.get()) {
            return new ArrayList<>(localHostNameList);
        }
        lock.lock();
        try {
            List<String> list = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    try {
                        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                        while (inetAddresses.hasMoreElements()) {
                            InetAddress inetAddress = inetAddresses.nextElement();
                            list.add(inetAddress.getCanonicalHostName());
                            list.add(inetAddress.getHostName());
                            list.add(inetAddress.getHostAddress());
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            localHostNameList.clear();
            localHostNameList.addAll(list);
            localHostNameExpireTs.set(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));
        } finally {
            lock.unlock();
        }
        return new ArrayList<>(localHostNameList);
    }

    /**
     * 判断是否是本机
     *
     * @param hostname
     * @return
     */
    public static boolean isLocalHost(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return false;
        }

        // localhost / 127.0.0.1
        if (Objects.equals(hostname, "localhost")
                || Objects.equals(hostname, "127.0.0.1")
                || Objects.equals(hostname, "[::1]")) {
            return true;
        }

        List<String> localHostNames = getLocalHostNames();
        boolean ret = localHostNames.contains(hostname);
        if (ret) {
            return true;
        }
        try {
            // 通过主机名，获取 IP 判断是否是本机的 IP
            InetAddress address = InetAddress.getByName(hostname);

            // 检查是否是"任意本地地址" (如 0.0.0.0) 或 "回环地址" (如 127.0.0.1, ::1)
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()) {
                return true;
            }

            String ip = address.getHostAddress();
            if (localHostNames.contains(ip)) {
                return true;
            }

            // 检查该地址是否绑定在本机的任何一个网络接口上
            try {
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
                if (networkInterface != null) {
                    return true;
                }
            } catch (SocketException e) {
                // ignore
            }
        } catch (UnknownHostException e) {
            // ignore
        }
        return false;
    }
}
