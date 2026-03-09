package com.blant.edgepredict.internal.task;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

public class ImportGraph {

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final CyLayoutAlgorithmManager layoutManager;

    public ImportGraph(CyNetworkFactory networkFactory,
                       CyNetworkManager networkManager,
                       CyNetworkViewFactory networkViewFactory,
                       CyNetworkViewManager networkViewManager,
                       CyLayoutAlgorithmManager layoutManager) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;
    }

    public void importFile() throws Exception {

        BlantLogWindow logWindow = BlantLogWindow.getInstance();

        // Check a job ID is available
        String jobId = BlantConfig.getJobId();
        if (jobId == null || jobId.isBlank()) {
            JOptionPane.showMessageDialog(null, "No BLANT job ID found. Please run 'Send to BLANT' first.");
            return;
        }
        BlantPoller poller = BlantPoller.getInstance();

        logWindow.appendLog("[INFO] Fetching result for job ID: " + jobId);

        // Ping the result endpoint
        URL url = new URL(BlantConfig.RESULTS_URL + jobId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != 200) {
            String error = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "Unknown error";
            logWindow.appendLog("[ERROR] Failed to fetch result: " + error);
            JOptionPane.showMessageDialog(null, "Failed to fetch BLANT result: " + error);
            return;
        }

        String responseText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        logWindow.appendLog("[INFO] Result received. Parsing network...");
        logWindow.appendLog("[DEBUG] Raw response:\n" + responseText);
        poller.stopPolling();

        // Create a new network
        CyNetwork network = networkFactory.createNetwork();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "BLANT Result");

        if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
            network.getDefaultEdgeTable().createColumn("confidence_score", Double.class, false);
        }

        // Parse response lines into edge data
        List<String[]> edgeDataList = new ArrayList<>();
        for (String line : responseText.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            StringTokenizer st = new StringTokenizer(line, " \t");
            int count = st.countTokens();
            String[] row = new String[count];
            for (int i = 0; i < count; i++) {
                row[i] = st.nextToken();
            }
            edgeDataList.add(row);
        }

        // Add nodes and edges
        Map<String, CyNode> nodeMap = new HashMap<>();
        int added = 0;

        for (String[] row : edgeDataList) {
            if (row.length < 3) continue;

            String sourceName = row[0];
            String interaction = row[1];
            String targetName = row[2];
            Double score = null;
            if (row.length >= 4) {
                try {
                    score = Double.parseDouble(row[3]);
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
            network.getRow(edge).set(CyEdge.INTERACTION, interaction);
            if (score != null) {
                network.getRow(edge).set("confidence_score", score);
            }
            added++;
        }

        // Register network and create view
        networkManager.addNetwork(network);
        CyNetworkView view = networkViewFactory.createNetworkView(network);
        networkViewManager.addNetworkView(view);

        // Apply Cytoscape's default layout
        CyLayoutAlgorithm layout = layoutManager.getDefaultLayout();
        TaskIterator it = layout.createTaskIterator(view, layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, null);
        while (it.hasNext()) {
            it.next().run(new TaskMonitor() {
                public void setTitle(String t) {}
                public void setProgress(double p) {}
                public void setStatusMessage(String m) {}
                public void showMessage(TaskMonitor.Level l, String m) {}
            });
        }
        view.updateView();

        logWindow.appendLog("[INFO] Network loaded: " + added + " edges added.");
        JOptionPane.showMessageDialog(null, "Import complete: " + added + " edges added.");
    }
}