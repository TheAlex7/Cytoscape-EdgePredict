package com.blant.edgepredict.internal.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CacheUtil {
    public static final String CONFIG_DIR_NAME = "CytoscapeConfiguration" + File.separator + "3" + File.separator + "apps" + File.separator + "BLANT";
    public static final String USER_HOME = System.getProperty("user.home");
    public static final File CONFIG_DIR = new File(USER_HOME, CONFIG_DIR_NAME);

    static {
        CONFIG_DIR.mkdirs();
    }

    private CacheUtil() {}

    public static String saveInput(String jobId, String data) {
        File inputLog = new File(CONFIG_DIR, "input_log_" + jobId + ".txt");
        try {
            Files.writeString(inputLog.toPath(), data, StandardCharsets.UTF_8);
            return "[INFO] Input log saved: " + inputLog.getAbsolutePath();
        } catch (IOException e) {
            return "[WARN] Input file could not be saved: " + e.getMessage();
        }
    }

    public static String saveOutput(String jobId, String data) {
        File outputLog = new File(CONFIG_DIR, "output_log_" + jobId + ".txt");
        try {
            Files.writeString(outputLog.toPath(), data, StandardCharsets.UTF_8);
            return "[INFO] Output log saved: " + outputLog.getAbsolutePath();
        } catch (IOException e) {
            return "[WARN] Output file could not be saved: " + e.getMessage();
        }
    }

    public static String getOutput(String jobId) {
        File outputLog = new File(CONFIG_DIR, "output_log_" + jobId + ".txt");
        try {
            return Files.readString(outputLog.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[ERROR] Output file cannot be loaded: " + e.getMessage();
        }
    }
}
