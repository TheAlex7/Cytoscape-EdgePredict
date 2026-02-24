package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.util.swing.FileUtil;

import com.blant.edgepredict.internal.task.LinkPredictionTask;
import com.blant.edgepredict.internal.task.ExportGraph;

public class NavDashboard extends JFrame {

    private final TaskManager taskManager;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;

    public NavDashboard(
            TaskManager taskManager,
            CyApplicationManager applicationManager,
            CyNetworkViewWriterManager writerManager,
            FileUtil fileUtil) {

        super("BLANT Navigation Controller");

        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;

        // Layout
        setLayout(new GridLayout(5, 1, 10, 10));
        ((JPanel) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Button 1: Run Logic
        JButton runBtn = new JButton("Run Prediction");
        runBtn.addActionListener(e ->
                taskManager.execute(new TaskIterator(new LinkPredictionTask()))
        );

        // Button 2: Show Network Stats
        JButton statsBtn = new JButton("Show Network Stats");
        statsBtn.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Stats logic goes here!")
        );

        // Button 3: Reset View
        JButton resetBtn = new JButton("Reset View");

        // Button 4: Export SIF
       JButton exportBtn = new JButton("Export Network as SIF");
        exportBtn.addActionListener(e -> {
            try {
                new ExportGraph(applicationManager, writerManager, fileUtil).export();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Export failed: " + ex.getMessage());
            }
        });


        add(new JLabel("Control Panel", SwingConstants.CENTER));
        add(runBtn);
        add(statsBtn);
        add(resetBtn);
        add(exportBtn);

        pack();
        setLocationRelativeTo(null);  // centers window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
