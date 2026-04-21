package com.blant.edgepredict.internal.ui;

import com.blant.edgepredict.internal.task.PredictTaskManager;
import com.blant.edgepredict.internal.util.VisualUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;

public class NavDashboard extends JFrame {
   private static NavDashboard instance;
   private final TaskManager taskManager;
   private final CyApplicationManager applicationManager;
   private final CyNetworkViewWriterManager writerManager;
   private final FileUtil fileUtil;
   private final VisualMappingManager vmm;
   private final VisualMappingFunctionFactory vmfDiscrete;
   private final VisualMappingFunctionFactory vmfPassthrough;
   private final VisualStyleFactory vsFactory;
   private final CyNetworkFactory networkFactory;
   private final CyNetworkManager networkManager;
   private final CyNetworkViewFactory networkViewFactory;
   private final CyNetworkViewManager networkViewManager;
   private final CyLayoutAlgorithmManager layoutManager;
   private String sampleMethod;
   private int precisionDigits;
   private int kVal;
   private String graphType = "Undirected";
   private boolean isSaved = true;
   private boolean isMultiThreaded;
   private CardLayout cardLayout = new CardLayout();
   private JPanel kValCards;
   private JSlider confidenceSlider;
   private JTextField thresholdInput;
   private JButton searchBtn;
   private JLabel sliderLabel;
   private JLabel sliderRangeLabel;
   private double scoreMin;
   private double scoreMax;
   private ChangeListener sliderChangeListener;
   private CyNetworkView activeView;

   private NavDashboard(TaskManager taskManager, CyApplicationManager applicationManager, CyNetworkViewWriterManager writerManager, FileUtil fileUtil, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager) {
      super("BLANT Navigation Controller");
      this.kValCards = new JPanel(this.cardLayout);
      this.scoreMin = (double)0.0F;
      this.scoreMax = (double)1.0F;
      this.taskManager = taskManager;
      this.applicationManager = applicationManager;
      this.fileUtil = fileUtil;
      this.writerManager = writerManager;
      this.vmm = vmm;
      this.vmfDiscrete = vmfDiscrete;
      this.vmfPassthrough = vmfPassthrough;
      this.vsFactory = vsFactory;
      this.networkFactory = networkFactory;
      this.networkManager = networkManager;
      this.networkViewFactory = networkViewFactory;
      this.networkViewManager = networkViewManager;
      this.layoutManager = layoutManager;
      this.setLayout(new BorderLayout(10, 10));
      ((JPanel)this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, 1));
      mainPanel.add(this.SampleMethodJPanel());
      mainPanel.add(this.PrecisionDigitspJPanel());
      mainPanel.add(this.GraphTypeJPanel());
      mainPanel.add(this.kValJPanel());
      mainPanel.add(this.chkSavJPanel());
      this.add(mainPanel, "Center");
      JButton logBtn = new JButton("BLANT Log");
      logBtn.addActionListener((e) -> BlantLogWindow.getInstance().setVisible(true));
      logBtn.setPreferredSize(new Dimension(145, 25));
      JButton sendBtn = new JButton("Send to BLANT");
      sendBtn.addActionListener((e) -> {
         try {
            (new PredictTaskManager(fileUtil, networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory, this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved, this.isMultiThreaded)).run();
         } catch (Exception ex) {
            JOptionPane.showMessageDialog((Component)null, "Send to BLANT failed: " + ex.getMessage());
         }

      });
      sendBtn.setPreferredSize(new Dimension(145, 25));
      JPanel buttonPanel = new JPanel(new FlowLayout(2));
      buttonPanel.add(logBtn);
      buttonPanel.add(sendBtn);
      this.add(this.sliderPanel(), "North");
      this.add(buttonPanel, "South");
      this.pack();
      this.setLocationRelativeTo((Component)null);
      this.setResizable(false);
      this.setDefaultCloseOperation(2);
   }

   private JPanel SampleMethodJPanel() {
      JPanel panel = new JPanel(new GridLayout(2, 1));
      JLabel subSample = new JLabel("Sample Method");
      String[] arrSample = new String[]{"MCMC (Markov Chain Monte Carlo)", "NBE (Node Based Expansion)", "EBE (Edge Based Expansion)", "RES (Reservior sampling)", "AR (accept/reject)"};
      JComboBox<String> jcbSample = new JComboBox(arrSample);
      jcbSample.addActionListener((e) -> this.sampleMethod = (String)jcbSample.getSelectedItem());
      panel.add(subSample);
      panel.add(jcbSample);
      return panel;
   }

   private JPanel PrecisionDigitspJPanel() {
      JPanel panel = new JPanel(new GridLayout(3, 1));
      JLabel subPrec = new JLabel("Precision Digits");
      Integer[] arrPrec = new Integer[]{1, 2, 3, 4, 5};
      JComboBox<Integer> jcbPrec = new JComboBox(arrPrec);
      jcbPrec.addActionListener((e) -> this.precisionDigits = (Integer)jcbPrec.getSelectedItem());
      JLabel subPrecWarn = new JLabel("*Warning! Increasing precision digit will cause runtime increase in quadratic manner!");
      panel.add(subPrec);
      panel.add(jcbPrec);
      panel.add(subPrecWarn);
      return panel;
   }

   private JPanel GraphTypeJPanel() {
      JPanel panel = new JPanel(new GridLayout(2, 1));
      JLabel subType = new JLabel("Graph Type");
      String[] arrType = new String[]{"Undirected", "Directed"};
      JComboBox<String> jcbType = new JComboBox(arrType);
      jcbType.addActionListener((e) -> {
         this.graphType = (String)jcbType.getSelectedItem();
         this.cardLayout.show(this.kValCards, this.graphType);
      });
      panel.add(subType);
      panel.add(jcbType);
      return panel;
   }

   private JPanel kValJPanel() {
      JPanel outerPanel = new JPanel(new GridLayout(0, 1));
      JLabel subK = new JLabel("K-values");
      Integer[] arrKUndirected = new Integer[]{4, 5, 6, 7, 8};
      JComboBox<Integer> jcbKUndirected = new JComboBox(arrKUndirected);
      jcbKUndirected.addActionListener((e) -> this.kVal = (Integer)jcbKUndirected.getSelectedItem());
      Integer[] arrKDirected = new Integer[]{3, 4, 5, 6};
      JComboBox<Integer> jcbKDirected = new JComboBox(arrKDirected);
      jcbKDirected.addActionListener((e) -> this.kVal = (Integer)jcbKDirected.getSelectedItem());
      this.kValCards.add(jcbKUndirected, "Undirected");
      this.kValCards.add(jcbKDirected, "Directed");
      outerPanel.add(subK);
      outerPanel.add(this.kValCards);
      return outerPanel;
   }

   private JPanel chkSavJPanel() {
      JPanel panel = new JPanel(new GridLayout(2, 0));
      JCheckBox chkSave = new JCheckBox("Save the input and the result");
      JCheckBox chkMulti = new JCheckBox("Use multithreading");
      chkSave.setSelected(true);
      chkSave.addActionListener((e) -> this.isSaved = chkSave.isSelected());
      chkMulti.addActionListener((e) -> this.isMultiThreaded = chkMulti.isSelected());
      panel.add(chkSave);
      panel.add(chkMulti);
      return panel;
   }

   private JPanel sliderPanel() {
      this.sliderRangeLabel = new JLabel("Import a network to enable filtering");
      this.sliderRangeLabel.setFont(this.sliderRangeLabel.getFont().deriveFont(2, 11.0F));
      this.sliderRangeLabel.setForeground(Color.GRAY);
      this.confidenceSlider = new JSlider(0, 1000, 0);
      this.confidenceSlider.setMajorTickSpacing(250);
      this.confidenceSlider.setMinorTickSpacing(50);
      this.confidenceSlider.setPaintTicks(true);
      this.confidenceSlider.setPaintLabels(false);
      this.confidenceSlider.setEnabled(false);
      this.thresholdInput = new JTextField(8);
      this.thresholdInput.setEnabled(false);
      this.thresholdInput.setToolTipText("Type threshold and press Enter");
      this.searchBtn = new JButton("Go");
      this.searchBtn.setEnabled(false);
      this.searchBtn.addActionListener((e) -> this.applySearchThreshold());
      this.thresholdInput.addActionListener((e) -> this.applySearchThreshold());
      this.sliderChangeListener = (e) -> {
         if (this.confidenceSlider.isEnabled()) {
            double fraction = (double)this.confidenceSlider.getValue() / (double)1000.0F;
            double threshold = this.scoreMin + fraction * (this.scoreMax - this.scoreMin);
            this.thresholdInput.setText(String.format("%.4f", threshold));
            this.thresholdInput.setForeground(Color.BLACK);
            this.applyThreshold();
         }
      };
      this.confidenceSlider.addChangeListener(this.sliderChangeListener);
      JPanel thresholdRow = new JPanel(new FlowLayout(0, 4, 0));
      this.sliderLabel = new JLabel("Confidence Threshold:");
      thresholdRow.add(this.sliderLabel);
      thresholdRow.add(this.thresholdInput);
      JPanel sliderPanel = new JPanel();
      sliderPanel.setLayout(new BoxLayout(sliderPanel, 1));
      sliderPanel.setBorder(BorderFactory.createTitledBorder("Edge Confidence Filter"));
      sliderPanel.add(thresholdRow);
      sliderPanel.add(Box.createVerticalStrut(4));
      sliderPanel.add(this.sliderRangeLabel);
      sliderPanel.add(Box.createVerticalStrut(6));
      sliderPanel.add(this.confidenceSlider);
      sliderPanel.add(Box.createVerticalStrut(4));
      sliderPanel.add(this.buildColorLegend());
      return sliderPanel;
   }

   public void setScoreRange(double min, double max, CyNetworkView view) {
      if (view == null) {
         System.err.println("[NavDashboard] setScoreRange called with null view — ignoring");
      } else {
         if (min >= max) {
            System.err.println("[NavDashboard] setScoreRange: min (" + min + ") >= max (" + max + ") — expanding range by ±0.0001");
            min -= 1.0E-4;
            max += 1.0E-4;
         }

         this.scoreMin = min;
         this.scoreMax = max;
         this.activeView = view;
         this.confidenceSlider.removeChangeListener(this.sliderChangeListener);
         this.confidenceSlider.setEnabled(false);
         this.confidenceSlider.setMinimum(0);
         this.confidenceSlider.setMaximum(1000);
         this.confidenceSlider.setValue(0);
         this.confidenceSlider.setEnabled(true);
         this.confidenceSlider.addChangeListener(this.sliderChangeListener);
         this.thresholdInput.setEnabled(true);
         this.thresholdInput.setForeground(Color.BLACK);
         this.thresholdInput.setText(String.format("%.4f", min));
         this.searchBtn.setEnabled(true);
         this.sliderLabel.setText(String.format("Confidence Threshold: %.4f", min));
         this.sliderRangeLabel.setText(String.format("Range: %.4f – %.4f   |   Drag to hide low-confidence edges", min, max));
         System.out.println("[NavDashboard] setScoreRange: min=" + min + " max=" + max + " view=" + view.getSUID());
         this.applyThreshold();
      }
   }

   private void applySearchThreshold() {
      String text = this.thresholdInput.getText().trim();

      try {
         double value = Double.parseDouble(text);
         value = Math.max(this.scoreMin, Math.min(this.scoreMax, value));
         double fraction = this.scoreMax - this.scoreMin == (double)0.0F ? (double)0.0F : (value - this.scoreMin) / (this.scoreMax - this.scoreMin);
         int sliderPos = (int)Math.round(fraction * (double)1000.0F);
         this.thresholdInput.setForeground(Color.BLACK);
         this.confidenceSlider.setValue(sliderPos);
      } catch (NumberFormatException var7) {
         this.thresholdInput.setForeground(Color.RED);
         this.thresholdInput.setToolTipText("Invalid number: " + text);
      }

   }

   private void applyThreshold() {
      double fraction = (double)this.confidenceSlider.getValue() / (double)1000.0F;
      double threshold = this.scoreMin + fraction * (this.scoreMax - this.scoreMin);
      this.sliderLabel.setText(String.format("Confidence Threshold: %.4f", threshold));
      CyNetworkView view = this.activeView;
      if (view == null) {
         view = this.applicationManager.getCurrentNetworkView();
      }

      if (view == null) {
         System.err.println("[NavDashboard] applyThreshold: no view available — call setScoreRange() after importing a network");
      } else {
         CyNetwork network = (CyNetwork)view.getModel();
         if (network == null) {
            System.err.println("[NavDashboard] applyThreshold: view has no model");
         } else if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
            System.err.println("[NavDashboard] applyThreshold: 'confidence_score' column not found in edge table — check that the import task writes this column");
         } else {
            view.getEdgeViews().forEach((ev) -> {
               CyRow row = network.getRow((CyIdentifiable)ev.getModel());
               Double score = (Double)row.get("confidence_score", Double.class);
               if (score != null) {
                  if (score >= threshold) {
                     Color gradientColor = VisualUtil.scoreToColor(score, this.scoreMin, this.scoreMax);
                     ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
                     ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
                     ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
                  } else {
                     ev.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
                  }

               }
            });
            view.updateView();
         }
      }
   }

   private JPanel buildColorLegend() {
      JPanel wrapper = new JPanel(new BorderLayout(4, 0));
      JPanel gradientBar = new JPanel() {
         protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = this.getWidth();
            int h = this.getHeight();

            for(int x = 0; x < w; ++x) {
               float t = (float)x / (float)Math.max(w - 1, 1);
               g.setColor(new Color(t, 0.0F, 1.0F - t));
               g.fillRect(x, 0, 1, h);
            }

         }
      };
      gradientBar.setPreferredSize(new Dimension(200, 14));
      gradientBar.setOpaque(true);
      JLabel lowLabel = new JLabel("Low");
      lowLabel.setFont(lowLabel.getFont().deriveFont(10.0F));
      lowLabel.setForeground(new Color(0, 0, 180));
      JLabel highLabel = new JLabel("High");
      highLabel.setFont(highLabel.getFont().deriveFont(10.0F));
      highLabel.setForeground(new Color(180, 0, 0));
      wrapper.add(lowLabel, "West");
      wrapper.add(gradientBar, "Center");
      wrapper.add(highLabel, "East");
      return wrapper;
   }

   public static NavDashboard getExistingInstance() {
      return instance;
   }

   public static NavDashboard getInstance(TaskManager taskManager, CyApplicationManager applicationManager, CyNetworkViewWriterManager writerManager, FileUtil fileUtil, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager) {
      if (instance == null || !instance.isDisplayable()) {
         instance = new NavDashboard(taskManager, applicationManager, writerManager, fileUtil, vmm, vmfDiscrete, vmfPassthrough, vsFactory, networkFactory, networkManager, networkViewFactory, networkViewManager, layoutManager);
      }

      instance.setVisible(true);
      return instance;
   }
}