package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskIterator;
import com.blant.edgepredict.internal.LinkPredictionTask;

import com.blant.edgepredict.internal.task.ExportGraph;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;

public class NavDashboard extends JFrame {
    private final TaskManager taskManager;
    private final CyApplicationManager applicationManager;
    private final CyNetworkViewWriterManager writerManager;

    public NavDashboard(
        TaskManager taskManager, 
        CyApplicationManager applicationManager,
        CyNetworkViewWriterManager writerManager) {
        super("BLANT Navigation Controller");
        this.taskManager = taskManager;
        this.applicationManager = applicationManager;
        this.writerManager = writerManager;

        // Set up the layout
        setLayout(new GridLayout(4, 1, 10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Button 1: Run Logic
        JButton runBtn = new JButton("Run Prediction");
        runBtn.addActionListener(e -> {
            taskManager.execute(new TaskIterator(new LinkPredictionTask()));
        });

        // Button 2: Statistics
        JButton statsBtn = new JButton("Show Network Stats");
        statsBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Stats logic goes here!");
        });

        // Button 3: Reset
        JButton resetBtn = new JButton("Reset View");

        // Test button: converting open view into sif text
        JButton test_export = new JButton("Export into sif format text");
        test_export.addActionListener(e -> {
            taskManager.execute(new TaskIterator(new ExportGraph(applicationManager, writerManager)));
        });

        add(new JLabel("Control Panel", SwingConstants.CENTER));
        add(runBtn);
        add(statsBtn);
        add(resetBtn);
        add(test_export);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}