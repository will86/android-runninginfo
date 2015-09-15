package net.jileniao.runninginfo.util;

import java.io.IOException;
import java.io.InputStream;

public class BatteryUtil {

    // 获取CPU最大频率（单位KHZ）
    // "/system/bin/cat" 命令行
    // "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" 存储最大频率的文件的路径
    public static String getCurrent() {
        String result = "";
        String path = "/sys/class/power_supply/battery/current_now";
        ProcessBuilder cmd;
        try {
            String[] args = { "/system/bin/cat", path };
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result += new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        }
        return result.trim();
    }
}