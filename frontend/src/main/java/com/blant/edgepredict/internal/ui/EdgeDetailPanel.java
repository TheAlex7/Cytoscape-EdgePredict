package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

public class EdgeDetailPanel extends JDialog {

    private static final Color HEADER_BG = new Color(45, 55, 72);
    private static final Color HEADER_FG = Color.WHITE;
    private static final Color LABEL_FG  = new Color(100, 116, 139);
    private static final Color VALUE_FG  = new Color(15, 23, 42);
    private static final Color DIVIDER   = new Color(226, 232, 240);
    private static final Color BTN_BG    = new Color(59, 130, 246);

    public EdgeDetailPanel(
            Frame parent,
            String source,
            String target,
            String interaction,
            Double score,
            String orbitPair) {

        super(parent, "Edge Details", false);
        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createLineBorder(DIVIDER, 1));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Edge Details");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        title.setForeground(HEADER_FG);
        header.add(title, BorderLayout.WEST);

        // Connection summary in header
        String connText = (source != null ? source : "?") + "  →  " + (target != null ? target : "?");
        JLabel connLabel = new JLabel(connText);
        connLabel.setFont(connLabel.getFont().deriveFont(Font.PLAIN, 11f));
        connLabel.setForeground(new Color(148, 163, 184));
        header.add(connLabel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Body ────────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        body.add(buildRow("Interaction",      interaction != null ? interaction : "—"));
        body.add(buildDivider());
        body.add(buildRow("Confidence Score", score != null ? String.format("%.6f", score) : "—"));
        body.add(buildDivider());
        body.add(buildRow("Orbit Pair",       orbitPair != null ? orbitPair : "—"));

        add(body, BorderLayout.CENTER);

        // ── Footer ──────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

        JButton close = new JButton("Close");
        close.setFont(close.getFont().deriveFont(Font.PLAIN, 11f));
        close.setBackground(BTN_BG);
        close.setForeground(Color.WHITE);
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setOpaque(true);
        close.setPreferredSize(new Dimension(64, 24));
        close.addActionListener(e -> dispose());
        footer.add(close);

        add(footer, BorderLayout.SOUTH);

        pack();
        setResizable(false);

        Point mouse = MouseInfo.getPointerInfo().getLocation();
        setLocation(mouse.x + 12, mouse.y + 12);
        setVisible(true);
    }

    /** One label + value row. */
    private JPanel buildRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(LABEL_FG);
        lbl.setPreferredSize(new Dimension(110, 16));

        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.BOLD, 11f));
        val.setForeground(VALUE_FG);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    /** Thin horizontal rule between rows. */
    private JSeparator buildDivider() {
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ────────────────────────────────────────────────────────────────────────

    public static class EdgeSelectionListener implements RowsSetListener {

        private final CyApplicationManager appManager;

        public EdgeSelectionListener(CyApplicationManager appManager) {
            this.appManager = appManager;
        }

        @Override
        public void handleEvent(RowsSetEvent event) {

            CyNetwork network = appManager.getCurrentNetwork();
            if (network == null) return;

            CyTable edgeTable = network.getDefaultEdgeTable();
            if (!event.getSource().equals(edgeTable)) return;

            for (RowSetRecord record : event.getColumnRecords(CyNetwork.SELECTED)) {

                Boolean selected = (Boolean) record.getValue();
                if (selected == null || !selected) continue;

                CyRow row = record.getRow();

                String edgeName    = row.get(CyNetwork.NAME, String.class);
                Double score       = row.get("confidence_score", Double.class);
                String orbitPair   = row.get("orbit_pair", String.class);
                String interaction = row.get(CyEdge.INTERACTION, String.class);

                String source = null;
                String target = null;

                if (edgeName != null && edgeName.contains("(") && edgeName.contains(")")) {
                    source = edgeName.substring(0, edgeName.indexOf("(")).trim();
                    target = edgeName.substring(edgeName.indexOf(")") + 1).trim();
                }

                final String fs  = source;
                final String ft  = target;
                final String fi  = interaction;
                final Double fsc = score;
                final String fop = orbitPair;

                SwingUtilities.invokeLater(() ->
                    new EdgeDetailPanel(null, fs, ft, fi, fsc, fop)
                );
            }
        }
    }
}