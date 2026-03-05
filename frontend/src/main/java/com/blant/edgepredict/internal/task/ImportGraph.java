package com.blant.edgepredict.internal.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JOptionPane;

public class ImportGraph {

    private final CyApplicationManager appManager;
    private final FileUtil fileUtil;
    private boolean cancelled;

    public ImportGraph(CyApplicationManager appManager, FileUtil fileUtil) {
        this.cancelled = false;
        this.appManager = appManager;
        this.fileUtil = fileUtil;
    }

    public void importFile() throws Exception {

        CyNetwork currentNetwork = appManager.getCurrentNetwork();
        if (currentNetwork == null) {
            JOptionPane.showMessageDialog(null, "No active network found.");
            return;
        }

        FileChooserFilter chooserFilter = new FileChooserFilter("SIF Network (*.sif)", "sif");
        File selectedFile = fileUtil.getFile(
                JOptionPane.getRootFrame(),
                "Load Edges from SIF",
                FileUtil.LOAD,
                Collections.singletonList(chooserFilter)
        );

        if (selectedFile == null) return;

        // Prepare columns
        CyTable edgeTable = currentNetwork.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
        if (edgeTable.getColumn("confidence_score") == null) {
            edgeTable.createColumn("confidence_score", Double.class, false);
        }

        // Node indexing
        Map<String, CyNode> nodeMap = new HashMap<>();
        for (CyNode node : currentNetwork.getNodeList()) {
            String name = currentNetwork.getRow(node).get(CyNetwork.NAME, String.class);
            if (name != null) nodeMap.put(name, node);
        }

        // Load edge data from file
        List<String[]> edgeDataList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
            String line;
            while ((line = br.readLine()) != null) {
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
        }

        // Add edges
        int added = 0;
        for (String[] row : edgeDataList) {
            if (cancelled) return;
            if (row.length < 3) continue;

            String sourceName = row[0];
            String targetName = row[1];
            double score = 0.0;
            try {
                score = Double.parseDouble(row[2]);
            } catch (NumberFormatException ignored) {}

            CyNode sourceNode = nodeMap.get(sourceName);
            CyNode targetNode = nodeMap.get(targetName);
            if (sourceNode != null && targetNode != null) {
                CyEdge newEdge = currentNetwork.addEdge(sourceNode, targetNode, true);
                currentNetwork.getRow(newEdge).set(CyEdge.INTERACTION, "Predicted");
                currentNetwork.getRow(newEdge).set("confidence_score", score);
                currentNetwork.getRow(newEdge).set(CyNetwork.NAME, sourceName + " (Predicted) " + targetName);
                added++;
            }
        }

        // Refresh view
        CyNetworkView currentView = appManager.getCurrentNetworkView();
        if (currentView != null) {
            currentView.updateView();
        }

        JOptionPane.showMessageDialog(null, "Import complete: " + added + " edges added.");
    }
}