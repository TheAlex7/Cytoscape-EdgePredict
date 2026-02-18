package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskIterator;
import com.blant.edgepredict.internal.LinkPredictionTask;

public class NavDashboard extends JFrame {
    private final TaskManager taskManager;

    public NavDashboard(TaskManager taskManager) {
        super("BLANT Navigation Controller");
        this.taskManager = taskManager;

        // Set up the layout
        setLayout(new GridLayout(4, 1, 10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Button 1: Run Logic
        JButton runBtn = new JButton("🚀 Run Prediction");
        runBtn.addActionListener(e -> {
            taskManager.execute(new TaskIterator(new LinkPredictionTask()));
        });

        // Button 2: Statistics
        JButton statsBtn = new JButton("📊 Show Network Stats");
        statsBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Stats logic goes here!");
        });

        // Button 3: Reset
        JButton resetBtn = new JButton("🔄 Reset View");

        add(new JLabel("Control Panel", SwingConstants.CENTER));
        add(runBtn);
        add(statsBtn);
        add(resetBtn);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}