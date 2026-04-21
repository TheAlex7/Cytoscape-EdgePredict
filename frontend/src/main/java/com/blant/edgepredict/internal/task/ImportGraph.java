package com.blant.edgepredict.internal.task;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

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
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.ui.NavDashboard;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;
import com.blant.edgepredict.internal.util.VisualUtil;

import java.awt.Color;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.view.vizmap.*;

public class ImportGraph {

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final CyLayoutAlgorithmManager layoutManager;

    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmfDiscrete;
    private final VisualMappingFunctionFactory vmfPassthrough;
    private final VisualStyleFactory vsFactory;
<<<<<<< Updated upstream
=======
    private final boolean isSaved;
    private final BlantLogWindow logWindow;
>>>>>>> Stashed changes

    public ImportGraph(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory,
            CyNetworkViewManager networkViewManager,
            CyLayoutAlgorithmManager layoutManager,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory vmfDiscrete,
            VisualMappingFunctionFactory vmfPassthrough,
            VisualStyleFactory vsFactory) {

        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;
        this.vmm = vmm;
        this.vmfDiscrete = vmfDiscrete;
        this.vmfPassthrough = vmfPassthrough;
        this.vsFactory = vsFactory;
    }

    public void importFile() throws Exception {

        BlantLogWindow logWindow = BlantLogWindow.getInstance();

        String jobId = BlantConfig.getJobId();
        if (jobId == null || jobId.isBlank()) {
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null,
                        "No BLANT job ID found. Please run 'Send to BLANT' first."));
            return;
        }

<<<<<<< Updated upstream
        BlantPoller poller = BlantPoller.getInstance();

        logWindow.appendLog("[INFO] Fetching result for job ID: " + jobId);

=======
        if (BlantConfig.getLoad()) {
            String responseText = CacheUtil.getOutput(jobId);
            this.logWindow.appendLog(responseText);
            if (responseText.contains("ERROR")) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "Locally saved file cannot be read."));
                return;
            }
            // Run processResult on EDT
            final String text = responseText;
            SwingUtilities.invokeLater(() -> {
                try { processResult(text, jobId); }
                catch (Exception ex) { ex.printStackTrace(); }
            });
            return;
        }

        this.logWindow.appendLog("[INFO] Fetching result for job ID: " + jobId);

>>>>>>> Stashed changes
        URL url = new URL(BlantConfig.RESULTS_URL + jobId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != 200) {
            String error = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "Unknown error";
<<<<<<< Updated upstream
            logWindow.appendLog("[ERROR] Failed to fetch result: " + error);
            JOptionPane.showMessageDialog(null, "Failed to fetch BLANT result: " + error);
            return;
        }
=======
            this.logWindow.appendLog("[ERROR] Failed to fetch result: " + error);
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, "Failed to fetch BLANT result: " + error));
            return;
        }

        String responseText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        this.logWindow.appendLog("[INFO] Result received. Parsing network...");
        this.logWindow.appendLog("[DEBUG] Raw response:\n" + responseText);

        // Run processResult on EDT so all Cytoscape view/model calls
        // happen on the correct thread and the slider update works
        final String finalResponse = responseText;
        final String finalJobId = jobId;
        SwingUtilities.invokeLater(() -> {
            try { processResult(finalResponse, finalJobId); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
    }
>>>>>>> Stashed changes

        String responseText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        logWindow.appendLog("[INFO] Result received. Parsing network...");
        logWindow.appendLog("[DEBUG] Raw response:\n" + responseText);

        poller.stopPolling();

        CyNetwork network = networkFactory.createNetwork();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "BLANT Result");

        // Create edge columns
        if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
            network.getDefaultEdgeTable().createColumn("confidence_score", Double.class, false);
        }
        if (network.getDefaultEdgeTable().getColumn("orbit_pair") == null) {
            network.getDefaultEdgeTable().createColumn("orbit_pair", String.class, false);
        }

        Map<String, CyNode> nodeMap = new HashMap<>();
        int added = 0;

        VisualUtil.showTableDialogue(responseText);

        for (String line : responseText.split("\n")) {

            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] row = line.split("\\s+");
<<<<<<< Updated upstream
            if (row.length < 4) {
                logWindow.appendLog("[WARN] Skipping malformed line: " + line);
=======
            if (row.length < 3) {
                this.logWindow.appendLog("[WARN] Skipping malformed line: " + line);
>>>>>>> Stashed changes
                continue;
            }

            String sourceName  = row[0];
            String interaction = "predicted";
            String targetName  = row[1];

            Double score = null;
            try {
                score = Double.parseDouble(row[2]);
            } catch (Exception e) {
                logWindow.appendLog("[WARN] Could not parse score: " + line);
            }

            String orbitPair = row.length >= 4 ? row[3] : null;

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
            network.getRow(edge).set(CyNetwork.NAME,
                    sourceName + " (" + interaction + ") " + targetName);
            network.getRow(edge).set(CyEdge.INTERACTION, interaction);

            if (score != null) {
                network.getRow(edge).set("confidence_score", score);
            }
            if (orbitPair != null) {
                network.getRow(edge).set("orbit_pair", orbitPair);
            }

            added++;
<<<<<<< Updated upstream
            logWindow.appendLog("[DEBUG] Edge: " + sourceName + " -> " + targetName +
                    " (" + interaction + ") score=" + score);
=======
            this.logWindow.appendLog("[DEBUG] Edge: " + sourceName + " -> " + targetName +
                    " (" + interaction + ") score=" + score + " orbit_pair=" + orbitPair);
>>>>>>> Stashed changes
        }

        // --- Compute min/max confidence score across all edges ---
        double scoreMin = Double.MAX_VALUE;
        double scoreMax = -Double.MAX_VALUE;

        for (CyEdge edge : network.getEdgeList()) {
            Double score = network.getRow(edge).get("confidence_score", Double.class);
            if (score != null) {
                if (score < scoreMin) scoreMin = score;
                if (score > scoreMax) scoreMax = score;
            }
        }

        if (scoreMin == Double.MAX_VALUE) {
            scoreMin = 0.0;
            scoreMax = 1.0;
        }

        final double finalMin = scoreMin;
        final double finalMax = scoreMax;
        final int finalAdded = added;

        logWindow.appendLog("[INFO] Score range: min=" + finalMin + ", max=" + finalMax);

        networkManager.addNetwork(network);

        CyNetworkView view = networkViewFactory.createNetworkView(network);
        networkViewManager.addNetworkView(view);

        CyLayoutAlgorithm layout = layoutManager.getDefaultLayout();
        TaskIterator it = layout.createTaskIterator(
                view,
                layout.getDefaultLayoutContext(),
                CyLayoutAlgorithm.ALL_NODE_VIEWS,
                null);

        while (it.hasNext()) {
            it.next().run(new TaskMonitor() {
                public void setTitle(String t) {}
                public void setProgress(double p) {}
                public void setStatusMessage(String m) {}
                public void showMessage(TaskMonitor.Level l, String m) {}
            });
        }

        VisualUtil.applyStyles(view, vmm, vmfDiscrete, vmfPassthrough, vsFactory);

        // --- Apply gradient colors + tooltips to all edges ---
        view.getEdgeViews().forEach(ev -> {
            CyEdge edgeModel = ev.getModel();
            Double score     = network.getRow(edgeModel).get("confidence_score", Double.class);
            String orbitPair = network.getRow(edgeModel).get("orbit_pair", String.class);
            String edgeName  = network.getRow(edgeModel).get(CyNetwork.NAME, String.class);

<<<<<<< Updated upstream
            Color gradientColor = scoreToColor(score, finalMin, finalMax);
            ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
            ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
=======
            if (score != null) {
                Color gradientColor = VisualUtil.scoreToColor(score, finalMin, finalMax);
                ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
                ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
            }

            String tooltip = "<html><b>" + edgeName + "</b><br>"
                    + "Score: " + (score != null ? String.format("%.6f", score) : "—") + "<br>"
                    + "Orbit Pair: " + (orbitPair != null ? orbitPair : "—") + "</html>";
            ev.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
>>>>>>> Stashed changes
        });

        view.updateView();

<<<<<<< Updated upstream
        logWindow.appendLog("[INFO] Network loaded: " + added + " edges added.");
=======
        this.logWindow.appendLog("[INFO] Network loaded: " + finalAdded + " edges added.");

        if (isSaved) {
            this.logWindow.appendLog(CacheUtil.saveOutput(jobId, responseText));
        }
>>>>>>> Stashed changes

        // --- Push score range + view to dashboard, then show completion dialog ---
        NavDashboard dashboard = NavDashboard.getExistingInstance();
        if (dashboard != null) {
            dashboard.setScoreRange(finalMin, finalMax, view);
        }

        JOptionPane.showMessageDialog(null,
                "Import complete: " + finalAdded + " edges added.\n" +
                String.format("Score range: %.4f – %.4f", finalMin, finalMax));
    }

    /**
     * Maps a confidence score to a color on a blue->red gradient.
     * Low score = blue (#0000FF), high score = red (#FF0000).
     */
    public static Color scoreToColor(double score, double min, double max) {
        float t = (max == min) ? 1f : (float) ((score - min) / (max - min));
        t = Math.max(0f, Math.min(1f, t));
        return new Color(t, 0f, 1f - t);
    }
}