package com.blant.edgepredict.internal.ui;

import com.blant.edgepredict.internal.util.BlantPoller;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class BlantLogWindow extends JFrame {
   private static BlantLogWindow instance;
   private final JTextArea logArea = new JTextArea();
   private final JButton closeBtn;

   private BlantLogWindow() {
      super("BLANT Log");
      this.logArea.setEditable(false);
      this.logArea.setFont(new Font("Monospaced", 0, 12));
      this.logArea.setBackground(Color.BLACK);
      this.logArea.setForeground(Color.GREEN);
      this.logArea.setMargin(new Insets(5, 5, 5, 5));
      JScrollPane scrollPane = new JScrollPane(this.logArea);
      scrollPane.setPreferredSize(new Dimension(600, 400));
      this.addWindowListener(new WindowAdapter() {
         public void windowClosed(WindowEvent windowEvent) {
            BlantPoller.getInstance().stopPolling();
            BlantLogWindow.instance = null;
         }
      });
      this.closeBtn = new JButton("Close");
      this.closeBtn.addActionListener((e) -> {
         BlantPoller.getInstance().stopPolling();
         this.dispose();
      });
      JPanel bottomPanel = new JPanel(new FlowLayout(2));
      bottomPanel.add(this.closeBtn);
      this.setLayout(new BorderLayout());
      this.add(scrollPane, "Center");
      this.add(bottomPanel, "South");
      this.pack();
      this.setLocationRelativeTo((Component)null);
      this.setDefaultCloseOperation(2);
   }

   public static BlantLogWindow getInstance() {
      if (instance == null) {
         instance = new BlantLogWindow();
      }

      return instance;
   }

   public void appendLog(String message) {
      SwingUtilities.invokeLater(() -> {
         this.logArea.append(message + "\n");
         this.logArea.setCaretPosition(this.logArea.getDocument().getLength());
      });
   }
}
