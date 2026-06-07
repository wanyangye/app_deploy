package com.bc.app_deploy.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import java.io.File;
import java.util.function.Consumer;

@Slf4j
public class GitUtils {
    
    /**
     * 克隆或拉取Git仓库
     */
    public static void cloneOrPull(String gitUrl, String branch, String username, String password, 
                                   File localPath, Consumer<String> logConsumer) throws Exception {
        logConsumer.accept("[Git] 开始克隆/拉取代码...");
        logConsumer.accept("[Git] 仓库地址: " + gitUrl);
        logConsumer.accept("[Git] 分支: " + branch);
        logConsumer.accept("[Git] 本地路径: " + localPath.getAbsolutePath());
        
        UsernamePasswordCredentialsProvider credentialsProvider = null;
        if (username != null && password != null) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        }
        
        if (localPath.exists() && new File(localPath, ".git").exists()) {
            // 已存在，执行pull
            logConsumer.accept("[Git] 检测到本地仓库，执行pull操作");
            try (Git git = Git.open(localPath)) {
                // 切换分支
                git.checkout()
                   .setName(branch)
                   .call();
                logConsumer.accept("[Git] 已切换到分支: " + branch);
                
                // 拉取代码
                PullResult pullResult;
                if (credentialsProvider != null) {
                    pullResult = git.pull()
                                   .setCredentialsProvider(credentialsProvider)
                                   .call();
                } else {
                    pullResult = git.pull().call();
                }
                
                logConsumer.accept("[Git] Pull完成: " + pullResult.isSuccessful());
            }
        } else {
            // 不存在，执行clone
            logConsumer.accept("[Git] 本地仓库不存在，执行clone操作");
            if (localPath.exists()) {
                deleteDirectory(localPath);
            }
            localPath.mkdirs();
            
            if (credentialsProvider != null) {
                Git.cloneRepository()
                   .setURI(gitUrl)
                   .setDirectory(localPath)
                   .setBranch(branch)
                   .setCredentialsProvider(credentialsProvider)
                   .call()
                   .close();
            } else {
                Git.cloneRepository()
                   .setURI(gitUrl)
                   .setDirectory(localPath)
                   .setBranch(branch)
                   .call()
                   .close();
            }
            
            logConsumer.accept("[Git] Clone完成");
        }
        
        logConsumer.accept("[Git] 代码拉取成功");
    }
    
    /**
     * 删除目录（兼容Windows文件锁定问题）
     */
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        // Windows上如果删除失败，等待一下再试
                        if (!file.delete()) {
                            try {
                                Thread.sleep(100);
                                file.delete();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            }
            directory.delete();
        }
    }
}
