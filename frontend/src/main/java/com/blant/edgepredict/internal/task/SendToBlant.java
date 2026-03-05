package com.blant.edgepredict.internal.task;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import com.blant.edgepredict.internal.ui.BlantLogWindow;

public class SendToBlant {

    private final FileUtil fileUtil;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;

    public SendToBlant(FileUtil fileUtil,
                       CyNetworkFactory networkFactory,
                       CyNetworkManager networkManager,
                       CyNetworkViewFactory networkViewFactory,
                       CyNetworkViewManager networkViewManager) {
        this.fileUtil = fileUtil;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
    }

    public void send() throws Exception {

        FileChooserFilter filter = new FileChooserFilter("Network files (txt, csv, sif)", new String[]{"txt", "csv", "sif"});

        File file = fileUtil.getFile(
                JOptionPane.getRootFrame(),
                "Select Network File for BLANT",
                FileUtil.LOAD,
                Collections.singletonList(filter)
        );

        if (file == null) return;

        // Open log window as soon as file is selected
        BlantLogWindow logWindow = new BlantLogWindow();
        logWindow.setVisible(true);
        logWindow.appendLog("[INFO] File selected: " + file.getName());
        logWindow.appendLog("[INFO] Sending to BLANT...");
        logWindow.startPolling();

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

            URL url = new URL("http://localhost:55161/blant");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(baos.toByteArray());
            }

            int status = conn.getResponseCode();

            if (status == 200) {
                logWindow.stopPolling();
                logWindow.appendLog("[INFO] BLANT processing complete. Loading result...");

                byte[] responseBytes = conn.getInputStream().readAllBytes();
                String responseText = new String(responseBytes, StandardCharsets.UTF_8);

                CyNetwork network = networkFactory.createNetwork();
                network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "BLANT Result");

                if (network.getDefaultEdgeTable().getColumn("score") == null) {
                    network.getDefaultEdgeTable().createColumn("score", Double.class, false);
                }

                Map<String, CyNode> nodeMap = new HashMap<>();

                for (String line : responseText.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length < 3) continue;

                    String sourceName = parts[0];
                    String interaction = parts[1];
                    String targetName = parts[2];
                    Double score = null;
                    if (parts.length >= 4) {
                        try {
                            score = Double.parseDouble(parts[3]);
                        } catch (NumberFormatException ignored) {}
                    }

                    CyNode source = nodeMap.computeIfAbsent(sourceName, name -> {
                        CyNode n = network.addNode();
                        network.getRow(n).set(CyNetwork.NAME, name);
                        return n;
                    });

                    CyNode target = nodeMap.computeIfAbsent(targetName, name -> {
                        CyNode n = network.addNode();
                        network.getRow(n).set(CyNetwork.NAME, name);
                        return n;
                    });

                    CyEdge edge = network.addEdge(source, target, false);
                    network.getRow(edge).set(CyNetwork.NAME, sourceName + " (" + interaction + ") " + targetName);
                    network.getRow(edge).set("interaction", interaction);
                    if (score != null) {
                        network.getRow(edge).set("score", score);
                    }
                }

                networkManager.addNetwork(network);
                CyNetworkView view = networkViewFactory.createNetworkView(network);
                networkViewManager.addNetworkView(view);

                // Position nodes in a circle so they aren't stacked
                List<CyNode> nodes = network.getNodeList();
                int total = nodes.size();
                double radius = 100.0 * total;
                for (int i = 0; i < total; i++) {
                    double angle = 2 * Math.PI * i / total;
                    double x = radius * Math.cos(angle);
                    double y = radius * Math.sin(angle);
                    View<CyNode> nodeView = view.getNodeView(nodes.get(i));
                    if (nodeView != null) {
                        nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
                        nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
                    }
                }
                view.updateView();

                logWindow.appendLog("[INFO] Network loaded into Cytoscape successfully.");

            } else {
                logWindow.stopPolling();
                byte[] errorBytes = conn.getErrorStream() != null
                        ? conn.getErrorStream().readAllBytes()
                        : "Unknown error".getBytes(StandardCharsets.UTF_8);
                String error = new String(errorBytes, StandardCharsets.UTF_8);
                logWindow.appendLog("[ERROR] BLANT request failed: " + error);
            }

        } catch (Exception ex) {
            logWindow.stopPolling();
            logWindow.appendLog("[ERROR] Exception: " + ex.getMessage());
            throw ex;
        }
    }
}