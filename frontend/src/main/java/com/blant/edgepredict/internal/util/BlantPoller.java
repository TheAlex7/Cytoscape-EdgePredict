package com.blant.edgepredict.internal.util;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.SwingWorker;

public class BlantPoller {
   private static BlantPoller instance;
   private SwingWorker<Void, String> poller;

   private BlantPoller() {
   }

   public static BlantPoller getInstance() {
      if (instance == null) {
         instance = new BlantPoller();
      }

      return instance;
   }

   public void startPolling(final String jobId, final PollingCallback callback) {
      this.poller = new SwingWorker<Void, String>() {
         private final int MAX_RETRIES = 5;
         private int retryCount = 0;

         protected Void doInBackground() throws Exception {
            while(!this.isCancelled()) {
               try {
                  URL url = new URL(BlantConfig.PROGRESS_URL + jobId);
                  HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                  conn.setRequestMethod("GET");
                  conn.setConnectTimeout(2000);
                  conn.setReadTimeout(2000);
                  if (conn.getResponseCode() == 200) {
                     this.retryCount = 0;
                     BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                     StringBuilder sb = new StringBuilder();

                     String line;
                     while((line = reader.readLine()) != null) {
                        sb.append(line);
                     }

                     String response = sb.toString();
                     this.publish(new String[]{"[PING] " + response});
                     String progressStr = response.split("\"progress\"\\s*:\\s* ")[1].split("[,}]")[0].trim();
                     int progress = Integer.parseInt(progressStr);
                     if (progress == 1) {
                        BlantConfig.setProgress(progress);
                        return null;
                     }
                  }

                  Thread.sleep(5000L);
               } catch (Exception e) {
                  ++this.retryCount;
                  this.publish(new String[]{String.format("[WARN] Connection failed (%d/%d): %s", this.retryCount, 5, e.getMessage())});
                  if (this.retryCount >= 5) {
                     this.publish(new String[]{"[ERROR] Max retries reached. Aborting polling."});
                     this.cancel(true);
                  }
               }
            }

            return null;
         }

         protected void process(List<String> chunks) {
            for(String chunk : chunks) {
               BlantLogWindow.getInstance().appendLog(chunk);
            }

         }

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
