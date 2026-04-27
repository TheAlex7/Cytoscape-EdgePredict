package com.blant.edgepredict.internal.ui;

import com.blant.edgepredict.internal.task.PredictTaskManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    private String sampleMethod;
    private int precisionDigits;
    private int kVal;
    private String graphType = "Undirected";
    private boolean isSaved = true;
    private final CardLayout cardLayout = new CardLayout();
    private JPanel kValCards;
    private final ConfidenceFilterPanel filterPanel;

    private NavDashboard(TaskManager taskManager, CyApplicationManager applicationManager, CyNetworkViewWriterManager writerManager, FileUtil fileUtil, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager) {
        super("BLANT Navigation Controller");
        this.kValCards = new JPanel(this.cardLayout);
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

        this.filterPanel = new ConfidenceFilterPanel(applicationManager);

        this.setLayout(new BorderLayout(10, 10));
        ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(this.advancedPanel(), BorderLayout.CENTER);
        this.add(this.filterPanel, BorderLayout.NORTH);

        JButton logBtn = new JButton("BLANT Log");
        logBtn.addActionListener(e -> BlantLogWindow.getInstance().setVisible(true));
        logBtn.setPreferredSize(new Dimension(120, 25));

        JButton closePopupsBtn = new JButton("Close All Popups");
        closePopupsBtn.addActionListener(e -> EdgeDetailPanel.closeAll());
        closePopupsBtn.setPreferredSize(new Dimension(120, 25));

        JButton sendBtn = new JButton("Send to BLANT");
        sendBtn.addActionListener(e -> {
            try {
                new PredictTaskManager(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory, this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved).run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(NavDashboard.this, "Send to BLANT failed: " + ex.getMessage());
            }
        });
        sendBtn.setPreferredSize(new Dimension(120, 25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(logBtn);
        buttonPanel.add(closePopupsBtn);
        buttonPanel.add(sendBtn);
        this.add(buttonPanel, BorderLayout.SOUTH);

        this.pack();
        this.setLocationRelativeTo((Component) null);
        this.setResizable(false);
        this.setAlwaysOnTop(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        JPanel panel = new JPanel(new GridLayout(3, 1));
        JLabel label = new JLabel("Precision Digits");
        Integer[] values = {1, 2, 3, 4, 5};
        JComboBox<Integer> combo = new JComboBox<>(values);
        combo.addActionListener(e -> this.precisionDigits = (Integer) combo.getSelectedItem());
        JLabel warning = new JLabel("*Warning! Increasing precision digit will cause runtime increase in quadratic manner!");
        panel.add(label);
        panel.add(combo);
        panel.add(warning);
        return panel;
    }

    private JPanel graphTypePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JLabel label = new JLabel("Graph Type");
        String[] types = {"Undirected", "Directed"};
        JComboBox<String> combo = new JComboBox<>(types);
        combo.addActionListener(e -> {
            this.graphType = (String) combo.getSelectedItem();
            this.cardLayout.show(this.kValCards, this.graphType);
        });
        panel.add(label);
        panel.add(combo);
        return panel;
    }

    private JPanel kValPanel() {
        JPanel outerPanel = new JPanel(new GridLayout(0, 1));
        JLabel label = new JLabel("K-values");
        Integer[] undirectedVals = {4, 5, 6, 7, 8};
        JComboBox<Integer> undirectedCombo = new JComboBox<>(undirectedVals);
        undirectedCombo.addActionListener(e -> this.kVal = (Integer) undirectedCombo.getSelectedItem());
        Integer[] directedVals = {3, 4, 5, 6};
        JComboBox<Integer> directedCombo = new JComboBox<>(directedVals);
        directedCombo.addActionListener(e -> this.kVal = (Integer) directedCombo.getSelectedItem());
        this.kValCards.add(undirectedCombo, "Undirected");
        this.kValCards.add(directedCombo, "Directed");
        outerPanel.add(label);
        outerPanel.add(this.kValCards);
        return outerPanel;
    }

    private JPanel savePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 0));
        JCheckBox chkSave = new JCheckBox("Save the input and the result");
        chkSave.setSelected(true);
        chkSave.addActionListener(e -> this.isSaved = chkSave.isSelected());
        panel.add(chkSave);
        return panel;
    }

    public void setScoreRange(double min, double max, CyNetworkView view) {
        filterPanel.setScoreRange(min, max, view);
    }

    public static NavDashboard getExistingInstance() {
        return instance;
    }

    public static NavDashboard getInstance(TaskManager taskManager, CyApplicationManager applicationManager, CyNetworkViewWriterManager writerManager, FileUtil fileUtil, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new NavDashboard(taskManager, applicationManager, writerManager, fileUtil, vmm, vmfDiscrete, vmfPassthrough, vsFactory, networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager);
        }
        instance.setVisible(true);
        return instance;
    }
}
