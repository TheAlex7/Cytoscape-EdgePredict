package com.blant.edgepredict.internal.task;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;
import java.awt.Component;
import java.lang.System.Logger.Level;
import javax.swing.JOptionPane;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;

public class PredictTaskManager {
   private final String sampleMethod;
   private final int precisionDigits;
   private final int kVal;
   private final boolean isSaved;
   private final boolean isMultithreaded;
   private final FileUtil fileUtil;
   private final CyNetworkFactory networkFactory;
   private final CyNetworkManager networkManager;
   private final CyNetworkViewFactory networkViewFactory;
   private final CyNetworkViewManager networkViewManager;
   private final CyLayoutAlgorithmManager layoutManager;
   private final VisualMappingManager vmm;
   private final VisualMappingFunctionFactory vmfDiscrete;
   private final VisualMappingFunctionFactory vmfPassthrough;
   private final VisualStyleFactory vsFactory;

   public PredictTaskManager(FileUtil fileUtil, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, String sampleMethod, int precisionDigits, int kVal, boolean isSaved, boolean isMultithreaded) {
      this.sampleMethod = sampleMethod;
      this.precisionDigits = precisionDigits;
      this.kVal = kVal;
      this.isSaved = isSaved;
      this.isMultithreaded = isMultithreaded;
      this.fileUtil = fileUtil;
      this.networkFactory = networkFactory;
      this.networkManager = networkManager;
      this.networkViewFactory = networkViewFactory;
      this.networkViewManager = networkViewManager;
      this.layoutManager = layoutManager;
      this.vmm = vmm;
      this.vmfDiscrete = vmfDiscrete;
      this.vmfPassthrough = vmfPassthrough;
      this.vsFactory = vsFactory;
   }

   public void run() {
      BlantLogWindow logWindow = BlantLogWindow.getInstance();
      BlantPoller poller = BlantPoller.getInstance();

      try {
         SendToBlant sendTask = new SendToBlant(this.fileUtil, this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved, this.isMultithreaded, logWindow);
         boolean status = sendTask.send();
         if (status) {
            poller.startPolling(BlantConfig.getJobId(), () -> {
               try {
                  ImportGraph importTask = new ImportGraph(this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.layoutManager, this.vmm, this.vmfDiscrete, this.vmfPassthrough, this.vsFactory, this.isSaved, logWindow);
                  importTask.importFile();
               } catch (Exception ex) {
                  System.getLogger(PredictTaskManager.class.getName()).log(Level.ERROR, (String)null, ex);
               }

            });
         } else {
            JOptionPane.showMessageDialog((Component)null, "Task cancelled due to the user input.");
         }
      } catch (Exception var5) {
      }

   }
}
