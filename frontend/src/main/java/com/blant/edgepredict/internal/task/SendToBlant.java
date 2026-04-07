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
import com.blant.edgepredict.internal.util.BlantPoller;


public class SendToBlant {

    private final FileUtil fileUtil;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private String sampleMethod;
    private int precisionDigits;
    private int kVal;
    private boolean isSaved;

    public SendToBlant(FileUtil fileUtil,
                       CyNetworkFactory networkFactory,
                       CyNetworkManager networkManager,
                       CyNetworkViewFactory networkViewFactory,
                       CyNetworkViewManager networkViewManager,
                       String sampleMethod,
                       int precisionDigits,
                       int kVal,
                       boolean isSaved) {
        this.fileUtil = fileUtil;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.sampleMethod = sampleMethod;
        this.precisionDigits = precisionDigits;
        this.kVal = kVal;
        this.isSaved = isSaved;
    }

    public void send() throws Exception {

        FileChooserFilter filter = new FileChooserFilter("Network files (txt, csv, sif, el)", new String[]{"txt", "csv", "sif", "el"});

        File file = fileUtil.getFile(
                JOptionPane.getRootFrame(),
                "Select Network File for Edge Prediction",
                FileUtil.LOAD,
                Collections.singletonList(filter)
        );
        if (file == null) return;

        // Open log window as soon as file is selected
        BlantLogWindow logWindow = BlantLogWindow.getInstance();
        BlantPoller poller = BlantPoller.getInstance();
        logWindow.setVisible(true);
        logWindow.appendLog("[INFO] File selected: " + file.getName());
        logWindow.appendLog("[INFO] Sending to Edge Prediction...");
        

        try {
            String boundary = UUID.randomUUID().toString();
            String LINE_FEED = "\r\n";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String headers = "--" + boundary + LINE_FEED
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_FEED
                    + "Content-Type: application/octet-stream" + LINE_FEED
                    + LINE_FEED;
            baos.write(headers.getBytes(StandardCharsets.UTF_8));
            baos.write(Files.readAllBytes(file.toPath()));
            baos.write((LINE_FEED + "--" + boundary + "--" + LINE_FEED).getBytes(StandardCharsets.UTF_8));

            URL url = new URL(BlantConfig.SUBMIT_URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(baos.toByteArray());
            } catch (Exception e) {
                poller.stopPolling();
                logWindow.appendLog("[ERROR] Failed to send file: " + e.getMessage());
                throw e;
            };
            int status = conn.getResponseCode();

            // If submission is successful, we expect the response to contain a job ID which we will use to poll for results
            if (status == 200) {
                poller.stopPolling();
                logWindow.appendLog("[INFO] Edge Prediction processing complete. Loading result...");
                byte[] responseBytes = conn.getInputStream().readAllBytes();
                String responseText = new String(responseBytes, StandardCharsets.UTF_8);
                // ISSUE: Solution for now
                // Assuming the response is a JSON string like {"job_id": "12345"}, we extract the job ID (this is to avoid adding a JSON library dependency just for this)
                // JSON parsing is very basic and brittle here, but it should work as long as the server response format doesn't change
                // importing json library prevented app from loading so we are doing this manually for now
                String jobId = responseText.split("\"job_id\"\\s*:\\s*\"")[1].split("\"")[0];
                BlantConfig.setJobId(jobId);
                logWindow.appendLog("[INFO] Job ID: " + jobId);
                poller.startPolling(BlantConfig.getJobId());
            }

        } catch (Exception ex) {
            poller.stopPolling();
            logWindow.appendLog("[ERROR] Exception: " + ex.getMessage());
            throw ex;
        };
    }
}