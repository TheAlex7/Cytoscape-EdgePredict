package com.blant.edgepredict.internal.ui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BlantLogWindow extends JFrame {

    private final JTextArea logArea;
    private final JButton closeBtn;
    private SwingWorker<Void, String> poller;

    public BlantLogWindow() {
        super("BLANT Log");

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> {
            stopPolling();
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

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void startPolling() {
        poller = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    try {
                        URL url = new URL("http://localhost:5000/ping");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2000);

                        if (conn.getResponseCode() == 200) {
                            BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            publish("[PING] " + sb.toString());
                        }
                    } catch (Exception e) {
                        publish("[ERROR] Could not reach server: " + e.getMessage());
                    }
                    Thread.sleep(1000);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    appendLog(chunk);
                }
            }
        };
        poller.execute();
    }

    public void stopPolling() {
        if (poller != null && !poller.isDone()) {
            poller.cancel(true);
        }
    }
}