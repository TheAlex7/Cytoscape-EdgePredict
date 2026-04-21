package com.blant.edgepredict.internal.task;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.ui.NavDashboard;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.CacheUtil;
import com.blant.edgepredict.internal.util.VisualUtil;
import java.awt.Color;
import java.awt.Component;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class ImportGraph {
   private final CyNetworkFactory networkFactory;
   private final CyNetworkManager networkManager;
   private final CyNetworkViewFactory networkViewFactory;
   private final CyNetworkViewManager networkViewManager;
   private final CyLayoutAlgorithmManager layoutManager;
   private final VisualMappingManager vmm;
   private final VisualMappingFunctionFactory vmfDiscrete;
   private final VisualMappingFunctionFactory vmfPassthrough;
   private final VisualStyleFactory vsFactory;
   private final boolean isSaved;
   private final BlantLogWindow logWindow;

   public ImportGraph(CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, boolean isSaved, BlantLogWindow logWindow) {
      this.networkFactory = networkFactory;
      this.networkManager = networkManager;
      this.networkViewFactory = networkViewFactory;
      this.networkViewManager = networkViewManager;
      this.layoutManager = layoutManager;
      this.vmm = vmm;
      this.vmfDiscrete = vmfDiscrete;
      this.vmfPassthrough = vmfPassthrough;
      this.vsFactory = vsFactory;
      this.isSaved = isSaved;
      this.logWindow = logWindow;
   }

   public void importFile() throws Exception {
      String jobId = BlantConfig.getJobId();
      if (jobId != null && !jobId.isBlank()) {
         if (BlantConfig.getLoad()) {
            String responseText = CacheUtil.getOutput(jobId);
            this.logWindow.appendLog(responseText);
            if (responseText.contains("ERROR")) {
               SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog((Component)null, "Locally saved file cannot be read."));
            } else {
               SwingUtilities.invokeLater(() -> {
                  try {
                     this.processResult(responseText, jobId);
                  } catch (Exception ex) {
                     ex.printStackTrace();
                  }

               });
            }
         } else {
            this.logWindow.appendLog("[INFO] Fetching result for job ID: " + jobId);
            URL url = new URL(BlantConfig.RESULTS_URL + jobId);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            int status = conn.getResponseCode();
            if (status != 200) {
               String error = conn.getErrorStream() != null ? new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8) : "Unknown error";
               this.logWindow.appendLog("[ERROR] Failed to fetch result: " + error);
               SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog((Component)null, "Failed to fetch BLANT result: " + error));
            } else {
               String responseText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
               this.logWindow.appendLog("[INFO] Result received. Parsing network...");
               this.logWindow.appendLog("[DEBUG] Raw response:\n" + responseText);
               SwingUtilities.invokeLater(() -> {
                  try {
                     this.processResult(responseText, jobId);
                  } catch (Exception ex) {
                     ex.printStackTrace();
                  }

               });
            }
         }
      } else {
         SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog((Component)null, "No BLANT job ID found. Please run 'Send to BLANT' first."));
      }
   }

   private void processResult(String responseText, String jobId) throws Exception {
      CyNetwork network = this.networkFactory.createNetwork();
      network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", "BLANT Result");
      if (network.getDefaultEdgeTable().getColumn("confidence_score") == null) {
         network.getDefaultEdgeTable().createColumn("confidence_score", Double.class, false);
      }

      if (network.getDefaultEdgeTable().getColumn("orbit_pair") == null) {
         network.getDefaultEdgeTable().createColumn("orbit_pair", String.class, false);
      }

      Map<String, CyNode> nodeMap = new HashMap();
      int added = 0;
      VisualUtil.showTableDialogue(responseText);

      for(String line : responseText.split("\n")) {
         line = line.trim();
         if (!line.isEmpty() && !line.startsWith("#")) {
            String[] row = line.split("\\s+");
            if (row.length < 3) {
               this.logWindow.appendLog("[WARN] Skipping malformed line: " + line);
            } else {
               String sourceName = row[0];
               String interaction = "predicted";
               String targetName = row[1];
               Double score = null;

               try {
                  score = Double.parseDouble(row[2]);
               } catch (Exception var19) {
                  this.logWindow.appendLog("[WARN] Could not parse score: " + line);
               }

               String orbitPair = row.length >= 4 ? row[3] : null;
               CyNode source = (CyNode)nodeMap.computeIfAbsent(sourceName, (name) -> {
                  CyNode n = network.addNode();
                  network.getRow(n).set("name", name);
                  return n;
               });
               CyNode target = (CyNode)nodeMap.computeIfAbsent(targetName, (name) -> {
                  CyNode n = network.addNode();
                  network.getRow(n).set("name", name);
                  return n;
               });
               CyEdge edge = network.addEdge(source, target, false);
               network.getRow(edge).set("name", sourceName + " (" + interaction + ") " + targetName);
               network.getRow(edge).set("interaction", interaction);
               if (score != null) {
                  network.getRow(edge).set("confidence_score", score);
               }

               if (orbitPair != null) {
                  network.getRow(edge).set("orbit_pair", orbitPair);
               }

               ++added;
               this.logWindow.appendLog("[DEBUG] Edge: " + sourceName + " -> " + targetName + " (" + interaction + ") score=" + score + " orbit_pair=" + orbitPair);
            }
         }
      }

      double scoreMin = Double.MAX_VALUE;
      double scoreMax = -Double.MAX_VALUE;

      for(CyEdge edge : network.getEdgeList()) {
         Double score = (Double)network.getRow(edge).get("confidence_score", Double.class);
         if (score != null) {
            if (score < scoreMin) {
               scoreMin = score;
            }

            if (score > scoreMax) {
               scoreMax = score;
            }
         }
      }

      if (scoreMin == Double.MAX_VALUE) {
         scoreMin = (double)0.0F;
         scoreMax = (double)1.0F;
      }

      this.logWindow.appendLog("[INFO] Score range: min=" + scoreMin + ", max=" + scoreMax);
      this.networkManager.addNetwork(network);
      CyNetworkView view = this.networkViewFactory.createNetworkView(network);
      this.networkViewManager.addNetworkView(view);
      CyLayoutAlgorithm layout = this.layoutManager.getDefaultLayout();
      TaskIterator it = layout.createTaskIterator(view, layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS, (String)null);

      while(it.hasNext()) {
         it.next().run(new TaskMonitor() {
            public void setTitle(String t) {
            }

            public void setProgress(double p) {
            }

            public void setStatusMessage(String m) {
            }

            public void showMessage(TaskMonitor.Level l, String m) {
            }
         });
      }

      VisualUtil.applyStyles(view, this.vmm, this.vmfDiscrete, this.vmfPassthrough, this.vsFactory);

      // Fix: capture effectively-final copies for use inside the lambda
      final double finalScoreMin = scoreMin;
      final double finalScoreMax = scoreMax;

      view.getEdgeViews().forEach((ev) -> {
         CyEdge edgeModel = (CyEdge)ev.getModel();
         Double score = (Double)network.getRow(edgeModel).get("confidence_score", Double.class);
         String orbitPair = (String)network.getRow(edgeModel).get("orbit_pair", String.class);
         String edgeName = (String)network.getRow(edgeModel).get("name", String.class);
         if (score != null) {
            Color gradientColor = VisualUtil.scoreToColor(score, finalScoreMin, finalScoreMax);
            ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, gradientColor);
            ev.setLockedValue(BasicVisualLexicon.EDGE_PAINT, gradientColor);
         }

         String tooltip = "<html><b>" + edgeName + "</b><br>Score: " + (score != null ? String.format("%.6f", score) : "—") + "<br>Orbit Pair: " + (orbitPair != null ? orbitPair : "—") + "</html>";
         ev.setLockedValue(BasicVisualLexicon.EDGE_TOOLTIP, tooltip);
      });

      view.updateView();
      this.logWindow.appendLog("[INFO] Network loaded: " + added + " edges added.");
      if (this.isSaved) {
         this.logWindow.appendLog(CacheUtil.saveOutput(jobId, responseText));
      }

      NavDashboard dashboard = NavDashboard.getExistingInstance();
      if (dashboard != null) {
         dashboard.setScoreRange(scoreMin, scoreMax, view);
      }

      JOptionPane.showMessageDialog((Component)null, "Import complete: " + added + " edges added.\n" + String.format("Score range: %.4f – %.4f", scoreMin, scoreMax));
   }
}