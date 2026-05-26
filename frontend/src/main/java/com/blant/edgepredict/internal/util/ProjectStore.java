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
    private static final File PROJECTS_FILE    = new File(CacheUtil.CONFIG_DIR, "projects.properties");
    private static final File INPUT_FILES_FILE = new File(CacheUtil.CONFIG_DIR, "project_files.properties");

    // ── generic helpers ────────────────────────────────────────────────────────

    private static Properties loadFile(File f) {
        Properties props = new Properties();
        if (f.exists()) {
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException ignored) {}
        }
        return props;
    }

    private static void persistFile(Properties props, File f) throws IOException {
        CacheUtil.CONFIG_DIR.mkdirs();
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            props.store(w, null);
        }
    }

    // ── project name ↔ job ID ─────────────────────────────────────────────────

    public static void saveProject(String name, String jobId) {
        if (name == null || name.isBlank() || jobId == null) return;
        Properties props = loadFile(PROJECTS_FILE);
        props.setProperty(name, jobId);
        try { persistFile(props, PROJECTS_FILE); } catch (IOException ignored) {}
    }

    public static Map<String, String> getProjects() {
        Properties props = loadFile(PROJECTS_FILE);
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return result;
    }

    /** Returns the local project name that already owns this jobId, or null. */
    public static String getNameByJobId(String jobId) {
        if (jobId == null) return null;
        for (Map.Entry<String, String> entry : getProjects().entrySet()) {
            if (jobId.equals(entry.getValue())) return entry.getKey();
        }
        return null;
    }

    public static void deleteProject(String name) {
        Properties props = loadFile(PROJECTS_FILE);
        String jobId = props.getProperty(name);
        props.remove(name);
        try { persistFile(props, PROJECTS_FILE); } catch (IOException ignored) {}
        if (jobId != null) {
            new File(CacheUtil.CONFIG_DIR, "output_log_" + jobId + ".txt").delete();
            new File(CacheUtil.CONFIG_DIR, "input_log_"  + jobId + ".txt").delete();
            // remove stored input-file path too
            Properties fileProps = loadFile(INPUT_FILES_FILE);
            fileProps.remove(jobId);
            try { persistFile(fileProps, INPUT_FILES_FILE); } catch (IOException ignored) {}
        }
    }

    public static void renameProject(String oldName, String newName) {
        if (newName == null || newName.isBlank()) return;
        Properties props = loadFile(PROJECTS_FILE);
        String jobId = props.getProperty(oldName);
        if (jobId == null) return;
        props.remove(oldName);
        props.setProperty(newName, jobId);
        try { persistFile(props, PROJECTS_FILE); } catch (IOException ignored) {}
    }

    // ── job ID ↔ input file path ───────────────────────────────────────────────

    public static void saveInputFile(String jobId, File file) {
        if (jobId == null || file == null) return;
        Properties props = loadFile(INPUT_FILES_FILE);
        props.setProperty(jobId, file.getAbsolutePath());
        try { persistFile(props, INPUT_FILES_FILE); } catch (IOException ignored) {}
    }

    /**
     * Returns the stored input File for a jobId, or null if never saved or the
     * file no longer exists on disk.
     */
    public static File getInputFile(String jobId) {
        if (jobId == null) return null;
        Properties props = loadFile(INPUT_FILES_FILE);
        String path = props.getProperty(jobId);
        if (path == null) return null;
        File f = new File(path);
        return f.exists() ? f : null;
    }
}
