package com.blant.edgepredict.internal.task;

import java.awt.Color;
import java.awt.Component;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.ui.NavDashboard;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;
import com.blant.edgepredict.internal.util.CacheUtil;
import com.blant.edgepredict.internal.util.VisualUtil;

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
    private final boolean isSaved;
    private final BlantLogWindow logWindow;

    public ImportGraph(CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, boolean isSaved, BlantLogWindow logWindow) {
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;
        this.vmm = vmm;
        this.vmfDiscrete = vmfDiscrete;
        this.vmfPassthrough = vmfPassthrough;
        this.vsFactory = vsFactory;
        this.isSaved = isSaved;
        this.logWindow = logWindow;
    }

    public void importFile() throws Exception {
        String jobId = BlantConfig.getJobId();
        if (jobId == null || jobId.isBlank()) return;

        if (BlantConfig.getLoad()) {
            String responseText = CacheUtil.getOutput(jobId);
            this.logWindow.appendLog("[INFO] Loading result from cache for job: " + jobId);
            if (responseText.contains("ERROR")) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog((Component) null, "Locally saved file cannot be read."));
            } else {
                this.processResult(responseText, jobId);
            }
        } else {
            this.logWindow.appendLog("[INFO] Fetching result for job ID: " + jobId);
            URL url = new URL(BlantConfig.getResultUrl() + jobId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            this.logWindow.appendLog("[INFO] Server responded with HTTP " + status);
            if (status != 200) {
                String error = conn.getErrorStream() != null
                        ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                        : "Unknown error";
                this.logWindow.appendLog("[ERROR] Failed to fetch result: " + error);
                String stderr = BlantPoller.fetchStderr(jobId);
                if (stderr != null && !stderr.isBlank()) {
                    this.logWindow.appendLog("[STDERR]\n" + stderr);
                }
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog((Component) null, "Failed to fetch BLANT result: " + error));
            } else {
                String responseText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                this.logWindow.appendLog("[INFO] Result received. Parsing network...");
                this.processResult(responseText, jobId);
            }
        }
    }

    private void processResult(String responseText, String jobId) throws Exception {
        // Heavy data processing on background thread — EDT stays free to repaint logs
        NetworkBuilder builder = new NetworkBuilder(logWindow);
        CyNetwork network = builder.build(responseText, networkFactory);
        builder.addOriginalEdges(BlantConfig.getInputFile(), network);
        double scoreMin = builder.getScoreMin();
        double scoreMax = builder.getScoreMax();
        int added = builder.getEdgesAdded();
        logWindow.appendLog("[INFO] Score range: min=" + scoreMin + ", max=" + scoreMax);

        // Add network and create view — must run on EDT
        CyNetworkView[] viewHolder = new CyNetworkView[1];
        SwingUtilities.invokeAndWait(() -> {
            VisualUtil.showTableDialogue(responseText);
            networkManager.addNetwork(network);
            viewHolder[0] = networkViewFactory.createNetworkView(network);
            networkViewManager.addNetworkView(viewHolder[0]);
        });
        CyNetworkView view = viewHolder[0];

        // Layout runs on background thread (designed to run as a Cytoscape task)
        layoutManager.getDefaultLayout().createTaskIterator(
                view, layoutManager.getDefaultLayout().getDefaultLayoutContext(),
                org.cytoscape.view.layout.CyLayoutAlgorithm.ALL_NODE_VIEWS, null
        ).forEachRemaining(t -> {
            try { t.run(new org.cytoscape.work.TaskMonitor() {
                public void setTitle(String s) {} public void setProgress(double p) {}
                public void setStatusMessage(String s) {} public void showMessage(org.cytoscape.work.TaskMonitor.Level l, String s) {}
            }); } catch (Exception ignored) {}
        });

        // Apply styles, edge colors, and update view — on EDT
        final double finalScoreMin = scoreMin;
        final double finalScoreMax = scoreMax;
        SwingUtilities.invokeAndWait(() -> {
            VisualUtil.applyStyles(view, vmm, vmfDiscrete, vmfPassthrough, vsFactory);

            view.getEdgeViews().forEach(ev -> {
                CyEdge edgeModel = (CyEdge) ev.getModel();
                Double score = network.getRow(edgeModel).get("confidence_score", Double.class);
                String orbitPair = network.getRow(edgeModel).get("orbit_pair", String.class);
                String edgeName = network.getRow(edgeModel).get("name", String.class);

                if (score != null) {
                    Color gradientColor = VisualUtil.scoreToColor(score, finalScoreMin, finalScoreMax);
                    ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
                    ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
                }

                String tooltip = "<html><b>" + edgeName + "</b><br>Score: "
                        + (score != null ? String.format("%.6f", score) : "—")
                        + "<br>Orbit Pair: " + (orbitPair != null ? orbitPair : "—") + "</html>";
                ev.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
            });

            view.updateView();
        });

        logWindow.appendLog("[INFO] Network loaded: " + added + " edges added.");

        if (isSaved) {
            logWindow.appendLog(CacheUtil.saveOutput(jobId, responseText));
        }

        NavDashboard dashboard = NavDashboard.getExistingInstance();
        if (dashboard != null) {
            SwingUtilities.invokeLater(() -> dashboard.setScoreRange(scoreMin, scoreMax, view));
        }
    }

}
