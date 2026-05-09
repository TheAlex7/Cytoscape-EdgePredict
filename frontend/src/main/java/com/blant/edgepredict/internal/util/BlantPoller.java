package com.blant.edgepredict.internal.util;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;

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

    public void startPolling(final String jobId, final boolean isOnline, final PollingCallback callback) {
        this.poller = new SwingWorker<Void, String>() {
            private int retryCount = 0;

            @Override
            protected Void doInBackground() throws Exception {
                while (!this.isCancelled()) {
                    try {
                        URL url = new URL(BlantConfig.getProgressUrl(isOnline) + jobId);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2000);

                        if (conn.getResponseCode() == 200) {
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
                        }

                        Thread.sleep(POLL_INTERVAL_MS);
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
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    BlantLogWindow.getInstance().appendLog(chunk);
                }
            }

            @Override
            public void done() {
                callback.onComplete();
            }
        };
        this.poller.execute();
    }

    public void stopPolling() {
        if (this.poller != null && !this.poller.isDone()) {
            this.poller.cancel(true);
        }
    }
}
