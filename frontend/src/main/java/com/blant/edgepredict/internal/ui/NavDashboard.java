package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import com.blant.edgepredict.internal.task.LinkPredictionTask;
import com.blant.edgepredict.internal.task.ExportGraph;
import com.blant.edgepredict.internal.task.ImportGraph;
import com.blant.edgepredict.internal.util.VisualUtil;

// This class creates the dashboard with buttons to run prediction, update colors, and export the graph
public class NavDashboard extends JFrame {
    // Fields to hold the services we need
    private final TaskManager taskManager;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmfFactoryDiscrete;
    private final VisualStyleFactory vsFactory;

    // Constructor to initialize the dashboard and its components
    public NavDashboard(TaskManager taskManager, 
                        CyApplicationManager applicationManager, 
                        CyNetworkViewWriterManager writerManager, 
                        FileUtil fileUtil, 
                        VisualMappingManager vmm, 
                        VisualMappingFunctionFactory vmfFactoryDiscrete, 
                        VisualStyleFactory vsFactory) {

        super("BLANT Navigation Controller");
        
        // Store the services for later use in button actions
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;
        this.vmm = vmm;
        this.vmfFactoryDiscrete = vmfFactoryDiscrete;
        this.vsFactory = vsFactory;

        // Set up the layout and padding for the dashboard
        setLayout(new GridLayout(6, 1, 10, 10)); 
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Button: Run Prediction
        JButton runBtn = new JButton("Run Prediction");
        runBtn.addActionListener(e -> taskManager.execute(new TaskIterator(new LinkPredictionTask())));

        // Button: Update Colors (Uses VisualUtil)
        JButton colorBtn = new JButton("Update Edge Colors");
        colorBtn.addActionListener(e -> 
            VisualUtil.applyStyles(applicationManager.getCurrentNetworkView(), vmm, vmfFactoryDiscrete, vsFactory)
        );

        // Button: Export SIF
        JButton exportBtn = new JButton("Export Network as SIF");
        exportBtn.addActionListener(e -> {
            try {
                new ExportGraph(applicationManager, writerManager, fileUtil).export();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        });

        // Button: Import SIF ** THIS BUTTON IS ONLY FOR UNIT TEST PURPOSE**
        List<String[]> dummyData;
        dummyData = new ArrayList<>();
        String[] dummyRow = {"a", "c", "0.5"};
        dummyData.add(dummyRow);
        JButton importBtn = new JButton("Import Network from SIF");
        importBtn.addActionListener(e -> {
            try {
                taskManager.execute(new TaskIterator(new ImportGraph(applicationManager, dummyData)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage());
            }
        });

        // Add components to the dashboard
        add(new JLabel("Control Panel", SwingConstants.CENTER));
        add(runBtn);
        add(colorBtn);
        add(exportBtn);
        add(importBtn);
        // You can add more buttons here for additional functionality
        pack();// Adjust size to fit components
        setLocationRelativeTo(null);// Center on screen
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);// Close only the dashboard, not the entire app
    }

  
}