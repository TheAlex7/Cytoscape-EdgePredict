package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import com.blant.edgepredict.internal.util.VisualUtil;

public class ConfidenceFilterPanel extends JPanel {
    static final int SLIDER_SCALE = 1000;

    private final CyApplicationManager applicationManager;
    private JSlider confidenceSlider;
    private JTextField thresholdInput;
    private JLabel sliderLabel;
    private JLabel sliderRangeLabel;
    private JCheckBox showOriginalChk;
    private JCheckBox hideUnpredictedChk;
    private double scoreMin = 0.0;
    private double scoreMax = 1.0;
    private boolean showOriginalEdges = false;
    private boolean hideNodesWithoutPredicted = false;
    private ChangeListener sliderChangeListener;
    private CyNetworkView activeView;

    public ConfidenceFilterPanel(CyApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        buildUI();
    }

    private void buildUI() {
        sliderRangeLabel = new JLabel("Import a network to enable filtering");
        sliderRangeLabel.setFont(sliderRangeLabel.getFont().deriveFont(2, 11.0f));
        sliderRangeLabel.setForeground(Color.GRAY);

        confidenceSlider = new JSlider(0, SLIDER_SCALE, 0);
        confidenceSlider.setMajorTickSpacing(250);
        confidenceSlider.setMinorTickSpacing(50);
        confidenceSlider.setPaintTicks(true);
        confidenceSlider.setPaintLabels(false);
        confidenceSlider.setEnabled(false);

        thresholdInput = new JTextField(8);
        thresholdInput.setEnabled(false);
        thresholdInput.setToolTipText("Type threshold and press Enter");
        thresholdInput.addActionListener(e -> applySearchThreshold());

        sliderChangeListener = e -> {
            if (confidenceSlider.isEnabled()) {
                double fraction = (double) confidenceSlider.getValue() / SLIDER_SCALE;
                double threshold = scoreMin + fraction * (scoreMax - scoreMin);
                thresholdInput.setText(String.format("%.4f", threshold));
                thresholdInput.setForeground(Color.BLACK);
                applyThreshold();
            }
        };
        confidenceSlider.addChangeListener(sliderChangeListener);

        JPanel thresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        sliderLabel = new JLabel("Confidence Threshold:");
        thresholdRow.add(sliderLabel);
        thresholdRow.add(thresholdInput);

        showOriginalChk = new JCheckBox("Show original edges", false);
        showOriginalChk.addActionListener(e -> {
            showOriginalEdges = showOriginalChk.isSelected();
            applyThreshold();
        });

        hideUnpredictedChk = new JCheckBox("Hide nodes without predicted edges", false);
        hideUnpredictedChk.addActionListener(e -> {
            hideNodesWithoutPredicted = hideUnpredictedChk.isSelected();
            applyThreshold();
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Edge Confidence Filter"));
        add(thresholdRow);
        add(Box.createVerticalStrut(4));
        add(sliderRangeLabel);
        add(Box.createVerticalStrut(6));
        add(confidenceSlider);
        add(Box.createVerticalStrut(4));
        add(showOriginalChk);
        add(Box.createVerticalStrut(2));
        add(hideUnpredictedChk);
        add(Box.createVerticalStrut(4));
        add(buildColorLegend());
    }

    public void setScoreRange(double min, double max, CyNetworkView view) {
        if (view == null) {
            System.err.println("[ConfidenceFilterPanel] setScoreRange called with null view — ignoring");
            return;
        }
        if (min >= max) {
            System.err.println("[ConfidenceFilterPanel] setScoreRange: min >= max — expanding range by ±0.0001");
            min -= 1e-4;
            max += 1e-4;
        }

        scoreMin = min;
        scoreMax = max;
        activeView = view;
        showOriginalEdges = false;
        showOriginalChk.setSelected(false);
        hideNodesWithoutPredicted = false;
        hideUnpredictedChk.setSelected(false);

        confidenceSlider.removeChangeListener(sliderChangeListener);
        confidenceSlider.setEnabled(false);
        confidenceSlider.setMinimum(0);
        confidenceSlider.setMaximum(SLIDER_SCALE);
        confidenceSlider.setValue(0);
        confidenceSlider.setEnabled(true);
        confidenceSlider.addChangeListener(sliderChangeListener);

        thresholdInput.setEnabled(true);
        thresholdInput.setForeground(Color.BLACK);
        thresholdInput.setText(String.format("%.4f", min));
        sliderLabel.setText(String.format("Confidence Threshold: %.4f", min));
        sliderRangeLabel.setText(String.format("Range: %.4f – %.4f   |   Drag to hide low-confidence edges", min, max));

        System.out.println("[ConfidenceFilterPanel] setScoreRange: min=" + min + " max=" + max + " view=" + view.getSUID());
        applyThreshold();
    }

    private void applySearchThreshold() {
        String text = thresholdInput.getText().trim();
        try {
            double value = Double.parseDouble(text);
            value = Math.max(scoreMin, Math.min(scoreMax, value));
            double fraction = (scoreMax - scoreMin) == 0.0 ? 0.0 : (value - scoreMin) / (scoreMax - scoreMin);
            int sliderPos = (int) Math.round(fraction * SLIDER_SCALE);
            thresholdInput.setForeground(Color.BLACK);
            confidenceSlider.setValue(sliderPos);
        } catch (NumberFormatException e) {
            thresholdInput.setForeground(Color.RED);
            thresholdInput.setToolTipText("Invalid number: " + text);
        }
    }

    private void applyThreshold() {
        double fraction = (double) confidenceSlider.getValue() / SLIDER_SCALE;
        double threshold = scoreMin + fraction * (scoreMax - scoreMin);
        sliderLabel.setText(String.format("Confidence Threshold: %.4f", threshold));

        CyNetworkView view = activeView != null ? activeView : applicationManager.getCurrentNetworkView();
        if (view == null) {
            System.err.println("[ConfidenceFilterPanel] applyThreshold: no view available");
            return;
        }
        CyNetwork network = (CyNetwork) view.getModel();
        if (network == null) {
            System.err.println("[ConfidenceFilterPanel] applyThreshold: view has no model");
            return;
        }
        view.getEdgeViews().forEach(ev -> {
            CyRow row = network.getRow((CyIdentifiable) ev.getModel());
            String interaction = row.get("interaction", String.class);
            if ("original".equals(interaction)) {
                ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, showOriginalEdges);
            } else {
                Double score = row.get("confidence_score", Double.class);
                if (score != null && score >= threshold) {
                    Color gradientColor = VisualUtil.scoreToColor(score, scoreMin, scoreMax);
                    ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
                    ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
                    ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
                } else if (score != null) {
                    ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
                }
            }
        });

        view.getNodeViews().forEach(nv -> {
            CyNode node = (CyNode) nv.getModel();
            boolean hasPredictedAboveThreshold = network.getAdjacentEdgeList(node, CyEdge.Type.ANY)
                    .stream()
                    .anyMatch(e -> {
                        CyRow er = network.getRow(e);
                        if (!"predicted".equals(er.get("interaction", String.class))) return false;
                        Double score = er.get("confidence_score", Double.class);
                        return score != null && score >= threshold;
                    });
            nv.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, !hideNodesWithoutPredicted || hasPredictedAboveThreshold);
        });

        view.updateView();
    }

    private JPanel buildColorLegend() {
        JPanel wrapper = new JPanel(new BorderLayout(4, 0));
        JPanel gradientBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth(), h = getHeight();
                for (int x = 0; x < w; x++) {
                    float t = (float) x / Math.max(w - 1, 1);
                    g.setColor(new Color(t, 0.0f, 1.0f - t));
                    g.fillRect(x, 0, 1, h);
                }
            }
        };
        gradientBar.setPreferredSize(new Dimension(300, 14));
        gradientBar.setOpaque(true);

        JLabel lowLabel = new JLabel("Low");
        lowLabel.setFont(lowLabel.getFont().deriveFont(10.0f));
        lowLabel.setForeground(new Color(0, 0, 180));

        JLabel highLabel = new JLabel("High");
        highLabel.setFont(highLabel.getFont().deriveFont(10.0f));
        highLabel.setForeground(new Color(180, 0, 0));

        wrapper.add(lowLabel, BorderLayout.WEST);
        wrapper.add(gradientBar, BorderLayout.CENTER);
        wrapper.add(highLabel, BorderLayout.EAST);
        return wrapper;
    }
}
