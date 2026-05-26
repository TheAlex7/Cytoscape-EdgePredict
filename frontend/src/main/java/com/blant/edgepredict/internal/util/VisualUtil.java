package com.blant.edgepredict.internal.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
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
   public static void applyStyles(CyNetworkView view, VisualMappingManager vmm, VisualMappingFunctionFactory discreteVmf, VisualMappingFunctionFactory passthroughVmf, VisualStyleFactory vsFactory) {
      if (view != null && vmm != null && discreteVmf != null && passthroughVmf != null && vsFactory != null) {
         VisualStyle style = null;

         for(VisualStyle s : vmm.getAllVisualStyles()) {
            if (s.getTitle().equals("BLANT Style")) {
               style = s;
               break;
            }
         }

         if (style == null) {
            style = vsFactory.createVisualStyle("BLANT Style");
            vmm.addVisualStyle(style);
         }

         PassthroughMapping<String, String> nodeLabel = (PassthroughMapping)passthroughVmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_LABEL);
         style.addVisualMappingFunction(nodeLabel);
         PassthroughMapping<String, String> nodeTooltip = (PassthroughMapping)passthroughVmf.createVisualMappingFunction("name", String.class, BasicVisualLexicon.NODE_TOOLTIP);
         style.addVisualMappingFunction(nodeTooltip);
         String colName = "interaction";
         if (((CyNetwork)view.getModel()).getDefaultEdgeTable().getColumn(colName) != null) {
            DiscreteMapping<String, Paint> edgeColor = (DiscreteMapping)discreteVmf.createVisualMappingFunction(colName, String.class, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
            edgeColor.putMapValue("predicted", Color.GREEN);
            edgeColor.putMapValue("Predicted", Color.GREEN);
            edgeColor.putMapValue("original", Color.BLACK);
            style.addVisualMappingFunction(edgeColor);
            DiscreteMapping<String, Paint> edgePaint = (DiscreteMapping)discreteVmf.createVisualMappingFunction(colName, String.class, BasicVisualLexicon.EDGE_PAINT);
            edgePaint.putMapValue("predicted", Color.GREEN);
            edgePaint.putMapValue("Predicted", Color.GREEN);
            edgePaint.putMapValue("original", Color.BLACK);
            style.addVisualMappingFunction(edgePaint);
         }

         style.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, (double)4.0F);
         vmm.setVisualStyle(style, view);
         style.apply(view);
         view.updateView();
      }
   }

   public static void showTableDialogue(String raw_data) {
      List<String[]> edgeList = new ArrayList();

      for(String line : raw_data.split("\n")) {
         line = line.trim();
         if (!line.isEmpty() && !line.startsWith("#")) {
            String[] row = line.split("\\s+");
            edgeList.add(row);
         }
      }

      SwingUtilities.invokeLater(() -> {
         String[] columnNames = new String[]{"Source", "Target", "Confident Score", "Orbit Pair"};
         String[][] data = (String[][])edgeList.toArray(new String[0][]);
         JTable table = new JTable(data, columnNames);
         table.setAutoCreateRowSorter(true);

         JScrollPane scrollPane = new JScrollPane(table);
         scrollPane.setPreferredSize(new Dimension(500, 400));
         
         JOptionPane optionPane = new JOptionPane(scrollPane, JOptionPane.PLAIN_MESSAGE);
         JDialog dialog = optionPane.createDialog(null, "Array Data Viewer");
         dialog.setModal(false); 
         dialog.setVisible(true);
      });
   }

   public static Color scoreToColor(double score, double min, double max) {
      float t = max == min ? 1.0F : (float)((score - min) / (max - min));
      t = Math.max(0.0F, Math.min(1.0F, t));
      return new Color(t, 0.0F, 1.0F - t);
   }
}
