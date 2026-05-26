package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;

import com.blant.edgepredict.internal.task.PredictTaskManager;
import com.blant.edgepredict.internal.util.BlantConfig;

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
    private final DialogTaskManager dialogTaskManager;

    private String sampleMethod;
    private double precisionDigits = 1.0;
    private List<String> kVal = new ArrayList<>();
    private String graphType = "Undirected";
    private boolean isSaved = true;
    private List<Checkbox> kValCheckboxes = new ArrayList<>();
    private final JPanel kValPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final ConfidenceFilterPanel filterPanel;
    private JSlider precisionSlider;
    private Boolean isFile = false;

    private NavDashboard(TaskManager taskManager, CyApplicationManager applicationManager, CyNetworkViewWriterManager writerManager, FileUtil fileUtil, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, DialogTaskManager dialogTaskManager) {
        super("BLANT Navigation Controller");
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.fileUtil = fileUtil;
        this.writerManager = writerManager;
        this.vmm = vmm;
        this.vmfDiscrete = vmfDiscrete;
        this.vmfPassthrough = vmfPassthrough;
        this.vsFactory = vsFactory;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;
        this.dialogTaskManager = dialogTaskManager;

        this.filterPanel = new ConfidenceFilterPanel(applicationManager);

        this.setLayout(new BorderLayout(10, 10));
        ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(this.advancedPanel(), BorderLayout.CENTER);
        this.add(this.filterPanel, BorderLayout.NORTH);

        JButton logBtn = new JButton("Open Log");
        logBtn.addActionListener(e -> BlantLogWindow.getInstance().setVisible(true));
        logBtn.setPreferredSize(new Dimension(120, 25));

        JButton closePopupsBtn = new JButton("Close Popups");
        closePopupsBtn.addActionListener(e -> { EdgeDetailPanel.closeAll(); NodeDetailPanel.closeAll(); });
        closePopupsBtn.setPreferredSize(new Dimension(120, 25));

        JButton projectsBtn = new JButton("My Projects");
        projectsBtn.addActionListener(e -> ProjectsDashboard.getInstance(networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory));
        projectsBtn.setPreferredSize(new Dimension(120, 25));

        JButton sendBtn = new JButton("Run");
        sendBtn.addActionListener(e -> {
            this.kVal.clear();
             for (Checkbox cb : kValCheckboxes) {
                if (cb.getState()) {
                    this.kVal.add(cb.getLabel());
                }
            }
            String projectName = askForUniqueName();
            if (projectName == null) return;
            try {
                new PredictTaskManager(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory, dialogTaskManager, this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved, projectName, this.isFile).run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(NavDashboard.this, "Predict task failed: " + ex.getMessage());
            }
        });
        sendBtn.setPreferredSize(new Dimension(120, 25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(logBtn);
        buttonPanel.add(closePopupsBtn);
        buttonPanel.add(projectsBtn);
        buttonPanel.add(sendBtn);
        this.add(buttonPanel, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo((Component) null);
        this.setResizable(false);
        this.setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    private JPanel advancedPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(sampleMethodPanel());
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(precisionDigitsPanel());
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(graphTypePanel());
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(kValPanel());
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(savePanel());
        contentPanel.setVisible(false);

        JButton toggleBtn = new JButton("Advanced Settings ▶");
        toggleBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        toggleBtn.addActionListener(e -> {
            boolean nowVisible = !contentPanel.isVisible();
            contentPanel.setVisible(nowVisible);
            toggleBtn.setText(nowVisible ? "Advanced Settings ▼" : "Advanced Settings ▶");
            this.pack();
        });

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createTitledBorder(""));
        wrapper.add(toggleBtn);
        wrapper.add(contentPanel);
        return wrapper;
    }

    private JPanel sampleMethodPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel label = new JLabel("Sample Method");
        String[] methods = {"MCMC (Markov Chain Monte Carlo)", "NBE (Node Based Expansion)", "EBE (Edge Based Expansion)", "RES (Reservior sampling)", "AR (accept/reject)"};
        JComboBox<String> combo = new JComboBox<>(methods);
        combo.addActionListener(e -> this.sampleMethod = (String) combo.getSelectedItem());
        panel.add(label);
        panel.add(combo);
        return panel;
    }


    private JPanel precisionDigitsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel precisionLable = new JLabel("Precision Digits: 1.0");
        JLabel warningLabel = new JLabel("<HTML><I>*Warning! Increasing precision digit will cause runtime increase in quadratic manner!</I></HTML>");
        warningLabel.setForeground(Color.GRAY);

        precisionSlider = new JSlider(10, 30, 10);
        precisionSlider.setMajorTickSpacing(10);
        precisionSlider.setMinorTickSpacing(1);
        precisionSlider.setPaintTicks(true);
        precisionSlider.setPaintLabels(true);
        precisionSlider.setSnapToTicks(true);

        Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
        for (int i = 10; i <= 50; i += 10) {
            labelTable.put(i, new JLabel(String.valueOf(i / 10.0)));
        }
        precisionSlider.setLabelTable(labelTable);

        precisionSlider.addChangeListener(e -> {
            double actualValue = precisionSlider.getValue() / 10.0;
            this.precisionDigits = actualValue;
            precisionLable.setText(String.format("Precision Digits: %.1f", this.precisionDigits));
        });       

        panel.setBorder(BorderFactory.createTitledBorder("Precision Digits"));
        panel.add(precisionLable);
        panel.add(warningLabel);
        panel.add(precisionSlider);
        warningLabel.setPreferredSize(new Dimension(10, warningLabel.getPreferredSize().height * 2));

        return panel;
    };

    private JPanel graphTypePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel label = new JLabel("Graph Type");
        // String[] types = {"Undirected", "Directed"};
        String[] types = {"Undirected"};
        JComboBox<String> combo = new JComboBox<>(types);
        combo.addActionListener(e -> {
            this.graphType = (String) combo.getSelectedItem(); 
            this.kValPanel.removeAll(); 
            this.kValCheckboxes.clear();

            String[] kValOptions;
            if (this.graphType.equals("Undirected")) kValOptions = new String[]{"4", "5", "6", "7", "8"};
            else kValOptions = new String[]{"3", "4", "5", "6"};

            for (String kValues : kValOptions) {
                Checkbox cb = new Checkbox(kValues);
                this.kValCheckboxes.add(cb);
                this.kValPanel.add(cb);
            }

            this.kValPanel.revalidate();
            this.kValPanel.repaint();
        });
        panel.add(label);
        panel.add(combo);
        return panel;
    }

    private JPanel kValPanel() {
        JPanel outerPanel = new JPanel(new GridLayout(0, 1));

        String[] kValOptions = new String[]{"4", "5", "6", "7", "8"};
        for (String label : kValOptions) {
            Checkbox cb = new Checkbox(label);
            this.kValCheckboxes.add(cb);
            this.kValPanel.add(cb);
        }
        kValCheckboxes.get(0).setState(true);

        outerPanel.add(new JLabel("K-values"));
        outerPanel.add(this.kValPanel);
        outerPanel.add(new JLabel("*Warning! High K-values will cause runtime increase in quadratic manner!"));
        return outerPanel;
    }

    private JPanel savePanel() {
        JPanel panel = new JPanel(new GridLayout(3, 0));
        JCheckBox chkSave = new JCheckBox("Save the input and the result");
        chkSave.setSelected(true);
        chkSave.addActionListener(e -> this.isSaved = chkSave.isSelected());
        JCheckBox chkOnline = new JCheckBox("Use online BLANT service");
        chkOnline.setSelected(true);
        chkOnline.addActionListener(e -> BlantConfig.setOnline(chkOnline.isSelected()));
        chkOnline.setPreferredSize(new Dimension(120, 25));
        JCheckBox chkForce = new JCheckBox("Force Mode");
        chkForce.setSelected(false);
        chkForce.addActionListener(e -> BlantConfig.setForce(chkForce.isSelected()));
        chkForce.setPreferredSize(new Dimension(120, 25));
        panel.add(chkSave);
        panel.add(chkOnline);
        panel.add(chkForce);
        return panel;
    }

    private String askForUniqueName() {
        while (true) {
            String name = ProjectNameDialog.show(this);
            if (name == null) return null;
            if (com.blant.edgepredict.internal.util.ProjectStore.getProjects().containsKey(name)) {
                JOptionPane.showMessageDialog(this,
                    "A project named \"" + name + "\" already exists. Please choose a different name.",
                    "Name Already Used", JOptionPane.WARNING_MESSAGE);
            } else {
                return name;
            }
        }
    }

    public void setScoreRange(double min, double max, CyNetworkView view) {
        filterPanel.setScoreRange(min, max, view);
    }

    public static NavDashboard getExistingInstance() {
        return instance;
    }

    public static double getCurrentThreshold() {
        if (instance == null || !instance.isDisplayable()) return 0.0;
        return instance.filterPanel.getCurrentThreshold();
    }

    public static boolean isFilterEnabled() {
        if (instance == null || !instance.isDisplayable()) return false;
        return instance.filterPanel.isSliderEnabled();
    }

    public static NavDashboard getInstance(TaskManager taskManager, CyApplicationManager applicationManager, CyNetworkViewWriterManager writerManager, FileUtil fileUtil, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, DialogTaskManager dialogTaskManager) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new NavDashboard(taskManager, applicationManager, writerManager, fileUtil, vmm, vmfDiscrete, vmfPassthrough, vsFactory, networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager, dialogTaskManager);
        }
        instance.setVisible(true);
        return instance;
    }
}
