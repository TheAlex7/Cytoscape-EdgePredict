package com.blant.edgepredict.internal.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.blant.edgepredict.internal.ui.BlantLogWindow;

public class BlantPoller {
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("\"progress\"\\s*:\\s*(\\d+)");
    private static final int MAX_RETRIES = 5;
    private static final long POLL_INTERVAL_MS = 5000L;

    private static BlantPoller instance;
    private SwingWorker<Void, String> poller;

    private BlantPoller() {}

    public static BlantPoller getInstance() {
        if (instance == null) {
            instance = new BlantPoller();
        }
        return instance;
    }

    public void startPolling(final String jobId, final PollingCallback callback) {
        BlantLogWindow.getInstance().setAbortBtnEnabled(true);
        this.poller = new SwingWorker<Void, String>() {
            private int retryCount = 0;

            @Override
            protected Void doInBackground() throws Exception {
                while (!this.isCancelled()) {
                    try {
                        URL url = URI.create(BlantConfig.getProgressUrl() + jobId).toURL();
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2000);

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            retryCount = 0;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            String response = sb.toString();
                            publish("[PING] " + response);

                            Matcher m = PROGRESS_PATTERN.matcher(response);
                            if (m.find()) {
                                int progress = Integer.parseInt(m.group(1));
                                if (progress == 1) {
                                    BlantConfig.setProgress(progress);
                                    return null;
                                }
                            } else {
                                publish("[WARN] Could not parse progress from response: " + response);
                            }
                        } else {
                            InputStream errStream = conn.getErrorStream();
                            String errBody = "";
                            if (errStream != null) {
                                BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8));
                                StringBuilder errSb = new StringBuilder();
                                String errLine;
                                while ((errLine = errReader.readLine()) != null) errSb.append(errLine);
                                errBody = ": " + errSb.toString();
                            }
                            publish("[ERROR] Server returned HTTP " + responseCode + errBody);
                            if (responseCode >= 500) {
                                publish("[ERROR] Server error encountered. Fetching BLANT stderr...");
                                String stderr = fetchStderr(jobId);
                                if (stderr != null && !stderr.isBlank()) {
                                    publish("[STDERR]\n" + stderr);
                                }
                                return null;
                            }
                        }
                        Thread.sleep(5000); // Poll every 5 second
                    } catch (Exception e) {
                        retryCount++;
                        publish(String.format("[WARN] Connection failed (%d/%d): %s", retryCount, MAX_RETRIES, e.getMessage()));

                        if (retryCount >= MAX_RETRIES) {
                            publish("[ERROR] Could not reach the BLANT server after " + MAX_RETRIES + " attempts. The session may have timed out — please wait a few minutes and try again.");
                            return null;
                        }
                        Thread.sleep(POLL_INTERVAL_MS);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    BlantLogWindow.getInstance().appendLog(chunk);
                }
            }

            @Override
            public void done() {
                BlantLogWindow.getInstance().setAbortBtnEnabled(false);
                callback.onComplete();
            }
        };
        this.poller.execute();
    }

    public static String fetchStderr(String jobId) {
        if (jobId == null) return null;
        try {
            java.net.URL url = java.net.URI.create(BlantConfig.getStderrUrl() + jobId).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void stopPolling() {
        if (this.poller != null && !this.poller.isDone()) {
            this.poller.cancel(true);
        }
    }

    public boolean isPolling() {
        return this.poller != null && !this.poller.isDone();
    }

    public void abort() {
        if (this.isPolling()) {
            BlantLogWindow logWindow = BlantLogWindow.getInstance();
            logWindow.appendLog("[INFO] Aborting BLANT task...");
            try {
                URL url = URI.create(BlantConfig.getAbortUrl()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");

                int status = conn.getResponseCode();
                if (status == 200) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Aborted: " + BlantConfig.getJobId()));
                    this.stopPolling();
                } else {
                    logWindow.appendLog("[ERROR] Failed to abort BLANT task. Server returned: " + status);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Failed to abort. Server returned: " + status));
                }
            } catch (IOException ex) {
                logWindow.appendLog("[ERROR] Failed to abort BLANT task: " + ex.getMessage());
            }
        }
    }
}
