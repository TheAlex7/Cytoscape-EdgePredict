package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
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

    // What are these variables for? Maybe we don't need these anymore
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

    private String sampleMethod;
    private int precisionDigits;
    private int kVal;
    private boolean isSaved;
    private String graphType = "Undirected";

    private CardLayout cardLayout = new CardLayout();
    private JPanel kValCards = new JPanel(cardLayout);

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

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        mainPanel.add(SampleMethodJPanel());
        mainPanel.add(PrecisionDigitspJPanel());
        mainPanel.add(GraphTypeJPanel());
        mainPanel.add(kValJPanel());
        mainPanel.add(chkSavJPanel());        
        add(mainPanel, BorderLayout.CENTER);

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

        // Button: Send to BLANT
        JButton sendBtn = new JButton("Send to BLANT");
        sendBtn.addActionListener(e -> {
            try {
                new SendToBlant(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager, 
                    this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved).send();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Send to BLANT failed: " + ex.getMessage());
            }
        });

        // Will we need this much of buttons?
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(logBtn);
        buttonPanel.add(arrayBtn);
        buttonPanel.add(sendBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private JPanel SampleMethodJPanel(){
        JPanel panel = new JPanel(new GridLayout(2, 1));

        JLabel subSample = new JLabel("Sample Method");

        String[] arrSample = {"MCMC (Markov Chain Monte Carlo)",
        "NBE (Node Based Expansion)",
        "EBE (Edge Based Expansion)",
        "RES (Reservior sampling)",
        "AR (accept/reject)"};
        JComboBox<String> jcbSample = new JComboBox<>(arrSample);
        jcbSample.addActionListener(e -> { this.sampleMethod = (String) jcbSample.getSelectedItem(); });

        panel.add(subSample);
        panel.add(jcbSample);
        return panel;
    }

    private JPanel PrecisionDigitspJPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));

        JLabel subPrec = new JLabel("Precision Digits");

        Integer[] arrPrec = {1, 2, 3, 4, 5};
        JComboBox<Integer> jcbPrec = new JComboBox<>(arrPrec);
        jcbPrec.addActionListener(e -> { this.precisionDigits = (int) jcbPrec.getSelectedItem(); });

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
        chkSave.addActionListener(e -> { this.isSaved = chkSave.isSelected(); });

        panel.add(chkSave);
        return panel;
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
        instance.setVisible(true);
        return instance;
    }
}