package com.blant.edgepredict.internal.util;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.ui.BlantLogWindow;

public class BlantPoller {

    private static BlantPoller instance;
    private SwingWorker<Void, String> poller;

    private BlantPoller() {}

    public static BlantPoller getInstance() {
        if (instance == null) {
            instance = new BlantPoller();
        }
        return instance;
    }

    public void startPolling(String jobId) {
        poller = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    try {
                        URL url = new URL(BlantConfig.PROGRESS_URL + jobId);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2000);

                        if (conn.getResponseCode() == 200) {
                            BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            reader.close();

                            String response = sb.toString();
                            // Expected response format: {"progress": 0} or {"progress": 100}
                            // We will extract the "progress" value and publish it to the log window. Testing purposes
                            // publish("[PING] " + response);


                            int progress  = extractInt(response, "progress");
                            

                            if (progress == 1) {
                                publish("[DONE] Job completed successfully. Final progress: " + progress + "%");
                                cancel(true);
                            } else if (progress != 0) {
                                publish("[WARN] Unexpected state from server (progress=" + progress + "): " + response);
                                cancel(true);
                            } else {
                                publish("[PROGRESS] " + progress + "%");
                            }

                        } else {
                            publish("[ERROR] Server returned HTTP " + conn.getResponseCode());
                        }

                    
                    } catch (Exception e) {
                        publish("[ERROR] Could not reach server: " + e.getMessage());
                    }

                    if (isCancelled()) break;

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    BlantLogWindow.getInstance().appendLog(chunk);
                }
            }
        };
        poller.execute();
    }

    public void stopPolling() {
        if (poller != null && !poller.isDone()) {
            poller.cancel(true);
        }
    }

    // ---------------------------------------------------------------- utils --

    /**
     * Extracts an integer value from a flat JSON string.
     * Handles: {"key": 42} and {"key":42} and {"key": 42, ...}
     */
    private int extractInt(String json, String key) {
        String[] parts = json.split("\"" + key + "\"\\s*:\\s*");
        if (parts.length < 2) throw new NumberFormatException("Key not found: " + key);
        return Integer.parseInt(parts[1].split("[,}]")[0].trim());
    }
}