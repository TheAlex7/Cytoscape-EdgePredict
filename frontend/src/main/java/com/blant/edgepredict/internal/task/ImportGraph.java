package com.blant.edgepredict.internal.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.view.model.CyNetworkView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ImportGraph extends AbstractTask{

    private final CyApplicationManager appManager;
    private boolean cancelled;

    public ImportGraph(CyApplicationManager appManager) {
        this.cancelled = false;
        this.appManager = appManager;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Importing Predicted Edges...");
        
        CyNetwork currentNetwork = appManager.getCurrentNetwork();
        if (currentNetwork == null) {
            JOptionPane.showMessageDialog(null, "No active network view found.");
            return;
        }

        // Prepare columns
        CyTable edgeTable = currentNetwork.getTable(CyEdge.class, CyNetwork.DEFAULT_ATTRS);
        if (edgeTable.getColumn("confidence_score") == null) {
            edgeTable.createColumn("confidence_score", Double.class, false);
        }

        // Node Indexing
        Map<String, CyNode> nodeMap = new HashMap<>();
        for (CyNode node : currentNetwork.getNodeList()) {
            String name = currentNetwork.getRow(node).get(CyNetwork.NAME, String.class);
            if (name != null) nodeMap.put(name, node);
        }

        // Load edge data list
        JFileChooser fileChooser = new JFileChooser();
        File selectedFile = fileChooser.getSelectedFile();
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
        taskMonitor.setStatusMessage("Adding edges to the network...");
        int totalEdges = edgeDataList.size();
        int currentCount = 0;
        for (String[] row : edgeDataList) {
            if (this.cancelled) return; 

            // Parse data
            String sourceName = row[0];
            String targetName = row[1];
            double score = Double.parseDouble(row[2]);
            
            // Create edges and add into current view
            CyNode sourceNode = nodeMap.get(sourceName);
            CyNode targetNode = nodeMap.get(targetName);
            if (sourceNode != null && targetNode != null) {
                CyEdge newEdge = currentNetwork.addEdge(sourceNode, targetNode, true);
                var edgeRow = currentNetwork.getRow(newEdge);
                edgeRow.set(CyEdge.INTERACTION, "Predicted");
                edgeRow.set("confidence_score", score);
                edgeRow.set(CyNetwork.NAME, sourceName + " (Predicted) " + targetName);
            }

            // Refresh progress rate
            currentCount++;
            taskMonitor.setProgress((double) currentCount / totalEdges);
        }

        // Refresh view
        CyNetworkView currentView = appManager.getCurrentNetworkView();
        if (currentView != null) {
            taskMonitor.setStatusMessage("Updating network view...");
            currentView.updateView();
        }
    }
}