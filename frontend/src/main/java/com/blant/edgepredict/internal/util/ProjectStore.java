package com.blant.edgepredict.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ProjectStore {
    private static final File PROJECTS_FILE = new File(CacheUtil.CONFIG_DIR, "projects.properties");

    private static Properties load() {
        Properties props = new Properties();
        if (PROJECTS_FILE.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(PROJECTS_FILE), StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException ignored) {}
        }
        return props;
    }

    private static void persist(Properties props) throws IOException {
        CacheUtil.CONFIG_DIR.mkdirs();
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(PROJECTS_FILE), StandardCharsets.UTF_8)) {
            props.store(writer, null);
        }
    }

    public static void saveProject(String name, String jobId) {
        if (name == null || name.isBlank() || jobId == null) return;
        Properties props = load();
        props.setProperty(name, jobId);
        try {
            persist(props);
        } catch (IOException ignored) {}
    }

    public static Map<String, String> getProjects() {
        Properties props = load();
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return result;
    }

    public static void deleteProject(String name) {
        Properties props = load();
        String jobId = props.getProperty(name);
        props.remove(name);
        try {
            persist(props);
        } catch (IOException ignored) {}
        if (jobId != null) {
            new File(CacheUtil.CONFIG_DIR, "output_log_" + jobId + ".txt").delete();
            new File(CacheUtil.CONFIG_DIR, "input_log_" + jobId + ".txt").delete();
        }
    }

    public static void renameProject(String oldName, String newName) {
        if (newName == null || newName.isBlank()) return;
        Properties props = load();
        String jobId = props.getProperty(oldName);
        if (jobId == null) return;
        props.remove(oldName);
        props.setProperty(newName, jobId);
        try {
            persist(props);
        } catch (IOException ignored) {}
    }
}
