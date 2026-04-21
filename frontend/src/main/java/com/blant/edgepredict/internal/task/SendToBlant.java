package com.blant.edgepredict.internal.task;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.CacheUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.UUID;
import javax.swing.JOptionPane;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;

public class SendToBlant {
   private final FileUtil fileUtil;
   private final CyNetworkFactory networkFactory;
   private final CyNetworkManager networkManager;
   private final CyNetworkViewFactory networkViewFactory;
   private final CyNetworkViewManager networkViewManager;
   private final BlantLogWindow logWindow;
   private final String sampleMethod;
   private final int precisionDigits;
   private final int kVal;
   private final boolean isSaved;
   private final boolean isMultithreaded;

   public SendToBlant(FileUtil fileUtil, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, String sampleMethod, int precisionDigits, int kVal, boolean isSaved, boolean isMultithreaded, BlantLogWindow logWindow) {
      this.fileUtil = fileUtil;
      this.networkFactory = networkFactory;
      this.networkManager = networkManager;
      this.networkViewFactory = networkViewFactory;
      this.networkViewManager = networkViewManager;
      this.logWindow = logWindow;
      this.sampleMethod = sampleMethod;
      this.precisionDigits = precisionDigits;
      this.kVal = kVal;
      this.isSaved = isSaved;
      this.isMultithreaded = isMultithreaded;
   }

   public boolean send() throws Exception {
      FileChooserFilter filter = new FileChooserFilter("Network files (txt, csv, sif)", new String[]{"txt", "csv", "sif"});
      File file = this.fileUtil.getFile(JOptionPane.getRootFrame(), "Select Network File for BLANT", 0, Collections.singletonList(filter));
      if (file == null) {
         return false;
      } else {
         this.logWindow.setVisible(true);
         this.logWindow.appendLog("[INFO] File selected: " + file.getName());
         this.logWindow.appendLog("[INFO] Sending to BLANT...");

         try {
            String boundary = UUID.randomUUID().toString();
            String LINE_FEED = "\r\n";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String headers = "--" + boundary + LINE_FEED + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_FEED + "Content-Type: application/octet-stream" + LINE_FEED + LINE_FEED;
            baos.write(headers.getBytes(StandardCharsets.UTF_8));
            baos.write(Files.readAllBytes(file.toPath()));
            String sampleMethodPart = "--" + boundary + LINE_FEED + "Content-Disposition: form-data; name=\"sample_method\"" + LINE_FEED + LINE_FEED + this.sampleMethod + LINE_FEED;
            baos.write(sampleMethodPart.getBytes(StandardCharsets.UTF_8));
            String precisionDigitsPart = "--" + boundary + LINE_FEED + "Content-Disposition: form-data; name=\"precision_digits\"" + LINE_FEED + LINE_FEED + this.precisionDigits + LINE_FEED;
            baos.write(precisionDigitsPart.getBytes(StandardCharsets.UTF_8));
            String kPart = "--" + boundary + LINE_FEED + "Content-Disposition: form-data; name=\"k\"" + LINE_FEED + LINE_FEED + this.kVal + LINE_FEED;
            baos.write(kPart.getBytes(StandardCharsets.UTF_8));
            baos.write((LINE_FEED + "--" + boundary + "--" + LINE_FEED).getBytes(StandardCharsets.UTF_8));
            URL url = new URL(BlantConfig.SUBMIT_URL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try {
               OutputStream os = conn.getOutputStream();
               try {
                  os.write(baos.toByteArray());
               } catch (Throwable var17) {
                  if (os != null) {
                     try {
                        os.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }
                  throw var17;
               }
               if (os != null) {
                  os.close();
               }
            } catch (Exception e) {
               this.logWindow.appendLog("[ERROR] Failed to send file: " + e.getMessage());
               throw e;
            }

            int status = conn.getResponseCode();
            this.logWindow.appendLog("[INFO] Server responded with HTTP " + status);

            // Read whichever stream is available
            InputStream responseStream = (status >= 200 && status < 300)
                  ? conn.getInputStream()
                  : conn.getErrorStream();

            if (responseStream == null) {
               this.logWindow.appendLog("[ERROR] Server returned HTTP " + status + " with no response body.");
               return false;
            }

            String responseText = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            this.logWindow.appendLog("[DEBUG] Server response: " + responseText);

            if (status == 200 || status == 409) {
               if (!responseText.contains("\"job_id\"")) {
                  this.logWindow.appendLog("[ERROR] Response did not contain a job_id: " + responseText);
                  return false;
               }

               String jobId = responseText.split("\"job_id\"\\s*:\\s*\"")[1].split("\"")[0];
               BlantConfig.setJobId(jobId);
               this.logWindow.appendLog("[INFO] Job ID: " + jobId);

               if (status == 200) {
                  this.logWindow.appendLog("[INFO] File sent successfully. Awaiting response...");
                  if (this.isSaved) {
                     this.logWindow.appendLog(CacheUtil.saveInput(jobId, new String(baos.toByteArray(), StandardCharsets.UTF_8)));
                  }
               } else {
                  // 409 means job already exists for this file
                  this.logWindow.appendLog("[INFO] Job already exists on server (HTTP 409). Using existing job ID.");
               }

               return true;
            } else {
               this.logWindow.appendLog("[ERROR] Unexpected HTTP status " + status + ": " + responseText);
               return false;
            }

         } catch (Exception ex) {
            this.logWindow.appendLog("[ERROR] Exception: " + ex.getMessage());
            throw ex;
         }
      }
   }
}