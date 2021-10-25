package com.gel.ricedecetion.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file-service")
public class WebConfig {

	/**
     * 上传路径
     */
    private static String profile = "";

    public static String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        WebConfig.profile = profile;
    }
	
    // 获取下载路径
    public static String getDownloadPath() {
        return profile + "download/";
    }
	
    // 获取上传路径
    public static String getBaseDir() {
        return profile + "decetion/";
    }
}
