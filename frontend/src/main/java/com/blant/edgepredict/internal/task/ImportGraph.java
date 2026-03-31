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
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;
import com.blant.edgepredict.internal.util.VisualUtil;

public class ImportGraph {

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final CyLayoutAlgorithmManager layoutManager;

    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmf;
    private final VisualStyleFactory vsFactory;

    public ImportGraph(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory,
            CyNetworkViewManager networkViewManager,
            CyLayoutAlgorithmManager layoutManager,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory vmf,
            VisualStyleFactory vsFactory) {

        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;

        this.vmm = vmm;
        this.vmf = vmf;
        this.vsFactory = vsFactory;
    }

    public void importFile() throws Exception {

        BlantLogWindow logWindow = BlantLogWindow.getInstance();

        String jobId = BlantConfig.getJobId();
        if (jobId == null || jobId.isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "No BLANT job ID found. Please run 'Send to BLANT' first.");
            return;
        }

        BlantPoller poller = BlantPoller.getInstance();

        logWindow.appendLog("[INFO] Fetching result for job ID: " + jobId);

        URL url = new URL(BlantConfig.RESULTS_URL + jobId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != 200) {
            String error = conn.getErrorStream() != null
                    ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
                    : "Unknown error";

            logWindow.appendLog("[ERROR] Failed to fetch result: " + error);
            JOptionPane.showMessageDialog(null,
                    "Failed to fetch BLANT result: " + error);
            return;
        }

        String responseText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        logWindow.appendLog("[INFO] Result received. Parsing network...");
        logWindow.appendLog("[DEBUG] Raw response:\n" + responseText);

        poller.stopPolling();

        CyNetwork network = networkFactory.createNetwork();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "BLANT Result");

        if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
            network.getDefaultEdgeTable().createColumn("confidence_score", Double.class, false);
        }

        Map<String, CyNode> nodeMap = new HashMap<>();
        int added = 0;

        VisualUtil.showTableDialogue(responseText);

        for (String line : responseText.split("\n")) {

            line = line.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] row = line.split("\\s+");

            if (row.length < 4) {
                logWindow.appendLog("[WARN] Skipping malformed line: " + line);
                continue;
            }

            String sourceName = row[0];
            String interaction = row[1];
            String targetName = row[2];

            Double score = null;
            try {
                score = Double.parseDouble(row[3]);
            } catch (Exception e) {
                logWindow.appendLog("[WARN] Could not parse score: " + line);
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

            network.getRow(edge).set(CyNetwork.NAME,
                    sourceName + " (" + interaction + ") " + targetName);

            network.getRow(edge).set(CyEdge.INTERACTION, interaction);

            if (score != null) {
                network.getRow(edge).set("confidence_score", score);
            }

            added++;

            logWindow.appendLog("[DEBUG] Edge: " +
                    sourceName + " -> " +
                    targetName + " (" +
                    interaction + ") score=" + score);
        }

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

        // Apply visual styles first (colors, sizes, etc.)
        VisualUtil.applyStyles(view, vmm, vmf, vsFactory);

        // Set node labels and tooltips to the node name
        view.getNodeViews().forEach(nv -> {
            String name = network.getRow(nv.getModel()).get(CyNetwork.NAME, String.class);
            nv.setVisualProperty(BasicVisualLexicon.NODE_LABEL, name);
            nv.setVisualProperty(BasicVisualLexicon.NODE_TOOLTIP, name);
        });

        view.updateView();

        logWindow.appendLog("[INFO] Network loaded: " + added + " edges added.");

        JOptionPane.showMessageDialog(null,
                "Import complete: " + added + " edges added.");
        
    }
}
