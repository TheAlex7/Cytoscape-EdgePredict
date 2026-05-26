package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class NodeDetailPanel extends JDialog {

    // ── popup registry ────────────────────────────────────────────────────────
    private static final List<NodeDetailPanel> openPopups = new ArrayList<>();
    private final String nodeName;

    // ── colours (match EdgeDetailPanel) ──────────────────────────────────────
    private static final Color HEADER_BG       = new Color(45, 55, 72);
    private static final Color HEADER_FG       = Color.WHITE;
    private static final Color LABEL_FG        = new Color(100, 116, 139);
    private static final Color VALUE_FG        = new Color(15, 23, 42);
    private static final Color DIVIDER         = new Color(226, 232, 240);
    private static final Color BTN_BG          = new Color(59, 130, 246);
    private static final Color PREDICTED_COLOR = new Color(22, 163, 74);
    private static final Color ORIGINAL_COLOR  = new Color(100, 116, 139);

    // ── fan-out state ─────────────────────────────────────────────────────────
    private static volatile Long             currentFannedSUID = null;
    private static final Map<Long, double[]> savedPositions    = new HashMap<>(); // suid → [x,y]
    private static final List<View<CyNode>>  fannedViews       = new ArrayList<>();
    private static Timer                     fanTimer          = null;

    private static final double FAN_DISTANCE = 200.0;
    private static final double FAN_RADIUS   = 400.0; // nodes within this distance get pushed
    private static final int    FAN_STEPS    = 14;
    private static final int    FAN_MS       = 16;   // ~60 fps

    // ── edge info record ──────────────────────────────────────────────────────
    public static class EdgeInfo {
        final String neighbor, orbitPair;
        final Double score;
        EdgeInfo(String neighbor, Double score, String orbitPair) {
            this.neighbor = neighbor; this.score = score; this.orbitPair = orbitPair;
        }
    }

    private final CyNetwork     network;
    private final CyNetworkView netView;
    private final List<EdgeInfo> allPredicted;
    private final List<EdgeInfo> allOriginal;
    private JScrollPane scrollPane;

    // ── constructor ───────────────────────────────────────────────────────────

    public NodeDetailPanel(Frame parent, String nodeName,
                           List<EdgeInfo> predicted, List<EdgeInfo> original,
                           CyNetwork network, CyNetworkView netView) {
        super(parent, nodeName != null ? nodeName : "?", false);  // Fix 5: node name as title
        this.nodeName = nodeName;
        this.network = network;
        this.netView = netView;
        predicted.sort((a, b) -> {
            if (a.score == null) return 1;
            if (b.score == null) return -1;
            return Double.compare(b.score, a.score);
        });
        this.allPredicted = new ArrayList<>(predicted);   // Fix 2: store full sorted list
        this.allOriginal  = new ArrayList<>(original);

        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createLineBorder(DIVIDER, 1));

        // Header — Fix 5: just the node name, no redundant "Node Details" label
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel nameLabel = new JLabel(nodeName != null ? nodeName : "?");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
        nameLabel.setForeground(HEADER_FG);
        header.add(nameLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Body — Fix 2: apply current slider threshold on open
        scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        List<EdgeInfo> displayPredicted;
        if (NavDashboard.isFilterEnabled()) {
            double initThreshold = NavDashboard.getCurrentThreshold();
            displayPredicted = allPredicted.stream()
                    .filter(e -> e.score == null || e.score >= initThreshold)
                    .collect(Collectors.toList());
        } else {
            displayPredicted = new ArrayList<>(allPredicted);
        }
        setBodyContent(displayPredicted);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

        // Fix 2: button to re-filter by the current slider threshold
        JButton filterBtn = new JButton("Filter by Threshold");
        filterBtn.setFont(filterBtn.getFont().deriveFont(Font.PLAIN, 11f));
        filterBtn.setFocusPainted(false);
        filterBtn.addActionListener(e -> applyFilter());
        footer.add(filterBtn);

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
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Point mouse = MouseInfo.getPointerInfo().getLocation();
        setLocation(mouse.x + 12, mouse.y + 12);
        openPopups.add(this);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                openPopups.remove(NodeDetailPanel.this);
                // Deselect the node so clicking it again fires a new selection event
                if (network != null && nodeName != null) {
                    for (CyNode n : network.getNodeList()) {
                        if (nodeName.equals(network.getRow(n).get("name", String.class))) {
                            network.getRow(n).set("selected", false);
                            break;
                        }
                    }
                }
            }
        });
        setVisible(true);
    }

    private void setBodyContent(List<EdgeInfo> displayPredicted) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        body.add(buildSectionHeader("Predicted Edges (" + displayPredicted.size() + ")", PREDICTED_COLOR));
        body.add(Box.createVerticalStrut(2));
        if (displayPredicted.isEmpty()) {
            body.add(buildSimpleRow("None", LABEL_FG));
        } else {
            body.add(buildColumnHeaders());
            body.add(buildDivider());
            for (int i = 0; i < displayPredicted.size(); i++) {
                body.add(buildPredictedRow(displayPredicted.get(i)));
                if (i < displayPredicted.size() - 1) body.add(buildDivider());
            }
        }

        body.add(Box.createVerticalStrut(10));
        body.add(buildSectionHeader("Original Edges (" + allOriginal.size() + ")", ORIGINAL_COLOR));
        body.add(Box.createVerticalStrut(2));
        if (allOriginal.isEmpty()) {
            body.add(buildSimpleRow("None", LABEL_FG));
        } else {
            for (int i = 0; i < allOriginal.size(); i++) {
                body.add(buildOriginalRow(allOriginal.get(i).neighbor));
                if (i < allOriginal.size() - 1) body.add(buildDivider());
            }
        }

        int totalRows = displayPredicted.size() + allOriginal.size();
        scrollPane.setViewportView(body);
        scrollPane.setPreferredSize(new Dimension(420, Math.min(420, totalRows * 28 + 130)));
    }

    private void applyFilter() {
        List<EdgeInfo> filtered;
        if (NavDashboard.isFilterEnabled()) {
            double threshold = NavDashboard.getCurrentThreshold();
            filtered = allPredicted.stream()
                    .filter(e -> e.score == null || e.score >= threshold)
                    .collect(Collectors.toList());
        } else {
            filtered = new ArrayList<>(allPredicted);
        }
        setBodyContent(filtered);
        pack();
    }

    public static void closeAll() {
        List<NodeDetailPanel> snapshot = new ArrayList<>(openPopups);
        openPopups.clear();
        snapshot.forEach(NodeDetailPanel::dispose);
    }

    private static NodeDetailPanel findExisting(String name) {
        if (name == null) return null;
        for (NodeDetailPanel p : openPopups)
            if (name.equals(p.nodeName)) return p;
        return null;
    }

    // ── fan-out / restore ─────────────────────────────────────────────────────

    static void fanOut(CyNode node, CyNetworkView view) {
        stopTimer();

        // Snap any previous fan back instantly before starting a new one
        for (View<CyNode> nv : fannedViews) {
            double[] orig = savedPositions.get(nv.getModel().getSUID());
            if (orig != null) {
                nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, orig[0]);
                nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, orig[1]);
            }
        }
        savedPositions.clear();
        fannedViews.clear();

        currentFannedSUID = node.getSUID();

        View<CyNode> clickedView = view.getNodeView(node);
        if (clickedView == null) return;
        double cx = clickedView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
        double cy = clickedView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);

        Map<View<CyNode>, double[]> starts = new HashMap<>();
        Map<View<CyNode>, double[]> ends   = new HashMap<>();

        for (View<CyNode> nv : view.getNodeViews()) {
            if (nv.getModel().getSUID() == node.getSUID()) continue; // skip clicked node

            double nx = nv.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
            double ny = nv.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
            double dx = nx - cx, dy = ny - cy;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > FAN_RADIUS) continue; // too far away

            savedPositions.put(nv.getModel().getSUID(), new double[]{nx, ny});
            fannedViews.add(nv);

            double pushDist = Math.max(1.0, dist);
            starts.put(nv, new double[]{nx, ny});
            ends.put(nv, new double[]{nx + dx / pushDist * FAN_DISTANCE,
                                       ny + dy / pushDist * FAN_DISTANCE});
        }

        animate(view, starts, ends);
    }

    static void restoreFan(CyNetworkView view) {
        stopTimer();
        if (savedPositions.isEmpty()) { currentFannedSUID = null; return; }

        Map<View<CyNode>, double[]> starts = new HashMap<>();
        Map<View<CyNode>, double[]> ends   = new HashMap<>();

        for (View<CyNode> nv : fannedViews) {
            double[] orig = savedPositions.get(nv.getModel().getSUID());
            if (orig == null) continue;
            double cx = nv.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
            double cy = nv.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
            starts.put(nv, new double[]{cx, cy});
            ends.put(nv, orig.clone());
        }

        savedPositions.clear();
        fannedViews.clear();
        currentFannedSUID = null;
        animate(view, starts, ends);
    }

    private static void animate(CyNetworkView view,
                                Map<View<CyNode>, double[]> starts,
                                Map<View<CyNode>, double[]> ends) {
        if (starts.isEmpty()) return;
        int[] step = {0};
        fanTimer = new Timer(FAN_MS, null);
        fanTimer.addActionListener(e -> {
            step[0]++;
            double t = Math.min(1.0, (double) step[0] / FAN_STEPS);
            double ease = 1.0 - Math.pow(1.0 - t, 3); // ease-out cubic
            for (Map.Entry<View<CyNode>, double[]> entry : starts.entrySet()) {
                View<CyNode> nv = entry.getKey();
                double[] s = entry.getValue();
                double[] en = ends.get(nv);
                if (en == null) continue;
                nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, s[0] + (en[0] - s[0]) * ease);
                nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, s[1] + (en[1] - s[1]) * ease);
            }
            view.updateView();
            if (step[0] >= FAN_STEPS) stopTimer();
        });
        fanTimer.start();
    }

    private static void stopTimer() {
        if (fanTimer != null) { fanTimer.stop(); fanTimer = null; }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private JPanel buildSectionHeader(String text, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(color);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JPanel buildColumnHeaders() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p.add(makeLabel("Neighbor",   false, LABEL_FG, 130));
        p.add(Box.createHorizontalStrut(8));
        p.add(makeLabel("Score",      false, LABEL_FG, 100));
        p.add(Box.createHorizontalStrut(8));
        p.add(makeLabel("Orbit Pair", false, LABEL_FG, 80));
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private void navigateToNode(String nodeName) {
        if (network == null || nodeName == null) return;
        for (CyNode n : network.getNodeList())
            network.getRow(n).set("selected", false);
        CyNode target = null;
        for (CyNode n : network.getNodeList()) {
            if (nodeName.equals(network.getRow(n).get("name", String.class))) {
                network.getRow(n).set("selected", true);
                target = n;
                break;
            }
        }
        if (netView != null) {
            if (target != null) {
                View<CyNode> nv = netView.getNodeView(target);
                if (nv != null) {
                    double x = nv.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
                    double y = nv.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
                    netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_X_LOCATION, x);
                    netView.setVisualProperty(BasicVisualLexicon.NETWORK_CENTER_Y_LOCATION, y);
                }
            }
            netView.updateView();
        }
    }

    private JButton makeNodeLink(String name, int width) {
        JButton btn = new JButton(name != null ? name : "?");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 11f));
        btn.setForeground(new Color(59, 130, 246));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        Dimension d = new Dimension(width, 16);
        btn.setPreferredSize(d); btn.setMinimumSize(d); btn.setMaximumSize(d);
        btn.addActionListener(e -> navigateToNode(name));
        return btn;
    }

    private JPanel buildPredictedRow(EdgeInfo e) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        p.add(makeNodeLink(e.neighbor, 130));
        p.add(Box.createHorizontalStrut(8));
        p.add(makeLabel(e.score     != null ? String.format("%.6f", e.score) : "—", false, VALUE_FG, 100));
        p.add(Box.createHorizontalStrut(8));
        p.add(makeLabel(e.orbitPair != null ? e.orbitPair                    : "—", false, LABEL_FG, 80));
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private JPanel buildSimpleRow(String text, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(color);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JPanel buildOriginalRow(String nodeName) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        p.add(makeNodeLink(nodeName, 200), BorderLayout.WEST);
        return p;
    }

    private JLabel makeLabel(String text, boolean bold, Color color, int width) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, 11f));
        l.setForeground(color);
        Dimension d = new Dimension(width, 16);
        l.setPreferredSize(d); l.setMinimumSize(d); l.setMaximumSize(d);
        return l;
    }

    private JSeparator buildDivider() {
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ── selection listener ────────────────────────────────────────────────────

    public static class NodeSelectionListener implements RowsSetListener {
        private final CyApplicationManager appManager;

        public NodeSelectionListener(CyApplicationManager appManager) {
            this.appManager = appManager;
        }

        @Override
        public void handleEvent(RowsSetEvent event) {
            CyNetwork network = appManager.getCurrentNetwork();
            if (network == null) return;
            CyTable nodeTable = network.getDefaultNodeTable();
            if (!event.getSource().equals(nodeTable)) return;

            CyNetworkView view = appManager.getCurrentNetworkView();

            for (RowSetRecord record : event.getColumnRecords("selected")) {
                Boolean selected = (Boolean) record.getValue();
                if (selected == null) continue;

                CyRow row = record.getRow();
                Long suid = row.get(CyIdentifiable.SUID, Long.class);
                if (suid == null) continue;

                if (!selected) {
                    // Deselect: restore fan if this was the fanned node
                    if (suid.equals(currentFannedSUID) && view != null) {
                        final CyNetworkView v = view;
                        SwingUtilities.invokeLater(() -> restoreFan(v));
                    }
                    continue;
                }

                // Select: fan out neighbours + show popup
                CyNode node = network.getNodeList().stream()
                        .filter(n -> n.getSUID() == suid)
                        .findFirst().orElse(null);
                if (node == null) continue;

                if (view != null) {
                    final CyNode fn = node;
                    final CyNetworkView fv = view;
                    SwingUtilities.invokeLater(() -> fanOut(fn, fv));
                }

                String nodeName = row.get("name", String.class);
                List<EdgeInfo> predicted = new ArrayList<>();
                List<EdgeInfo> original  = new ArrayList<>();

                for (CyEdge edge : network.getAdjacentEdgeList(node, CyEdge.Type.ANY)) {
                    CyRow edgeRow = network.getRow(edge);
                    String interaction = edgeRow.get("interaction", String.class);
                    CyNode other = edge.getSource().getSUID() == node.getSUID()
                            ? edge.getTarget() : edge.getSource();
                    String otherName = network.getRow(other).get("name", String.class);
                    if ("predicted".equals(interaction)) {
                        predicted.add(new EdgeInfo(otherName,
                                edgeRow.get("confidence_score", Double.class),
                                edgeRow.get("orbit_pair", String.class)));
                    } else if ("original".equals(interaction)) {
                        original.add(new EdgeInfo(otherName, null, null));
                    }
                }

                final String name = nodeName;
                final List<EdgeInfo> fp = new ArrayList<>(predicted);
                final List<EdgeInfo> fo = new ArrayList<>(original);
                final CyNetwork fn2 = network;
                final CyNetworkView fv2 = view;
                SwingUtilities.invokeLater(() -> {
                    NodeDetailPanel existing = findExisting(name);
                    if (existing != null) {
                        existing.toFront();
                        return;
                    }
                    new NodeDetailPanel(null, name, fp, fo, fn2, fv2);
                });
            }
        }
    }
}
