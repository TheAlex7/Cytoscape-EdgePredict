package com.blant.edgepredict.internal.ui;

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

import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;

public class BlantLogWindow extends JFrame {
   private static BlantLogWindow instance;
   private final JTextArea logArea = new JTextArea();
   private final JButton closeBtn;
   private final JButton abortBtn;

   private BlantLogWindow() {
      super("BLANT Log");
      this.logArea.setEditable(false);
      this.logArea.setFont(new Font("Monospaced", 0, 12));
      this.logArea.setBackground(Color.BLACK);
      this.logArea.setForeground(Color.GREEN);
      this.logArea.setMargin(new Insets(5, 5, 5, 5));
      this.logArea.setLineWrap(true);
      this.logArea.setWrapStyleWord(true);

      JScrollPane scrollPane = new JScrollPane(this.logArea);
      scrollPane.setPreferredSize(new Dimension(600, 400));
      this.addWindowListener(new WindowAdapter() {
         public void windowClosed(WindowEvent windowEvent) {
            setVisible(false);
         }
      });

      this.closeBtn = new JButton("Close");
      this.closeBtn.addActionListener((e) -> {
         setVisible(false);
      });

      this.abortBtn = new JButton("Stop Task");
      this.abortBtn.setEnabled(false);
      this.abortBtn.addActionListener((e) -> {
         if (BlantPoller.getInstance().isPolling()) {
            BlantConfig.setAborted(true);
            BlantPoller.getInstance().stopPolling();
            BlantPoller.getInstance().abort();
         }
      });

      JPanel bottomPanel = new JPanel(new FlowLayout(2));
      bottomPanel.add(this.abortBtn);
      bottomPanel.add(this.closeBtn);

      this.setLayout(new BorderLayout());
      this.add(scrollPane, "Center");
      this.add(bottomPanel, "South");
      this.pack();
      this.setLocationRelativeTo((Component)null);
      this.setDefaultCloseOperation(2);
      this.setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
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

   public void setAbortBtnEnabled(boolean enabled) {
      SwingUtilities.invokeLater(() -> this.abortBtn.setEnabled(enabled));
   }
}
