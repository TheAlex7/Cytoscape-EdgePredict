package com.blant.edgepredict.internal.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

public class VisualUtil {

    public static void applyStyles(
            CyNetworkView view,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory discreteVmf,
            VisualMappingFunctionFactory passthroughVmf,
            VisualStyleFactory vsFactory) {

        if (view == null || vmm == null || discreteVmf == null || passthroughVmf == null || vsFactory == null) {
            return;
        }

        VisualStyle style = null;

        for (VisualStyle s : vmm.getAllVisualStyles()) {
            if (s.getTitle().equals("BLANT Style")) {
                style = s;
                break;
            }
        }

        if (style == null) {
            style = vsFactory.createVisualStyle("BLANT Style");
            vmm.addVisualStyle(style);
        }

        // --- Node label: passthrough mapping from CyNetwork.NAME ---
        PassthroughMapping<String, String> nodeLabel =
                (PassthroughMapping<String, String>) passthroughVmf.createVisualMappingFunction(
                        CyNetwork.NAME,
                        String.class,
                        BasicVisualLexicon.NODE_LABEL);
        style.addVisualMappingFunction(nodeLabel);

        // --- Node tooltip: passthrough mapping from CyNetwork.NAME ---
        PassthroughMapping<String, String> nodeTooltip =
                (PassthroughMapping<String, String>) passthroughVmf.createVisualMappingFunction(
                        CyNetwork.NAME,
                        String.class,
                        BasicVisualLexicon.NODE_TOOLTIP);
        style.addVisualMappingFunction(nodeTooltip);

        // --- Edge color: discrete mapping by interaction type ---
        String colName = "interaction";

        if (view.getModel().getDefaultEdgeTable().getColumn(colName) != null) {

            DiscreteMapping<String, Paint> edgeColor =
                    (DiscreteMapping<String, Paint>) discreteVmf.createVisualMappingFunction(
                            colName,
                            String.class,
                            BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);

            edgeColor.putMapValue("predicted", Color.GREEN);
            edgeColor.putMapValue("Predicted", Color.GREEN);
            style.addVisualMappingFunction(edgeColor);

            DiscreteMapping<String, Paint> edgePaint =
                    (DiscreteMapping<String, Paint>) discreteVmf.createVisualMappingFunction(
                            colName,
                            String.class,
                            BasicVisualLexicon.EDGE_PAINT);

            edgePaint.putMapValue("predicted", Color.GREEN);
            edgePaint.putMapValue("Predicted", Color.GREEN);
            style.addVisualMappingFunction(edgePaint);
        }

        style.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 4.0);

        vmm.setVisualStyle(style, view);
        style.apply(view);
        view.updateView();
    }

    public static void showTableDialogue(String raw_data) {
        List<String[]> edgeList = new ArrayList<>();
        for (String line : raw_data.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] row = line.split("\\s+");
            edgeList.add(row);
        }

        SwingUtilities.invokeLater(() -> {
            String[] columnNames = {"Source", "Interaction", "Target", "Confident Score"};
            String[][] data = edgeList.toArray(new String[0][]);
            JTable table = new JTable(data, columnNames);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(null, scrollPane, "Array Data Viewer", JOptionPane.PLAIN_MESSAGE);
        });
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
