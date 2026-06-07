package com.bc.app_deploy.utils;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class BuildUtils {
    
    /**
     * 执行构建命令
     */
    public static boolean executeCommand(String command, File workDir, Consumer<String> logConsumer) {
        try {
            logConsumer.accept("[Build] 执行构建命令: " + command);
            logConsumer.accept("[Build] 工作目录: " + workDir.getAbsolutePath());
            
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // Windows系统使用cmd，Linux/Mac使用sh
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }
            
            processBuilder.directory(workDir);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logConsumer.accept(line);
                }
            }
            
            int exitCode = process.waitFor();
            logConsumer.accept("[Build] 构建完成，退出码: " + exitCode);
            
            return exitCode == 0;
        } catch (Exception e) {
            logConsumer.accept("[Build] 构建异常: " + e.getMessage());
            log.error("构建异常", e);
            return false;
        }
    }
    
    /**
     * 查找构建产物
     */
    public static File findBuildArtifact(File baseDir, String buildDir) throws Exception {
        // 检查是否包含通配符
        if (buildDir.contains("*")) {
            return findArtifactWithPattern(baseDir, buildDir);
        }
        
        File artifactPath = new File(baseDir, buildDir);
        
        if (!artifactPath.exists()) {
            throw new Exception("构建产物路径不存在: " + artifactPath.getAbsolutePath());
        }
        
        // 如果是文件，直接返回
        if (artifactPath.isFile()) {
            return artifactPath;
        }
        
        // 如果是目录，查找jar或zip文件
        File[] files = artifactPath.listFiles((dir, name) -> 
            name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".zip"));
        
        if (files != null && files.length > 0) {
            return files[0];
        }
        
        // 对于Vue项目，可能是dist目录，需要打包
        if (artifactPath.getName().equals("dist") && artifactPath.isDirectory()) {
            return packDistDirectory(artifactPath);
        }
        
        throw new Exception("未找到构建产物");
    }
    
    /**
     * 将 dist 目录打包为 zip 文件（使用 Java 原生方法）
     */
    private static File packDistDirectory(File distDir) throws Exception {
        try {
            File parentDir = distDir.getParentFile();
            File zipFile = new File(parentDir, "dist.zip");
            
            // 删除旧的压缩包
            if (zipFile.exists()) {
                zipFile.delete();
            }
            
            log.info("开始打包 dist 目录: {} -> {}", distDir.getAbsolutePath(), zipFile.getAbsolutePath());
            
            // 使用 Java 原生 ZIP 压缩
            try (ZipOutputStream zos = new ZipOutputStream(
                    new FileOutputStream(zipFile))) {
                zipDirectory(distDir, distDir.getName(), zos);
            }
            
            if (zipFile.exists() && zipFile.length() > 0) {
                log.info("dist 目录打包成功，文件大小: {} bytes", zipFile.length());
                return zipFile;
            } else {
                throw new Exception("打包 dist 目录失败：生成的 zip 文件无效");
            }
        } catch (Exception e) {
            log.error("打包 dist 目录失败", e);
            throw new Exception("打包 dist 目录失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 递归压缩目录
     */
    private static void zipDirectory(File sourceDir, String parentPath, ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            String zipEntryName = parentPath + "/" + file.getName();
            
            if (file.isDirectory()) {
                // 目录：递归压缩
                zipDirectory(file, zipEntryName, zos);
            } else {
                // 文件：添加到 ZIP
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    
                    zos.closeEntry();
                }
            }
        }
    }
    
    /**
     * 使用通配符查找构建产物
     */
    private static File findArtifactWithPattern(File baseDir, String pattern) throws Exception {
        // 分离路径和文件名模式
        // 例如: target/*.jar -> 路径:target, 模式:*.jar
        int lastSeparator = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
        
        String dirPath;
        String filePattern;
        
        if (lastSeparator > 0) {
            dirPath = pattern.substring(0, lastSeparator);
            filePattern = pattern.substring(lastSeparator + 1);
        } else {
            dirPath = ".";
            filePattern = pattern;
        }
        
        File searchDir = new File(baseDir, dirPath);
        
        if (!searchDir.exists() || !searchDir.isDirectory()) {
            throw new Exception("构建产物目录不存在: " + searchDir.getAbsolutePath());
        }
        
        // 将通配符模式转换为正则表达式
        String regex = filePattern.replace(".", "\\.")
                                   .replace("*", ".*")
                                   .replace("?", ".");
        
        // 查找匹配的文件
        File[] matchedFiles = searchDir.listFiles((dir, name) -> name.matches(regex));
        
        if (matchedFiles == null || matchedFiles.length == 0) {
            throw new Exception("未找到匹配的构建产物: " + pattern + " (搜索目录: " + searchDir.getAbsolutePath() + ")");
        }
        
        // 返回最新的文件（按修改时间排序）
        File latestFile = matchedFiles[0];
        for (File file : matchedFiles) {
            if (file.lastModified() > latestFile.lastModified()) {
                latestFile = file;
            }
        }
        
        return latestFile;
    }
    
    /**
     * 清理构建目录，解决文件被占用的问题
     * 支持 Java 项目（jar/war）和 Vue 项目（dist 目录）
     */
    public static void cleanBuildDirectory(File workDir, String buildDir, Consumer<String> logConsumer) {
        try {
            // 处理构建目录路径
            String dirPath = buildDir;
            if (buildDir.contains("/") || buildDir.contains("\\")) {
                int lastSeparator = Math.max(buildDir.lastIndexOf('/'), buildDir.lastIndexOf('\\'));
                dirPath = buildDir.substring(0, lastSeparator);
            }
            
            File targetDir = new File(workDir, dirPath);
            
            if (!targetDir.exists()) {
                logConsumer.accept("[Clean] 构建目录不存在，跳过清理: " + targetDir.getAbsolutePath());
                return;
            }
            
            logConsumer.accept("[Clean] 正在检查构建目录: " + targetDir.getAbsolutePath());
            
            // 判断是 Vue 项目还是 Java 项目
            if (buildDir.equals("dist") || buildDir.endsWith("/dist") || buildDir.endsWith("\\dist")) {
                // Vue 项目：删除 dist 目录
                cleanDistDirectory(targetDir, logConsumer);
            } else {
                // Java 项目：清理 jar/war 文件
                cleanJavaArtifacts(targetDir, logConsumer);
            }
            
        } catch (Exception e) {
            logConsumer.accept("[Clean] 清理失败: " + e.getMessage());
            log.warn("清理构建目录失败", e);
        }
    }
    
    /**
     * 清理 Vue 项目的 dist 目录
     */
    private static void cleanDistDirectory(File distDir, Consumer<String> logConsumer) {
        try {
            if (!distDir.exists()) {
                logConsumer.accept("[Clean] dist 目录不存在，无需清理");
                return;
            }
            
            logConsumer.accept("[Clean] 正在删除 dist 目录: " + distDir.getAbsolutePath());
            
            // 尝试删除 dist 目录，最多重试 3 次
            int maxRetries = 3;
            boolean deleted = false;
            
            for (int i = 0; i < maxRetries && !deleted; i++) {
                try {
                    if (i > 0) {
                        logConsumer.accept("[Clean] 正在重试删除 (" + (i + 1) + "/" + maxRetries + ")...");
                        // 等待一段时间后重试
                        Thread.sleep(1000);
                    }
                    
                    // 递归删除 dist 目录
                    deleteDirectoryWithRetry(distDir, logConsumer);
                    deleted = true;
                    logConsumer.accept("[Clean] dist 目录清理完成");
                } catch (IOException e) {
                    if (i == maxRetries - 1) {
                        throw e; // 最后一次失败则抛出异常
                    }
                    logConsumer.accept("[Clean] 删除失败: " + e.getMessage() + "，准备重试...");
                    
                    // Windows 下尝试强制释放文件句柄
                    System.gc();
                    System.runFinalization();
                }
            }
            
        } catch (Exception e) {
            logConsumer.accept("[Clean] 清理 dist 目录失败: " + e.getMessage());
            logConsumer.accept("[Clean] 将继续执行构建，npm 可能会清理旧的 dist 目录");
            log.warn("清理 dist 目录失败", e);
        }
    }
    
    /**
     * 递归删除目录（带重试机制）
     */
    private static void deleteDirectoryWithRetry(File directory, Consumer<String> logConsumer) throws IOException {
        if (!directory.exists()) {
            return;
        }
        
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryWithRetry(file, logConsumer);
                }
            }
        }
        
        // 尝试删除文件或空目录，带重试
        int retryCount = 5;
        boolean deleted = false;
        
        for (int i = 0; i < retryCount && !deleted; i++) {
            if (i > 0) {
                // 等待后重试
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 强制 GC 释放文件句柄
                System.gc();
                System.runFinalization();
            }
            
            deleted = directory.delete();
            
            if (!deleted && i < retryCount - 1) {
                // 如果是 Windows，尝试使用 attrib 命令移除只读属性
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    try {
                        ProcessBuilder pb = new ProcessBuilder("attrib", "-R", "-S", "-H", directory.getAbsolutePath());
                        pb.start().waitFor();
                    } catch (Exception e) {
                        // 忽略 attrib 命令失败
                    }
                }
            }
        }
        
        if (!deleted) {
            throw new IOException("无法删除文件或目录: " + directory.getAbsolutePath() + " (拒绝访问)");
        }
    }
    
    /**
     * 清理 Java 项目的 jar/war 文件
     */
    private static void cleanJavaArtifacts(File targetDir, Consumer<String> logConsumer) {
        if (targetDir.isDirectory()) {
            // 查找jar/war文件
            File[] files = targetDir.listFiles((dir, name) -> 
                name.endsWith(".jar") || name.endsWith(".war"));
            
            if (files != null && files.length > 0) {
                // Windows系统下，终止占用文件的进程
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    for (File file : files) {
                        killProcessUsingFile(file, logConsumer);
                    }
                    // 等待进程完全退出
                    logConsumer.accept("[Clean] 等待进程释放文件...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                logConsumer.accept("[Clean] 进程清理完成，Maven 将自动清理构建文件");
            } else {
                logConsumer.accept("[Clean] 未找到需要清理的构建产物");
            }
        }
    }
    
    /**
     * 在Windows系统下终止占用文件的进程
     */
    private static void killProcessUsingFile(File file, Consumer<String> logConsumer) {
        try {
            logConsumer.accept("[Clean] 检查文件占用: " + file.getName());
            
            // 获取当前进程 PID，避免自杀
            long currentPid = ProcessHandle.current().pid();
            logConsumer.accept("[Clean] 当前进程 PID: " + currentPid + " (将被排除)");
            
            // 使用 wmic 直接查找包含jar文件名的java进程
            String fileName = file.getName();
            ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c",
                "wmic process where \"name='java.exe' and CommandLine like '%" + fileName + "%'\" get ProcessId"
            );
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
            
            String line;
            boolean foundProcess = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过标题行和空行
                if (line.isEmpty() || line.equalsIgnoreCase("ProcessId")) {
                    continue;
                }
                
                // 尝试解析为PID
                try {
                    int pid = Integer.parseInt(line);
                    
                    // 跳过当前进程（避免自杀）
                    if (pid == currentPid) {
                        logConsumer.accept("[Clean] 跳过当前 MarsDeploy 进程: " + pid);
                        continue;
                    }
                    
                    foundProcess = true;
                    logConsumer.accept("[Clean] 发现占用进程 PID: " + pid + "，正在终止...");
                    
                    // 终止进程
                    ProcessBuilder killPb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
                    Process killProcess = killPb.start();
                    
                    // 读取 taskkill 的输出
                    BufferedReader killReader = new BufferedReader(
                        new InputStreamReader(killProcess.getInputStream(), "GBK"));
                    String killOutput;
                    while ((killOutput = killReader.readLine()) != null) {
                        logConsumer.accept("[Clean] " + killOutput);
                    }
                    killReader.close();
                    
                    int exitCode = killProcess.waitFor();
                    if (exitCode == 0) {
                        logConsumer.accept("[Clean] 已成功终止进程: " + pid);
                    } else {
                        logConsumer.accept("[Clean] 终止进程失败 (退出码: " + exitCode + ")");
                    }
                } catch (NumberFormatException e) {
                    // 不是数字，跳过
                }
            }
            
            if (!foundProcess) {
                logConsumer.accept("[Clean] 未找到占用该文件的其他进程");
            }
            
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            logConsumer.accept("[Clean] 查找占用进程失败: " + e.getMessage());
            log.debug("查找占用进程失败", e);
        }
    }
}
