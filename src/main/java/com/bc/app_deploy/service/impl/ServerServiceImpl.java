package com.bc.app_deploy.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bc.app_deploy.mapper.ProjectServerMapper;
import com.bc.app_deploy.mapper.ServerMapper;
import com.bc.app_deploy.model.entity.ProjectServerDO;
import com.bc.app_deploy.model.entity.ServerDO;
import com.bc.app_deploy.service.IServerService;
import com.bc.app_deploy.utils.SshUtils;
import com.jcraft.jsch.Session;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ServerServiceImpl extends ServiceImpl<ServerMapper, ServerDO> implements IServerService {

    @Resource
    private ProjectServerMapper projectServerMapper;

    /**
     * 保存或更新服务器信息，同时测试连接
     */
    @Override
    public boolean saveOrUpdateServer(ServerDO server) {
        boolean connected = testConnection(server);
        server.setStatus(connected ? 1 : 0);
        return this.saveOrUpdate(server);
    }

    /**
     * 检查服务器是否关联了项目
     */
    @Override
    public boolean hasRelatedProjects(Long serverId) {
        LambdaQueryWrapper<ProjectServerDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectServerDO::getServerId, serverId);
        return projectServerMapper.selectCount(wrapper) > 0;
    }


    /**
     * 测试服务器连接
     */
    @Override
    public boolean testConnection(ServerDO server) {
        return SshUtils.testConnection(
                server.getHost(),
                server.getPort(),
                server.getAccount(),
                server.getPassword(),
                server.getPrivateKey()
        );
    }


    /**
     * 获取服务器监控信息
     */
    @Override
    public Map<String, Object> getMonitorInfo(ServerDO server) throws Exception {
        Map<String, Object> monitorData = new HashMap<>();
        Session session = null;

        try {
            session = SshUtils.createSession(
                    server.getHost(),
                    server.getPort(),
                    server.getAccount(),
                    server.getPassword(),
                    server.getPrivateKey()
            );
            session.connect();

            // 获取系统信息
            monitorData.put("hostname", executeCommand(session, "hostname"));
            monitorData.put("uptime", executeCommand(session, "uptime -p"));
            monitorData.put("os", executeCommand(session, "cat /etc/os-release | grep PRETTY_NAME | cut -d '\"' -f 2"));

            // 获取CPU信息
            String cpuUsage = executeCommand(session, "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - $1}'");
            monitorData.put("cpuUsage", parseDouble(cpuUsage));

            String cpuCores = executeCommand(session, "nproc");
            monitorData.put("cpuCores", parseInteger(cpuCores));

            // 获取内存信息
            String memInfo = executeCommand(session, "free -m | grep Mem");
            String[] memParts = memInfo.trim().split("\\s+");
            if (memParts.length >= 3) {
                long totalMem = Long.parseLong(memParts[1]);
                long usedMem = Long.parseLong(memParts[2]);
                monitorData.put("memTotal", totalMem);
                monitorData.put("memUsed", usedMem);
                monitorData.put("memUsage", totalMem > 0 ? (double) usedMem / totalMem * 100 : 0);
            }

            // 获取磁盘信息
            String diskInfo = executeCommand(session, "df -h / | tail -1");
            String[] diskParts = diskInfo.trim().split("\\s+");
            if (diskParts.length >= 5) {
                monitorData.put("diskTotal", diskParts[1]);
                monitorData.put("diskUsed", diskParts[2]);
                monitorData.put("diskAvail", diskParts[3]);
                String usagePercent = diskParts[4].replace("%", "");
                monitorData.put("diskUsage", parseDouble(usagePercent));
            }


            // 获取进程数
            String processCount = executeCommand(session, "ps aux | wc -l");
            monitorData.put("processCount", parseInteger(processCount));

            // 获取负载信息
            String loadAvg = executeCommand(session, "uptime | awk -F'load average:' '{print $2}'");
            monitorData.put("loadAvg", loadAvg.trim());

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return monitorData;
    }


    /**
     * 执行SSH命令并返回结果
     */
    private String executeCommand(Session session, String command) throws Exception {
        com.jcraft.jsch.ChannelExec channelExec = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);

        InputStream in = channelExec.getInputStream();
        channelExec.connect();

        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }

        channelExec.disconnect();
        return result.toString().trim();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
