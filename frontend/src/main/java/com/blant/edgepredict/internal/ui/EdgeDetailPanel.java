package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

public class EdgeDetailPanel extends JDialog {
   private static final List<EdgeDetailPanel> openPopups = new ArrayList<>();
   private static final Color HEADER_BG = new Color(45, 55, 72);
   private static final Color HEADER_FG;
   private static final Color LABEL_FG;
   private static final Color VALUE_FG;
   private static final Color DIVIDER;
   private static final Color BTN_BG;

   public EdgeDetailPanel(Frame parent, String source, String target, String interaction, Double score, String orbitPair) {
      super(parent, "Edge Details", false);
      this.setLayout(new BorderLayout());
      this.getRootPane().setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
      JPanel header = new JPanel(new BorderLayout());
      header.setBackground(HEADER_BG);
      header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
      JLabel title = new JLabel("Edge Details");
      title.setFont(title.getFont().deriveFont(1, 12.0F));
      title.setForeground(HEADER_FG);
      header.add(title, "West");
      String connText = (source != null ? source : "?") + "  →  " + (target != null ? target : "?");
      JLabel connLabel = new JLabel(connText);
      connLabel.setFont(connLabel.getFont().deriveFont(0, 11.0F));
      connLabel.setForeground(new Color(148, 163, 184));
      header.add(connLabel, "East");
      this.add(header, "North");
      JPanel body = new JPanel();
      body.setLayout(new BoxLayout(body, 1));
      body.setBackground(Color.WHITE);
      body.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
      body.add(this.buildRow("Interaction", interaction != null ? interaction : "—"));
      body.add(this.buildDivider());
      body.add(this.buildRow("Confidence Score", score != null ? String.format("%.6f", score) : "—"));
      body.add(this.buildDivider());
      body.add(this.buildRow("Orbit Pair", orbitPair != null ? orbitPair : "—"));
      this.add(body, "Center");
      JPanel footer = new JPanel(new FlowLayout(2, 10, 6));
      footer.setBackground(new Color(248, 250, 252));
      footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));
      JButton close = new JButton("Close");
      close.setFont(close.getFont().deriveFont(0, 11.0F));
      close.setBackground(BTN_BG);
      close.setForeground(Color.WHITE);
      close.setFocusPainted(false);
      close.setBorderPainted(false);
      close.setOpaque(true);
      close.setPreferredSize(new Dimension(64, 24));
      close.addActionListener((e) -> this.dispose());
      footer.add(close);
      this.add(footer, "South");
      this.pack();
      this.setResizable(false);
      this.setAlwaysOnTop(true);
      Point mouse = MouseInfo.getPointerInfo().getLocation();
      this.setLocation(mouse.x + 12, mouse.y + 12);
      openPopups.add(this);
      this.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosed(WindowEvent e) {
            openPopups.remove(EdgeDetailPanel.this);
         }
      });
      this.setVisible(true);
   }

   public static void closeAll() {
      List<EdgeDetailPanel> snapshot = new ArrayList<>(openPopups);
      openPopups.clear();
      snapshot.forEach(EdgeDetailPanel::dispose);
   }

   private JPanel buildRow(String label, String value) {
      JPanel row = new JPanel(new BorderLayout(12, 0));
      row.setBackground(Color.WHITE);
      row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
      JLabel lbl = new JLabel(label);
      lbl.setFont(lbl.getFont().deriveFont(0, 11.0F));
      lbl.setForeground(LABEL_FG);
      lbl.setPreferredSize(new Dimension(110, 16));
      JLabel val = new JLabel(value);
      val.setFont(val.getFont().deriveFont(1, 11.0F));
      val.setForeground(VALUE_FG);
      row.add(lbl, "West");
      row.add(val, "Center");
      return row;
   }

   private JSeparator buildDivider() {
      JSeparator sep = new JSeparator(0);
      sep.setForeground(DIVIDER);
      sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
      return sep;
   }

   static {
      HEADER_FG = Color.WHITE;
      LABEL_FG = new Color(100, 116, 139);
      VALUE_FG = new Color(15, 23, 42);
      DIVIDER = new Color(226, 232, 240);
      BTN_BG = new Color(59, 130, 246);
   }

   public static class EdgeSelectionListener implements RowsSetListener {
      private final CyApplicationManager appManager;

      public EdgeSelectionListener(CyApplicationManager appManager) {
         this.appManager = appManager;
      }

      public void handleEvent(RowsSetEvent event) {
         CyNetwork network = this.appManager.getCurrentNetwork();
         if (network != null) {
            CyTable edgeTable = network.getDefaultEdgeTable();
            if (((CyTable)event.getSource()).equals(edgeTable)) {
               for(RowSetRecord record : event.getColumnRecords("selected")) {
                  Boolean selected = (Boolean)record.getValue();
                  if (selected != null && selected) {
                     CyRow row = record.getRow();
                     String edgeName = (String)row.get("name", String.class);
                     Double score = (Double)row.get("confidence_score", Double.class);
                     String orbitPair = (String)row.get("orbit_pair", String.class);
                     String interaction = (String)row.get("interaction", String.class);
                     String parsedSource = null;
                     String parsedTarget = null;
                     if (edgeName != null && edgeName.contains("(") && edgeName.contains(")")) {
                        parsedSource = edgeName.substring(0, edgeName.indexOf("(")).trim();
                        parsedTarget = edgeName.substring(edgeName.indexOf(")") + 1).trim();
                     }

                     // Fix: capture effectively-final copies for use inside the lambda
                     final String finalSource = parsedSource;
                     final String finalTarget = parsedTarget;

                     SwingUtilities.invokeLater(() -> new EdgeDetailPanel((Frame)null, finalSource, finalTarget, interaction, score, orbitPair));
                  }
               }

            }
         }
      }
   }
}