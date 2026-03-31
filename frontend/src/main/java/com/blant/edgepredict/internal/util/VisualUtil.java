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

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

public class VisualUtil {

    public static void applyStyles(
            CyNetworkView view,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory vmf,
            VisualStyleFactory vsFactory) {

        if (view == null || vmm == null || vmf == null || vsFactory == null) {
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

        String colName = "interaction";

        if (view.getModel().getDefaultEdgeTable().getColumn(colName) == null) {
            return;
        }

        DiscreteMapping<String, Paint> edgeColor =
                (DiscreteMapping<String, Paint>) vmf.createVisualMappingFunction(
                        colName,
                        String.class,
                        BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);

        edgeColor.putMapValue("predicted", Color.GREEN);
        edgeColor.putMapValue("Predicted", Color.GREEN);

        style.addVisualMappingFunction(edgeColor);

        DiscreteMapping<String, Paint> edgePaint =
                (DiscreteMapping<String, Paint>) vmf.createVisualMappingFunction(
                        colName,
                        String.class,
                        BasicVisualLexicon.EDGE_PAINT);

        edgePaint.putMapValue("predicted", Color.GREEN);
        edgePaint.putMapValue("Predicted", Color.GREEN);

        style.addVisualMappingFunction(edgePaint);

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
}
