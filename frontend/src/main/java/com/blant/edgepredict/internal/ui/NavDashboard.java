package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;

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
        String[] arrSample = new String[5];
		arrSample[0] = "MCMC (Markov Chain Monte Carlo)";
		arrSample[1] = "NBE (Node Based Expansion)";
		arrSample[2] = "EBE (Edge Based Expansion)";
		arrSample[3] = "RES (Reservior sampling)";
		arrSample[4] = "AR (accept/reject)";
        JComboBox<String> jcbSample = new JComboBox<String>(arrSample);

        // Dropdown: Precision Digits
        JLabel subPrec = new JLabel("Precision Digits");
        Integer[] arrPrec = new Integer[5];
        for (int i = 0; i < arrPrec.length; i++) {
            arrPrec[i] = i + 1;
        }
        JComboBox<Integer> jcbPrec = new JComboBox<Integer>(arrPrec);
        JLabel subPrecWarn = new JLabel("*Warning! Increasing precision digit will cause runtime increase in quadratic manner!");

        // RadioBtn: Graph Type
        JLabel subType = new JLabel("Graph Type");
        JRadioButton radUndirected = new JRadioButton("Undirected");
        JRadioButton radDirected = new JRadioButton("Directed");
        ButtonGroup typeButtonGroup = new ButtonGroup();
        typeButtonGroup.add(radUndirected);
        typeButtonGroup.add(radDirected);
        radUndirected.setSelected(true);
        
        // RadioBtn: K-values
        JLabel subK = new JLabel("K-values");
        JRadioButton rad3 = new JRadioButton("3");
        JRadioButton rad4 = new JRadioButton("4");
        JRadioButton rad5 = new JRadioButton("5");
        JRadioButton rad6 = new JRadioButton("6");
        JRadioButton rad7 = new JRadioButton("7");
        JRadioButton rad8 = new JRadioButton("8");
        ButtonGroup kValueButtonGroup = new ButtonGroup();
        kValueButtonGroup.add(rad3);
        kValueButtonGroup.add(rad4);
        kValueButtonGroup.add(rad5);
        kValueButtonGroup.add(rad6);
        kValueButtonGroup.add(rad7);
        kValueButtonGroup.add(rad8);
        rad4.setSelected(true);
        rad3.setEnabled(false);

        JCheckBox chkSave = new JCheckBox("Save the input and the result");

        // Button: Export SIF
        JButton exportBtn = new JButton("Export Network as SIF");
        exportBtn.addActionListener(e -> {
            try {
                new ExportGraph(applicationManager, writerManager, fileUtil).export();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }); exportBtn.setPreferredSize(new Dimension(145, 25));

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
        importBtn.setPreferredSize(new Dimension(145, 25));

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

        JButton arrayBtn = new JButton("Array Data Viewer");
        arrayBtn.addActionListener(e -> {
            VisualUtil.showTableDialogue(""); // Todo: Put real data variable in here
        });
        arrayBtn.setPreferredSize(new Dimension(145, 25));

        // Add components to dashboard for BLANT parameters
        JPanel paramPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        paramPanel.add(subSample);
        paramPanel.add(jcbSample);
        paramPanel.add(subPrec);
        paramPanel.add(jcbPrec);
        paramPanel.add(subPrecWarn);
        paramPanel.add(subType);
        paramPanel.add(radUndirected);
        paramPanel.add(radDirected);
        paramPanel.add(subK);

        JPanel kPanel = new JPanel(new GridLayout(1, 6, 5 ,5));
        kPanel.add(rad3);
        kPanel.add(rad4);
        kPanel.add(rad5);
        kPanel.add(rad6);
        kPanel.add(rad7);
        kPanel.add(rad8);
        paramPanel.add(kPanel);
        paramPanel.add(chkSave);
        add(paramPanel, BorderLayout.CENTER);

        // Buttons at bottom
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(sendBtn);
        buttonPanel.add(logBtn);
        buttonPanel.add(arrayBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Action Listenener & User input variables
        int k = 4;
        
        boolean isUndirected = radUndirected.isSelected();


        ActionListener typeListener = e -> {
            rad7.setEnabled(isUndirected);
            rad8.setEnabled(isUndirected);
            rad3.setEnabled(!isUndirected);
            
            getContentPane().revalidate();
            getContentPane().repaint();
        };
        
        radUndirected.addActionListener(typeListener);
        radDirected.addActionListener(typeListener);
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