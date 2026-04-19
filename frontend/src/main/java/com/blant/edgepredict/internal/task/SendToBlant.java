package com.blant.edgepredict.internal.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.CacheUtil;


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

    public SendToBlant(FileUtil fileUtil,
                       CyNetworkFactory networkFactory,
                       CyNetworkManager networkManager,
                       CyNetworkViewFactory networkViewFactory,
                       CyNetworkViewManager networkViewManager,
                       String sampleMethod,
                       int precisionDigits,
                       int kVal,
                       boolean isSaved,
                       boolean isMultithreaded,
                       BlantLogWindow logWindow) {
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

        File file = fileUtil.getFile(
                JOptionPane.getRootFrame(),
                "Select Network File for BLANT",
                FileUtil.LOAD,
                Collections.singletonList(filter)
        );
        if (file == null) return false;

        // Open log window as soon as file is selected
        this.logWindow.setVisible(true);
        this.logWindow.appendLog("[INFO] File selected: " + file.getName());
        
        // ByteArrayOutputStream baos_1 = new ByteArrayOutputStream();
        // String content = "";
        // baos_1.write(content.getBytes(StandardCharsets.UTF_8));
        // baos_1.write(Files.readAllBytes(file.toPath()));
        // if (CacheUtil.getOutputAttempt(content, logWindow)) {
            //     this.logWindow.appendLog("[INFO] Found locally saved result. Loading cached file...");
            //     BlantConfig.setLoad(true);
            //     BlantConfig.setJobId("load");
            //     return true;
            // }
            
        // Send file to BLANT server
        this.logWindow.appendLog("[INFO] Sending to BLANT...");
        try {
            // Generate multipart/form-data request body manually
            String boundary = UUID.randomUUID().toString();
            String LINE_FEED = "\r\n";

            // Input File
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String headers = "--" + boundary + LINE_FEED
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_FEED
                    + "Content-Type: application/octet-stream" + LINE_FEED
                    + LINE_FEED;
            baos.write(headers.getBytes(StandardCharsets.UTF_8));
            baos.write(Files.readAllBytes(file.toPath()));

            // Sample Method
            String sampleMethodPart = "--" + boundary + LINE_FEED
                        + "Content-Disposition: form-data; name=\"sample_method\"" + LINE_FEED
                        + LINE_FEED
                        + this.sampleMethod + LINE_FEED;
            baos.write(sampleMethodPart.getBytes(StandardCharsets.UTF_8));

            // Precision Digits
            String precisionDigitsPart = "--" + boundary + LINE_FEED
                        + "Content-Disposition: form-data; name=\"precision_digits\"" + LINE_FEED
                        + LINE_FEED
                        + this.precisionDigits + LINE_FEED;
            baos.write(precisionDigitsPart.getBytes(StandardCharsets.UTF_8));

            // K Value
            String kPart = "--" + boundary + LINE_FEED
                        + "Content-Disposition: form-data; name=\"k\"" + LINE_FEED
                        + LINE_FEED
                        + this.kVal + LINE_FEED;
            baos.write(kPart.getBytes(StandardCharsets.UTF_8));

            baos.write((LINE_FEED + "--" + boundary + "--" + LINE_FEED).getBytes(StandardCharsets.UTF_8));

            URL url = new URL(BlantConfig.SUBMIT_URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // Write the multipart request body to the connection output stream
            try (OutputStream os = conn.getOutputStream()) {
                os.write(baos.toByteArray());
            } catch (Exception e) {
                this.logWindow.appendLog("[ERROR] Failed to send file: " + e.getMessage());
                throw e;
            };
            int status = conn.getResponseCode();

            // If submission is successful, we expect the response to contain a job ID which we will use to poll for results
            if (status == 200 || status == 409) {

                // Read response to extract job ID
                byte[] responseBytes = conn.getInputStream().readAllBytes();
                String responseText = new String(responseBytes, StandardCharsets.UTF_8);
                String jobId = responseText.split("\"job_id\"\\s*:\\s*\"")[1].split("\"")[0];

                // Save job ID to config for polling
                BlantConfig.setJobId(jobId);
                this.logWindow.appendLog("[INFO] Job ID: " + jobId);

                if (status == 200) {
                    this.logWindow.appendLog("[INFO] File sent successfully. Awaiting response...");
                    // Save Input logs if user has enabled save option
                    if (this.isSaved){ 
                        this.logWindow.appendLog(CacheUtil.saveInput(jobId, new String(baos.toByteArray(), StandardCharsets.UTF_8)));
                        return true;
                    }
                }
            }

        } catch (Exception ex) {
            this.logWindow.appendLog("[ERROR] Exception: " + ex.getMessage());
            throw ex;
        }

        return true;
    }
}