package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.blant.edgepredict.internal.util.BlantPoller;


public class BlantLogWindow extends JFrame {

    private static BlantLogWindow instance;

    private final JTextArea logArea;
    private final JButton closeBtn;

    private BlantLogWindow() {
        super("BLANT Log");

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                BlantPoller.getInstance().stopPolling();
                instance = null;
            }
        });

        closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> {
            BlantPoller.getInstance().stopPolling();
            dispose();
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(closeBtn);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public static BlantLogWindow getInstance() {
        if (instance == null) {
            instance = new BlantLogWindow();
        }
        return instance;
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}