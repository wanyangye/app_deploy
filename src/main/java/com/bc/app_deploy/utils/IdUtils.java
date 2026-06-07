package com.bc.app_deploy.utils;

import cn.hutool.core.util.IdUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class IdUtils extends IdUtil {
    /**
     * 生成唯一ID
     *
     * @return
     */
    public static Long genId() {
        return IdUtil.getSnowflakeNextId();
    }


    public static String generateGUID(String ip, int port) {
        String source = ip + ":" + port;
        // 基于输入的字符串生成确定的 UUID
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * 基于 IP 和 Port 生成唯一的 BigInt ID
     */
    public static Long generateLongId(String ip, int port) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            byte[] bytes = inetAddress.getAddress();

            // 1. 将 IPv4 转为 Long (占用 32 位)
            long ipLong = 0;
            for (byte b : bytes) {
                ipLong = (ipLong << 8) | (b & 0xFF);
            }

            // 2. 将 IP 左移 16 位，腾出空间给端口 (端口最大 65535，即 16 位)
            // 结果共占用 48 位，完全符合 BigInt (64位) 范围
            return (ipLong << 16) | (port & 0xFFFF);
        } catch (UnknownHostException e) {
            // 兜底方案：如果解析失败，使用哈希
            return (long) (ip + ":" + port).hashCode();
        }
    }
}