package com.blant.edgepredict.internal.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.swing.SwingWorker;

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

    public void startPolling(String jobId, PollingCallback callback) {
        poller = new SwingWorker<Void, String>() {
            private final int MAX_RETRIES = 5;
            private int retryCount = 0;

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
                            retryCount = 0;
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
                            String progressStr = response.split("\"progress\"\\s*:\\s* ")[1]
                                                        .split("[,}]")[0]
                                                        .trim();

                            int progress = Integer.parseInt(progressStr);
                            
                            // Stop polling if progress is 1
                            if (progress == 1) {
                                BlantConfig.setProgress(progress);
                                return null;
                            }
                        }
                        Thread.sleep(5000); // Poll every 5 second
                    } catch (Exception e) {
                        retryCount++;
                        publish(String.format("[WARN] Connection failed (%d/%d): %s", retryCount, MAX_RETRIES, e.getMessage()));

                        if (retryCount >= MAX_RETRIES) {
                            publish("[ERROR] Max retries reached. Aborting polling.");
                            cancel(true);
                        }
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

            @Override
            public void done() {
                callback.onComplete();
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