package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;


import com.blant.edgepredict.internal.task.ExportGraph;
import com.blant.edgepredict.internal.task.ImportGraph;
import com.blant.edgepredict.internal.task.SendToBlant;
import com.blant.edgepredict.internal.util.VisualUtil;

public class NavDashboard extends JFrame {

    private static NavDashboard instance;

    private final TaskManager taskManager;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmfFactoryDiscrete;
    private final VisualStyleFactory vsFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final CyLayoutAlgorithmManager layoutManager;


    private NavDashboard(TaskManager taskManager,
                         CyApplicationManager applicationManager,
                         CyNetworkViewWriterManager writerManager,
                         FileUtil fileUtil,
                         VisualMappingManager vmm,
                         VisualMappingFunctionFactory vmfFactoryDiscrete,
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
        this.vmfFactoryDiscrete = vmfFactoryDiscrete;
        this.vsFactory = vsFactory;
        this.networkFactory = networkFactory;

        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Dropdown: Sample Method
        JLabel subSample = new JLabel("Sample Method");
        subSample.setPreferredSize(new Dimension(250, 25));
        String[] arrSample = new String[1];
        arrSample[0] = "MCRC";
        JComboBox<String> jcbSample = new JComboBox<String>(arrSample);
        jcbSample.setPreferredSize(new Dimension(200, 25));

        // Dropdown: Precision Digits
        JLabel subPrec = new JLabel("Precision Digits");
        subPrec.setPreferredSize(new Dimension(250, 25));
        Integer[] arrPrec = new Integer[5];
        for (int i = 0; i < arrPrec.length; i++) {
            arrPrec[i] = i + 1;
        }
        JComboBox<Integer> jcbPrec = new JComboBox<Integer>(arrPrec);
        jcbPrec.setPreferredSize(new Dimension(200, 25));

        // Checkboxes: K-values
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

        // Button: Export SIF
        JButton exportBtn = new JButton("Export Network as SIF");
        exportBtn.addActionListener(e -> {
            try {
                new ExportGraph(applicationManager, writerManager, fileUtil).export();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        });
        exportBtn.setPreferredSize(new Dimension(145, 25));

        // Button: Import from BLANT
        JButton importBtn = new JButton("Import Network from SIF");
        importBtn.addActionListener(e -> {
            try {
                new ImportGraph(
                        networkFactory,
                        networkManager,
                        networkViewFactory,
                        networkViewManager,
                        layoutManager,
                        vmm,
                        vmfFactoryDiscrete,
                        vsFactory
                ).importFile();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage());
            }
        });
        importBtn.setPreferredSize(new Dimension(145, 25));;

        // Button: Send to BLANT
        JButton sendBtn = new JButton("Send to BLANT");
        sendBtn.addActionListener(e -> {
            try {
                new SendToBlant(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager).send();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Send to BLANT failed: " + ex.getMessage());
            }
        });
        sendBtn.setPreferredSize(new Dimension(145, 25));

        // Button: Open Log Window
        JButton logBtn = new JButton("BLANT Log");
        logBtn.addActionListener(e -> {
            BlantLogWindow logWindow = BlantLogWindow.getInstance();
            logWindow.setVisible(true);
        });
        logBtn.setPreferredSize(new Dimension(145, 25));

        // Add components to dashboard for BLANT parameters
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

        add(paramPanel, BorderLayout.CENTER);

        // Buttons at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(sendBtn);
        buttonPanel.add(logBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public static NavDashboard getInstance(TaskManager taskManager,
                                           CyApplicationManager applicationManager,
                                           CyNetworkViewWriterManager writerManager,
                                           FileUtil fileUtil,
                                           VisualMappingManager vmm,
                                           VisualMappingFunctionFactory vmfFactoryDiscrete,
                                           VisualStyleFactory vsFactory,
                                           CyNetworkFactory networkFactory,
                                           CyNetworkManager networkManager,
                                           CyNetworkViewFactory networkViewFactory,
                                           CyNetworkViewManager networkViewManager,
                                           CyLayoutAlgorithmManager layoutManager) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new NavDashboard(taskManager, applicationManager, writerManager,
                    fileUtil, vmm, vmfFactoryDiscrete, vsFactory,
                    networkFactory, networkManager, networkViewFactory, networkViewManager,
                    layoutManager);
        }
        return instance;
    }
}