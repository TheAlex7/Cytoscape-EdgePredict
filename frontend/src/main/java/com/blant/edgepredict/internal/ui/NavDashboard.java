package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

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
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;

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

<<<<<<< Updated upstream
=======
    // Input variables
>>>>>>> Stashed changes
    private String sampleMethod;
    private int precisionDigits;
    private int kVal;
    private boolean isSaved;
    private String graphType = "Undirected";
    private CardLayout cardLayout = new CardLayout();
    private JPanel kValCards = new JPanel(cardLayout);

<<<<<<< Updated upstream
    private JSlider confidenceSlider;
    private JTextField thresholdField;       // <-- new
=======
    // Slider state
    private JSlider confidenceSlider;
    private JTextField thresholdInput;
    private JButton searchBtn;
>>>>>>> Stashed changes
    private JLabel sliderLabel;
    private JLabel sliderRangeLabel;
    private double scoreMin = 0.0;
    private double scoreMax = 1.0;
    private ChangeListener sliderChangeListener;
    private boolean syncingControls = false; // <-- prevents feedback loops

    // FIX: activeView must never be stale — always set via setScoreRange()
    private CyNetworkView activeView;

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
        super("Edge Prediction Navigation Controller");
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

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(SampleMethodJPanel());
        mainPanel.add(PrecisionDigitspJPanel());
        mainPanel.add(GraphTypeJPanel());
        mainPanel.add(kValJPanel());
        mainPanel.add(chkSavJPanel());
        add(mainPanel, BorderLayout.CENTER);

<<<<<<< Updated upstream
        // --- Confidence Slider Panel ---
        sliderLabel = new JLabel("Confidence Threshold: —");
=======
        // --- Buttons ---
        JButton logBtn = new JButton("BLANT Log");
        logBtn.addActionListener(e -> BlantLogWindow.getInstance().setVisible(true));
        logBtn.setPreferredSize(new Dimension(145, 25));

        JButton sendBtn = new JButton("Send to BLANT");
        sendBtn.addActionListener(e -> {
            try {
                new PredictTaskManager(fileUtil, networkFactory, networkManager, networkViewFactory,
                        networkViewManager, layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory,
                        this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved, this.isMultiThreaded)
                        .run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Send to BLANT failed: " + ex.getMessage());
            }
        });
        sendBtn.setPreferredSize(new Dimension(145, 25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(logBtn);
        buttonPanel.add(sendBtn);

        add(sliderPanel(), BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private JPanel SampleMethodJPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));

        JLabel subSample = new JLabel("Sample Method");

        String[] arrSample = {
                "MCMC (Markov Chain Monte Carlo)",
                "NBE (Node Based Expansion)",
                "EBE (Edge Based Expansion)",
                "RES (Reservior sampling)",
                "AR (accept/reject)"
        };
        JComboBox<String> jcbSample = new JComboBox<>(arrSample);
        jcbSample.addActionListener(e -> this.sampleMethod = (String) jcbSample.getSelectedItem());

        panel.add(subSample);
        panel.add(jcbSample);
        return panel;
    }

    private JPanel PrecisionDigitspJPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));

        JLabel subPrec = new JLabel("Precision Digits");

        Integer[] arrPrec = {1, 2, 3, 4, 5};
        JComboBox<Integer> jcbPrec = new JComboBox<>(arrPrec);
        jcbPrec.addActionListener(e -> this.precisionDigits = (int) jcbPrec.getSelectedItem());

        JLabel subPrecWarn = new JLabel(
                "*Warning! Increasing precision digit will cause runtime increase in quadratic manner!");

        panel.add(subPrec);
        panel.add(jcbPrec);
        panel.add(subPrecWarn);
        return panel;
    }

    private JPanel GraphTypeJPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));

        JLabel subType = new JLabel("Graph Type");
        String[] arrType = {"Undirected", "Directed"};
        JComboBox<String> jcbType = new JComboBox<>(arrType);
        jcbType.addActionListener(e -> {
            this.graphType = (String) jcbType.getSelectedItem();
            cardLayout.show(this.kValCards, this.graphType);
        });

        panel.add(subType);
        panel.add(jcbType);
        return panel;
    }

    private JPanel kValJPanel() {
        JPanel outerPanel = new JPanel(new GridLayout(0, 1));
        JLabel subK = new JLabel("K-values");

        Integer[] arrKUndirected = {4, 5, 6, 7, 8};
        JComboBox<Integer> jcbKUndirected = new JComboBox<>(arrKUndirected);
        jcbKUndirected.addActionListener(e -> this.kVal = (int) jcbKUndirected.getSelectedItem());

        Integer[] arrKDirected = {3, 4, 5, 6};
        JComboBox<Integer> jcbKDirected = new JComboBox<>(arrKDirected);
        jcbKDirected.addActionListener(e -> this.kVal = (int) jcbKDirected.getSelectedItem());

        this.kValCards.add(jcbKUndirected, "Undirected");
        this.kValCards.add(jcbKDirected, "Directed");

        outerPanel.add(subK);
        outerPanel.add(this.kValCards);
        return outerPanel;
    }

    private JPanel chkSavJPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 0));
        JCheckBox chkSave = new JCheckBox("Save the input and the result");
        JCheckBox chkMulti = new JCheckBox("Use multithreading");
        chkSave.setSelected(true);
        chkSave.addActionListener(e -> this.isSaved = chkSave.isSelected());
        chkMulti.addActionListener(e -> this.isMultiThreaded = chkMulti.isSelected());

        panel.add(chkSave);
        panel.add(chkMulti);
        return panel;
    }

    private JPanel sliderPanel() {
>>>>>>> Stashed changes
        sliderRangeLabel = new JLabel("Import a network to enable filtering");
        sliderRangeLabel.setFont(sliderRangeLabel.getFont().deriveFont(Font.ITALIC, 11f));
        sliderRangeLabel.setForeground(Color.GRAY);

        confidenceSlider = new JSlider(0, 1000, 0);
        confidenceSlider.setMajorTickSpacing(250);
        confidenceSlider.setMinorTickSpacing(50);
        confidenceSlider.setPaintTicks(true);
        confidenceSlider.setPaintLabels(false);
        confidenceSlider.setEnabled(false);

<<<<<<< Updated upstream
        sliderChangeListener = e -> {
            if (syncingControls) return;
            syncingControls = true;
            double fraction = confidenceSlider.getValue() / 1000.0;
            double threshold = scoreMin + fraction * (scoreMax - scoreMin);
            thresholdField.setText(String.format("%.4f", threshold));
            thresholdField.setForeground(Color.BLACK);
            syncingControls = false;
            applyThreshold(threshold);
        };
        confidenceSlider.addChangeListener(sliderChangeListener);

        // --- Threshold text field ---
        thresholdField = new JTextField("—", 8);
        thresholdField.setEnabled(false);
        thresholdField.setMaximumSize(new Dimension(90, 24));

        // Pressing Enter in the field validates and applies the typed value
        thresholdField.addActionListener(e -> applyTypedThreshold());

        // Also apply when focus leaves the field
        thresholdField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                applyTypedThreshold();
            }
        });

        // Row with the label + text field side by side
        JPanel thresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        thresholdRow.add(sliderLabel);
        thresholdRow.add(Box.createHorizontalStrut(8));
        thresholdRow.add(thresholdField);
=======
        thresholdInput = new JTextField(8);
        thresholdInput.setEnabled(false);
        thresholdInput.setToolTipText("Type threshold and press Enter");

        searchBtn = new JButton("Go");
        searchBtn.setEnabled(false);

        searchBtn.addActionListener(e -> applySearchThreshold());
        thresholdInput.addActionListener(e -> applySearchThreshold());

        // FIX: define the listener once and keep a reference so it can be
        // safely removed before re-adding in setScoreRange() — prevents
        // duplicate listeners stacking up across multiple network imports.
        sliderChangeListener = e -> {
            if (!confidenceSlider.isEnabled()) return; // guard against spurious events during setup

            double fraction = confidenceSlider.getValue() / 1000.0;
            double threshold = scoreMin + fraction * (scoreMax - scoreMin);

            thresholdInput.setText(String.format("%.4f", threshold));
            thresholdInput.setForeground(Color.BLACK);

            applyThreshold();
        };

        confidenceSlider.addChangeListener(sliderChangeListener);

        JPanel thresholdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        sliderLabel = new JLabel("Confidence Threshold:");
        thresholdRow.add(sliderLabel);
        thresholdRow.add(thresholdInput);
>>>>>>> Stashed changes

        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.setBorder(BorderFactory.createTitledBorder("Edge Confidence Filter"));
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
        sliderPanel.add(thresholdRow);
        sliderPanel.add(Box.createVerticalStrut(4));
        sliderPanel.add(sliderRangeLabel);
        sliderPanel.add(Box.createVerticalStrut(6));
        sliderPanel.add(confidenceSlider);
        sliderPanel.add(Box.createVerticalStrut(4));
        sliderPanel.add(buildColorLegend());

<<<<<<< Updated upstream
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

        JButton sendBtn = new JButton("Send to Edge Prediction");
        sendBtn.addActionListener(e -> {
            try {
                new SendToBlant(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager,
                        this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved).send();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Send to Edge Prediction failed: " + ex.getMessage());
            }
        });
        sendBtn.setPreferredSize(new Dimension(145, 25));

        JButton logBtn = new JButton("Edge Prediction Log");
        logBtn.addActionListener(e -> BlantLogWindow.getInstance().setVisible(true));
        logBtn.setPreferredSize(new Dimension(145, 25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(logBtn);
        buttonPanel.add(sendBtn);

        add(sliderPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Parses the typed value, shows an out-of-bounds message if needed,
     * clamps to range, and applies. Called on Enter or focus-lost.
     */
    private void applyTypedThreshold() {
        if (syncingControls) return;
        String text = thresholdField.getText().trim();

        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            thresholdField.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    "\"" + text + "\" is not a valid number.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (value < scoreMin || value > scoreMax) {
            thresholdField.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    String.format("%.4f is outside the valid range (%.4f – %.4f).\nPlease enter a value within the range.", value, scoreMin, scoreMax),
                    "Out of bounds", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Valid — sync slider and apply
        thresholdField.setForeground(Color.BLACK);
        syncingControls = true;
        int sliderPos = (int) Math.round((value - scoreMin) / (scoreMax - scoreMin) * 1000);
        confidenceSlider.setValue(sliderPos);
        syncingControls = false;

        applyThreshold(value);
    }

    public void setScoreRange(double min, double max) {
        this.scoreMin = min;
        this.scoreMax = max;

=======
        return sliderPanel;
    }

    /**
     * Called by ImportGraph after a network loads to configure the slider
     * with the actual min/max score range and store the view directly.
     *
     * FIX: always removes the listener before mutating slider state, then
     * re-adds it — prevents the listener firing mid-setup with a half-valid
     * scoreMin/scoreMax and a stale activeView.
     */
    public void setScoreRange(double min, double max, CyNetworkView view) {
        if (view == null) {
            System.err.println("[NavDashboard] setScoreRange called with null view — ignoring");
            return;
        }

        // Validate range; if degenerate, expand slightly so slider math doesn't divide by zero
        if (min >= max) {
            System.err.println("[NavDashboard] setScoreRange: min (" + min + ") >= max (" + max
                    + ") — expanding range by ±0.0001");
            min = min - 0.0001;
            max = max + 0.0001;
        }

        this.scoreMin   = min;
        this.scoreMax   = max;
        this.activeView = view;

        // Detach listener before touching the slider to avoid mid-setup callbacks
>>>>>>> Stashed changes
        confidenceSlider.removeChangeListener(sliderChangeListener);
        confidenceSlider.setEnabled(false); // extra guard during reset

        confidenceSlider.setMinimum(0);
        confidenceSlider.setMaximum(1000);
        confidenceSlider.setValue(0);

        confidenceSlider.setEnabled(true);
        confidenceSlider.addChangeListener(sliderChangeListener);

<<<<<<< Updated upstream
        thresholdField.setEnabled(true);
        thresholdField.setText(String.format("%.4f", min));
        thresholdField.setForeground(Color.BLACK);

        sliderLabel.setText("Confidence Threshold:");
        sliderRangeLabel.setText(String.format(
                "Range: %.4f – %.4f   |   Drag or type to filter edges", min, max));
    }

    /**
     * Applies the given threshold value to the network view.
=======
        thresholdInput.setEnabled(true);
        thresholdInput.setForeground(Color.BLACK);
        thresholdInput.setText(String.format("%.4f", min));
        searchBtn.setEnabled(true);

        sliderLabel.setText(String.format("Confidence Threshold: %.4f", min));
        sliderRangeLabel.setText(String.format(
                "Range: %.4f – %.4f   |   Drag to hide low-confidence edges", min, max));

        System.out.println("[NavDashboard] setScoreRange: min=" + min + " max=" + max
                + " view=" + view.getSUID());

        // Apply immediately so the graph reflects the initial state
        applyThreshold();
    }

    /**
     * Parses the threshold input field, clamps to valid range,
     * and snaps the slider to the corresponding position.
     */
    private void applySearchThreshold() {
        String text = thresholdInput.getText().trim();
        try {
            double value = Double.parseDouble(text);

            value = Math.max(scoreMin, Math.min(scoreMax, value));

            double fraction = (scoreMax - scoreMin) == 0 ? 0
                    : (value - scoreMin) / (scoreMax - scoreMin);
            int sliderPos = (int) Math.round(fraction * 1000);

            thresholdInput.setForeground(Color.BLACK);

            // Setting the slider value triggers sliderChangeListener -> applyThreshold()
            confidenceSlider.setValue(sliderPos);

        } catch (NumberFormatException ex) {
            thresholdInput.setForeground(Color.RED);
            thresholdInput.setToolTipText("Invalid number: " + text);
        }
    }

    /**
     * Applies the current slider threshold: hides edges below it and
     * repaints visible edges with the blue->red gradient.
     *
     * FIX: resolves the view from activeView first, falls back to
     * applicationManager only as a last resort, and logs clearly when
     * neither is available so the silent no-op is visible in the console.
>>>>>>> Stashed changes
     */
    private void applyThreshold(double threshold) {
        sliderLabel.setText("Confidence Threshold:");

        // FIX: prefer the stored activeView; only fall back to applicationManager
        // if it is somehow null (e.g. view was disposed between calls).
        CyNetworkView view = activeView;
        if (view == null) {
            view = applicationManager.getCurrentNetworkView();
        }
        if (view == null) {
            System.err.println("[NavDashboard] applyThreshold: no view available — "
                    + "call setScoreRange() after importing a network");
            return;
        }

        CyNetwork network = view.getModel();
        if (network == null) {
            System.err.println("[NavDashboard] applyThreshold: view has no model");
            return;
        }

        // FIX: warn once if the confidence_score column is missing entirely,
        // rather than silently skipping every edge.
        if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
            System.err.println("[NavDashboard] applyThreshold: 'confidence_score' column not found "
                    + "in edge table — check that the import task writes this column");
            return;
        }

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

<<<<<<< Updated upstream
    private JPanel SampleMethodJPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel subSample = new JLabel("Sample Method");
        String[] arrSample = {"MCMC (Markov Chain Monte Carlo)", "NBE (Node Based Expansion)",
                "EBE (Edge Based Expansion)", "RES (Reservior sampling)", "AR (accept/reject)"};
        JComboBox<String> jcbSample = new JComboBox<>(arrSample);
        jcbSample.addActionListener(e -> this.sampleMethod = (String) jcbSample.getSelectedItem());
        panel.add(subSample);
        panel.add(jcbSample);
        return panel;
    }

    private JPanel PrecisionDigitspJPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        JLabel subPrec = new JLabel("Precision Digits");
        Integer[] arrPrec = {1, 2, 3, 4, 5};
        JComboBox<Integer> jcbPrec = new JComboBox<>(arrPrec);
        jcbPrec.addActionListener(e -> this.precisionDigits = (int) jcbPrec.getSelectedItem());
        JLabel subPrecWarn = new JLabel("*Warning! Increasing precision digit will cause runtime increase in quadratic manner!");
        panel.add(subPrec);
        panel.add(jcbPrec);
        panel.add(subPrecWarn);
        return panel;
    }

    private JPanel GraphTypeJPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel subType = new JLabel("Graph Type");
        String[] arrType = {"Undirected", "Directed"};
        JComboBox<String> jcbType = new JComboBox<>(arrType);
        jcbType.addActionListener(e -> {
            this.graphType = (String) jcbType.getSelectedItem();
            cardLayout.show(this.kValCards, this.graphType);
        });
        panel.add(subType);
        panel.add(jcbType);
        return panel;
    }

    private JPanel kValJPanel() {
        JPanel outerPanel = new JPanel(new GridLayout(0, 1));
        JLabel subK = new JLabel("K-values");
        Integer[] arrKUndirected = {4, 5, 6, 7, 8};
        JComboBox<Integer> jcbKUndirected = new JComboBox<>(arrKUndirected);
        jcbKUndirected.addActionListener(e -> this.kVal = (int) jcbKUndirected.getSelectedItem());
        Integer[] arrKDirected = {3, 4, 5, 6};
        JComboBox<Integer> jcbKDirected = new JComboBox<>(arrKDirected);
        jcbKDirected.addActionListener(e -> this.kVal = (int) jcbKDirected.getSelectedItem());
        this.kValCards.add(jcbKUndirected, "Undirected");
        this.kValCards.add(jcbKDirected, "Directed");
        outerPanel.add(subK);
        outerPanel.add(this.kValCards);
        return outerPanel;
    }

    private JPanel chkSavJPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 0));
        JCheckBox chkSave = new JCheckBox("Save the input and the result");
        chkSave.addActionListener(e -> this.isSaved = chkSave.isSelected());
        panel.add(chkSave);
        return panel;
    }

=======
>>>>>>> Stashed changes
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
        instance.setVisible(true);
        return instance;
    }
}