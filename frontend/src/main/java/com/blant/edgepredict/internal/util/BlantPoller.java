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

                        String response = sb.toString();

                        publish("[PING] " + response);

                        // ISSUE: This parsing is very brittle and assumes a specific format. 
                        // A more robust solution would be to use a JSON parsing library like Jackson or Gson,
                        // but that would add an additional dependency to the project. 
                        // For now, we will keep it simple and just document the expected format in the server
                        // response. The expected format is: {"progress": <int>, "status": <string>}
                        String progressStr = response.split("\"progress\"\\s*:\\s*")[1]
                                                    .split("[,}]")[0]
                                                    .trim();

                        int progress = Integer.parseInt(progressStr);

                        // Stop polling if progress is 0
                        if (progress == 0) {
                            cancel(true);
                        }
                    }
                    } catch (Exception e) {
                        publish("[ERROR] Could not reach server: " + e.getMessage());
                    }
                    Thread.sleep(5000); // Poll every 5 seconds
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
}