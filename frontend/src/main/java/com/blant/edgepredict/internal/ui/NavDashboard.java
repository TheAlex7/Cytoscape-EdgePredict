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
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import com.blant.edgepredict.internal.task.LinkPredictionTask;
import com.blant.edgepredict.internal.task.ExportGraph;
import com.blant.edgepredict.internal.task.ImportGraph;
import com.blant.edgepredict.internal.task.SendToBlant;
import com.blant.edgepredict.internal.util.VisualUtil;

public class NavDashboard extends JFrame {

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

    public NavDashboard(TaskManager taskManager,
                        CyApplicationManager applicationManager,
                        CyNetworkViewWriterManager writerManager,
                        FileUtil fileUtil,
                        VisualMappingManager vmm,
                        VisualMappingFunctionFactory vmfFactoryDiscrete,
                        VisualStyleFactory vsFactory,
                        CyNetworkFactory networkFactory,
                        CyNetworkManager networkManager,
                        CyNetworkViewFactory networkViewFactory,
                        CyNetworkViewManager networkViewManager) {

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

        setLayout(new FlowLayout(FlowLayout.LEFT));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel subSample = new JLabel("Sample Method");
        subSample.setPreferredSize(new Dimension(250, 25));

        String[] arrSample = new String[1];
        arrSample[0] = "MCRC";
        JComboBox<String> jcbSample = new JComboBox<String>(arrSample);
        jcbSample.setPreferredSize(new Dimension(200, 25));

        JLabel subPrec = new JLabel("Precision Digits");
        subPrec.setPreferredSize(new Dimension(250, 25));

        Integer[] arrPrec = new Integer[5];
        for (int i = 0; i < arrPrec.length; i++) {
            arrPrec[i] = i + 1;
        }
        JComboBox<Integer> jcbPrec = new JComboBox<Integer>(arrPrec);
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

        // Button: Run Prediction
        JButton runBtn = new JButton("Run Prediction");
        runBtn.addActionListener(e -> taskManager.execute(new TaskIterator(new LinkPredictionTask())));
        runBtn.setPreferredSize(new Dimension(145, 25));

        // Button: Update Colors
        JButton colorBtn = new JButton("Update Edge Colors");
        colorBtn.addActionListener(e ->
                VisualUtil.applyStyles(applicationManager.getCurrentNetworkView(), vmm, vmfFactoryDiscrete, vsFactory)
        );
        colorBtn.setPreferredSize(new Dimension(145, 25));

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

        // Button: Import SIF
        JButton importBtn = new JButton("Import Network from SIF");
        importBtn.addActionListener(e -> {
            try {
                new ImportGraph(applicationManager, fileUtil).importFile();
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
            BlantLogWindow logWindow = new BlantLogWindow();
            logWindow.setVisible(true);
            logWindow.startPolling();
        });
        logBtn.setPreferredSize(new Dimension(145, 25));

        add(subSample);
        add(jcbSample);

        add(subPrec);
        add(jcbPrec);

        add(subK);
        add(cbK3);
        add(cbK4);
        add(cbK5);
        add(cbK6);
        add(cbK7);

        add(subBlank);
        add(subPrecWarn);

        add(runBtn);
        add(colorBtn);
        add(exportBtn);
        add(importBtn);
        add(sendBtn);
        add(logBtn);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.4);  // 40% of screen width
        int height = (int) (screenSize.height * 0.4); // 40% of screen height
        setPreferredSize(new Dimension(width, height));
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}