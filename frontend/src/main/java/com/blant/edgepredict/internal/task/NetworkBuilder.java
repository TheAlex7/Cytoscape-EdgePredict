package com.blant.edgepredict.internal.task;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import java.util.HashMap;
import java.util.Map;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;

public class NetworkBuilder {
    private final BlantLogWindow logWindow;
    private double scoreMin = Double.MAX_VALUE;
    private double scoreMax = -Double.MAX_VALUE;
    private int edgesAdded = 0;

    public NetworkBuilder(BlantLogWindow logWindow) {
        this.logWindow = logWindow;
    }

    public CyNetwork build(String responseText, CyNetworkFactory networkFactory) {
        CyNetwork network = networkFactory.createNetwork();
        network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "BLANT Result");

        if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
            network.getDefaultEdgeTable().createColumn("confidence_score", Double.class, false);
        }
        if (network.getDefaultEdgeTable().getColumn("orbit_pair") == null) {
            network.getDefaultEdgeTable().createColumn("orbit_pair", String.class, false);
        }

        Map<String, CyNode> nodeMap = new HashMap<>();

        for (String line : responseText.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] row = line.split("\\s+");
            if (row.length < 3) {
                logWindow.appendLog("[WARN] Skipping malformed line: " + line);
                continue;
            }

            String sourceName = row[0];
            String targetName = row[1];
            Double score = null;
            try {
                score = Double.parseDouble(row[2]);
            } catch (NumberFormatException e) {
                logWindow.appendLog("[WARN] Could not parse score: " + line);
            }
            String orbitPair = row.length >= 4 ? row[3] : null;

            CyNode source = nodeMap.computeIfAbsent(sourceName, name -> {
                CyNode n = network.addNode();
                network.getRow(n).set("name", name);
                return n;
            });
            CyNode target = nodeMap.computeIfAbsent(targetName, name -> {
                CyNode n = network.addNode();
                network.getRow(n).set("name", name);
                return n;
            });

            CyEdge edge = network.addEdge(source, target, false);
            network.getRow(edge).set("name", sourceName + " (predicted) " + targetName);
            network.getRow(edge).set("interaction", "predicted");
            if (score != null) network.getRow(edge).set("confidence_score", score);
            if (orbitPair != null) network.getRow(edge).set("orbit_pair", orbitPair);

            edgesAdded++;
            logWindow.appendLog("[DEBUG] Edge: " + sourceName + " -> " + targetName + " score=" + score + " orbit_pair=" + orbitPair);
        }

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

        return network;
    }

    public double getScoreMin() { return scoreMin; }
    public double getScoreMax() { return scoreMax; }
    public int getEdgesAdded() { return edgesAdded; }
}
