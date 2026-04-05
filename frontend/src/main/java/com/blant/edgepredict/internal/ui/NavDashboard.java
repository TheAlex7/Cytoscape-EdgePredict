package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import java.awt.*;

import org.cytoscape.work.TaskManager;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;

import com.blant.edgepredict.internal.task.ExportGraph;
import com.blant.edgepredict.internal.task.ImportGraph;
import com.blant.edgepredict.internal.task.SendToBlant;

public class NavDashboard extends JFrame {

    private static NavDashboard instance;

    private final TaskManager taskManager;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmfDiscrete;
    private final VisualMappingFunctionFactory vmfPassthrough;
    private final VisualStyleFactory vsFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final CyLayoutAlgorithmManager layoutManager;

    // Slider state
    private JSlider confidenceSlider;
    private JLabel sliderLabel;
    private JLabel sliderRangeLabel;
    private double scoreMin = 0.0;
    private double scoreMax = 1.0;
    private ChangeListener sliderChangeListener;

    private NavDashboard(TaskManager taskManager,
                         CyApplicationManager applicationManager,
                         CyNetworkViewWriterManager writerManager,
                         FileUtil fileUtil,
                         VisualMappingManager vmm,
                         VisualMappingFunctionFactory vmfDiscrete,
                         VisualMappingFunctionFactory vmfPassthrough,
                         VisualStyleFactory vsFactory,
                         CyNetworkFactory networkFactory,
                         CyNetworkManager networkManager,
                         CyNetworkViewFactory networkViewFactory,
                         CyNetworkViewManager networkViewManager,
                         CyLayoutAlgorithmManager layoutManager) {
        super("BLANT Navigation Controller");
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;
        this.vmm = vmm;
        this.vmfDiscrete = vmfDiscrete;
        this.vmfPassthrough = vmfPassthrough;
        this.vsFactory = vsFactory;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- BLANT Parameters Panel ---
        JLabel subSample = new JLabel("Sample Method");
        subSample.setPreferredSize(new Dimension(250, 25));
        JComboBox<String> jcbSample = new JComboBox<>(new String[]{"MCRC"});
        jcbSample.setPreferredSize(new Dimension(200, 25));

        JLabel subPrec = new JLabel("Precision Digits");
        subPrec.setPreferredSize(new Dimension(250, 25));
        Integer[] arrPrec = new Integer[5];
        for (int i = 0; i < arrPrec.length; i++) arrPrec[i] = i + 1;
        JComboBox<Integer> jcbPrec = new JComboBox<>(arrPrec);
        jcbPrec.setPreferredSize(new Dimension(200, 25));

        JLabel subK = new JLabel("K-values");
        subK.setPreferredSize(new Dimension(250, 25));
        JCheckBox cbK3 = new JCheckBox("3");
        JCheckBox cbK4 = new JCheckBox("4");
        JCheckBox cbK5 = new JCheckBox("5");
        JCheckBox cbK6 = new JCheckBox("6");
        JCheckBox cbK7 = new JCheckBox("7");

        JLabel subBlank = new JLabel("");
        subBlank.setPreferredSize(new Dimension(450, 25));

        JLabel subPrecWarn = new JLabel("<HTML>*Warning! Increasing precision digit will cause runtime increase in<br/>quadratic manner!</HTML>");
        subPrecWarn.setPreferredSize(new Dimension(450, 50));

        JPanel paramPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        paramPanel.add(subSample);
        paramPanel.add(jcbSample);
        paramPanel.add(subPrec);
        paramPanel.add(jcbPrec);
        paramPanel.add(subK);
        paramPanel.add(cbK3);
        paramPanel.add(cbK4);
        paramPanel.add(cbK5);
        paramPanel.add(cbK6);
        paramPanel.add(cbK7);
        paramPanel.add(subBlank);
        paramPanel.add(subPrecWarn);

        // --- Confidence Slider Panel ---
        sliderLabel = new JLabel("Confidence Threshold: —");
        sliderRangeLabel = new JLabel("Import a network to enable filtering");
        sliderRangeLabel.setFont(sliderRangeLabel.getFont().deriveFont(Font.ITALIC, 11f));
        sliderRangeLabel.setForeground(Color.GRAY);

        confidenceSlider = new JSlider(0, 1000, 0);
        confidenceSlider.setMajorTickSpacing(250);
        confidenceSlider.setMinorTickSpacing(50);
        confidenceSlider.setPaintTicks(true);
        confidenceSlider.setPaintLabels(false);
        confidenceSlider.setEnabled(false);

        sliderChangeListener = e -> applyThreshold();
        confidenceSlider.addChangeListener(sliderChangeListener);

        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.setBorder(BorderFactory.createTitledBorder("Edge Confidence Filter"));
        sliderPanel.add(sliderLabel);
        sliderPanel.add(Box.createVerticalStrut(4));
        sliderPanel.add(sliderRangeLabel);
        sliderPanel.add(Box.createVerticalStrut(6));
        sliderPanel.add(confidenceSlider);
        sliderPanel.add(Box.createVerticalStrut(6));
        sliderPanel.add(buildColorLegend());

        // --- Buttons ---
        JButton exportBtn = new JButton("Export Network as SIF");
        exportBtn.addActionListener(e -> {
            try {
                new ExportGraph(applicationManager, writerManager, fileUtil).export();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        });
        exportBtn.setPreferredSize(new Dimension(175, 25));

        JButton importBtn = new JButton("Import Network from SIF");
        importBtn.addActionListener(e -> {
            try {
                new ImportGraph(
                        networkFactory, networkManager,
                        networkViewFactory, networkViewManager,
                        layoutManager, vmm,
                        vmfDiscrete, vmfPassthrough,
                        vsFactory
                ).importFile();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage());
            }
        });
        importBtn.setPreferredSize(new Dimension(175, 25));

        JButton sendBtn = new JButton("Send to BLANT");
        sendBtn.addActionListener(e -> {
            try {
                new SendToBlant(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager).send();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Send to BLANT failed: " + ex.getMessage());
            }
        });
        sendBtn.setPreferredSize(new Dimension(145, 25));

        JButton logBtn = new JButton("BLANT Log");
        logBtn.addActionListener(e -> BlantLogWindow.getInstance().setVisible(true));
        logBtn.setPreferredSize(new Dimension(145, 25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(sendBtn);
        buttonPanel.add(logBtn);

        // --- Layout ---
        add(sliderPanel, BorderLayout.NORTH);
        add(paramPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Called by ImportGraph after a network loads to configure the slider
     * with the actual min/max score range from the data.
     */
    public void setScoreRange(double min, double max) {
        this.scoreMin = min;
        this.scoreMax = max;

        // Remove listener while resetting slider programmatically to avoid
        // triggering applyThreshold() mid-update
        confidenceSlider.removeChangeListener(sliderChangeListener);
        confidenceSlider.setMinimum(0);
        confidenceSlider.setMaximum(1000);
        confidenceSlider.setValue(0);
        confidenceSlider.setEnabled(true);
        confidenceSlider.addChangeListener(sliderChangeListener);

        sliderLabel.setText(String.format("Confidence Threshold: %.4f", min));
        sliderRangeLabel.setText(String.format(
                "Range: %.4f – %.4f   |   Drag to hide low-confidence edges", min, max));
    }

    /**
     * Applies the current slider threshold: hides edges below it and
     * repaints visible edges with the blue->red gradient.
     */
    private void applyThreshold() {
        double fraction = confidenceSlider.getValue() / 1000.0;
        double threshold = scoreMin + fraction * (scoreMax - scoreMin);
        sliderLabel.setText(String.format("Confidence Threshold: %.4f", threshold));

        CyNetworkView view = applicationManager.getCurrentNetworkView();
        if (view == null) return;

        CyNetwork network = view.getModel();

        view.getEdgeViews().forEach(ev -> {
            CyRow row = network.getRow(ev.getModel());
            Double score = row.get("confidence_score", Double.class);
            if (score == null) return;

            if (score >= threshold) {
                Color gradientColor = ImportGraph.scoreToColor(score, scoreMin, scoreMax);
                ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
                ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
                ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
            } else {
                ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
            }
        });

        view.updateView();
    }

    /**
     * Builds a small blue->red gradient legend strip with low/high labels.
     */
    private JPanel buildColorLegend() {
        JPanel wrapper = new JPanel(new BorderLayout(4, 0));

        JPanel gradientBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth();
                int h = getHeight();
                for (int x = 0; x < w; x++) {
                    float t = (float) x / Math.max(w - 1, 1);
                    g.setColor(new Color(t, 0f, 1f - t));
                    g.fillRect(x, 0, 1, h);
                }
            }
        };
        gradientBar.setPreferredSize(new Dimension(200, 14));
        gradientBar.setOpaque(true);

        JLabel lowLabel = new JLabel("Low");
        lowLabel.setFont(lowLabel.getFont().deriveFont(10f));
        lowLabel.setForeground(new Color(0, 0, 180));

        JLabel highLabel = new JLabel("High");
        highLabel.setFont(highLabel.getFont().deriveFont(10f));
        highLabel.setForeground(new Color(180, 0, 0));

        wrapper.add(lowLabel, BorderLayout.WEST);
        wrapper.add(gradientBar, BorderLayout.CENTER);
        wrapper.add(highLabel, BorderLayout.EAST);

        return wrapper;
    }

    /**
     * Returns the existing singleton without requiring all constructor args.
     * Returns null if the dashboard was never opened.
     */
    public static NavDashboard getExistingInstance() {
        return instance;
    }

    public static NavDashboard getInstance(TaskManager taskManager,
                                           CyApplicationManager applicationManager,
                                           CyNetworkViewWriterManager writerManager,
                                           FileUtil fileUtil,
                                           VisualMappingManager vmm,
                                           VisualMappingFunctionFactory vmfDiscrete,
                                           VisualMappingFunctionFactory vmfPassthrough,
                                           VisualStyleFactory vsFactory,
                                           CyNetworkFactory networkFactory,
                                           CyNetworkManager networkManager,
                                           CyNetworkViewFactory networkViewFactory,
                                           CyNetworkViewManager networkViewManager,
                                           CyLayoutAlgorithmManager layoutManager) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new NavDashboard(taskManager, applicationManager, writerManager,
                    fileUtil, vmm, vmfDiscrete, vmfPassthrough, vsFactory,
                    networkFactory, networkManager, networkViewFactory, networkViewManager,
                    layoutManager);
        }
        return instance;
    }
}