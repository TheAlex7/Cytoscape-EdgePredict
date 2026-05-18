package com.blant.edgepredict.internal.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern JOB_ID_PATTERN = Pattern.compile("\"job(?:_id|ID)\"\\s*:\\s*\"([^\"]+)\"");

    private final FileUtil fileUtil;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final BlantLogWindow logWindow;
    private final String sampleMethod;
    private final double precisionDigits;
    private final List<String> kVal;
    private final boolean isSaved;

    public SendToBlant(FileUtil fileUtil, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, String sampleMethod, double precisionDigits, List<String> kVal, boolean isSaved, BlantLogWindow logWindow) {
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
    }

    public File selectFile(Boolean isFile) {
        if (isFile) {
            FileChooserFilter filter = new FileChooserFilter("Network files (txt, csv, sif, el)", new String[]{"txt", "csv", "sif", "el"});
            return this.fileUtil.getFile(JOptionPane.getRootFrame(), "Select Network File for BLANT", 0, Collections.singletonList(filter));
        }
        else {
            ExportGraph exportGraph = ExportGraph.getInstance();
            try {
                return exportGraph.ExportCurrentGraph(new org.cytoscape.work.TaskMonitor() {
                public void setTitle(String s) {} public void setProgress(double p) {}
                public void setStatusMessage(String s) {} public void showMessage(org.cytoscape.work.TaskMonitor.Level l, String s) {}});
            } catch (Exception e) {
                this.logWindow.appendLog("[ERROR] Failed to export current graph: " + e.getMessage());
                return null;
            }
        }
    }

    public boolean send(File file) throws Exception {
        BlantConfig.setInputFile(file);
        this.logWindow.setVisible(true);

        this.logWindow.appendLog("========================================");
        this.logWindow.appendLog("Session starts: " + new java.util.Date().toString());
        this.logWindow.appendLog("========================================");

        this.logWindow.appendLog("[INFO] File selected: " + file.getName());
        this.logWindow.appendLog("[INFO] BLANT server selected: " + (BlantConfig.isOnline ? "Online" : "Local"));
        this.logWindow.appendLog("[INFO] Sending to BLANT...");

        try {
            // Generate multipart/form-data request body manually
            String boundary = UUID.randomUUID().toString();
            String LINE_FEED = "\r\n";

            // Input File
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String headers = "--" + boundary + LINE_FEED
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_FEED
                    + "Content-Type: application/octet-stream" + LINE_FEED + LINE_FEED;
            baos.write(headers.getBytes(StandardCharsets.UTF_8));
            baos.write(Files.readAllBytes(file.toPath()));
            baos.write((LINE_FEED + "--" + boundary + "--" + LINE_FEED).getBytes(StandardCharsets.UTF_8));
            String kParam = this.kVal.isEmpty() ? "4" : this.kVal.get(0);
            String method = this.sampleMethod == null ? "MCMC" : this.sampleMethod.replaceAll("\\s*\\(.*\\)$", "").trim();
            
            String queryParams = "?k=" + kParam + "&method=" + method + "&precision=" + this.precisionDigits;
            if (BlantConfig.getLoad()) {
                queryParams += "&force=1";
                BlantConfig.setLoad(false);
            }
            URL url = new URL(BlantConfig.getSubmitUrl() + queryParams);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(baos.toByteArray());
            } catch (Exception e) {
                this.logWindow.appendLog("[ERROR] Failed to send file: " + e.getMessage());
                throw e;
            }

            int status = conn.getResponseCode();

            InputStream responseStream = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (responseStream == null) {
                this.logWindow.appendLog("[ERROR] Server returned HTTP " + status + " with no response body.");
                return false;
            }

            String responseText = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

            if (status == 200 || status == 409) {
                Matcher m = JOB_ID_PATTERN.matcher(responseText);
                if (!m.find()) {
                    this.logWindow.appendLog("[ERROR] Could not extract job_id from response: " + responseText);
                    return false;
                }
                String jobId = m.group(1);
                BlantConfig.setJobId(jobId);
                this.logWindow.appendLog("[INFO] Job ID: " + jobId);

                if (status == 200) {
                this.logWindow.appendLog("[INFO] File sent successfully. Awaiting response...");
                    if (this.isSaved) {
                        this.logWindow.appendLog(CacheUtil.saveInput(jobId, new String(baos.toByteArray(), StandardCharsets.UTF_8)));
                    }
                    return true;
                } else {
                    // HTTP 409: job already exists on server
                    this.logWindow.appendLog("[INFO] " + status + ": " + responseText);
                    String cached = CacheUtil.getOutput(jobId);
                    if (cached.startsWith("[ERROR]")) {
                        // Not in local cache — download it from the server
                        this.logWindow.appendLog("[INFO] Result not cached locally, downloading from server...");
                        try {
                            URL resultUrl = new URL(BlantConfig.getResultUrl() + jobId);
                            HttpURLConnection resultConn = (HttpURLConnection) resultUrl.openConnection();
                            resultConn.setRequestMethod("GET");
                            int resultStatus = resultConn.getResponseCode();
                            if (resultStatus == 200) {
                                cached = new String(resultConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                                this.logWindow.appendLog(CacheUtil.saveOutput(jobId, cached));
                                this.logWindow.appendLog("[INFO] Result downloaded and cached.");
                            } else {
                                this.logWindow.appendLog("[WARN] Server returned HTTP " + resultStatus + " for result — will retry with force.");
                            }
                        } catch (Exception downloadEx) {
                            this.logWindow.appendLog("[WARN] Could not download result: " + downloadEx.getMessage());
                        }
                    } else {
                        this.logWindow.appendLog("[INFO] Loaded result from local cache.");
                    }
                    BlantConfig.setLoad(true);
                    return !cached.startsWith("[ERROR]");
                }
            } else {
                this.logWindow.appendLog("[ERROR] Server returned HTTP " + status + ": " + responseText);
            }
        } catch (Exception ex) {
            this.logWindow.appendLog("[ERROR] Exception: " + ex.getMessage());
            throw ex;
        }

        return false;
    }
}
