package com.blant.edgepredict.internal.task;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;
import java.lang.System.Logger.Level;
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

    public PredictTaskManager(FileUtil fileUtil, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, String sampleMethod, int precisionDigits, int kVal, boolean isSaved) {
        this.sampleMethod = sampleMethod;
        this.precisionDigits = precisionDigits;
        this.kVal = kVal;
        this.isSaved = isSaved;
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

        // File selection must happen on the EDT — returns immediately.
        SendToBlant sendTask = new SendToBlant(this.fileUtil, this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved, logWindow);
        java.io.File file = sendTask.selectFile();
        if (file == null) {
            logWindow.appendLog("[INFO] Send to BLANT cancelled by user.");
            return;
        }

        // HTTP upload, polling, and import all run off the EDT so the UI stays responsive.
        new Thread(() -> {
            try {
                boolean status = sendTask.send(file);
                if (status) {
                    BlantPoller.getInstance().startPolling(BlantConfig.getJobId(), () -> {
                        try {
                            new ImportGraph(this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.layoutManager, this.vmm, this.vmfDiscrete, this.vmfPassthrough, this.vsFactory, this.isSaved, logWindow).importFile();
                        } catch (Exception ex) {
                            System.getLogger(PredictTaskManager.class.getName()).log(Level.ERROR, (String) null, ex);
                        }
                    });
                }
            } catch (Exception e) {
                logWindow.appendLog("[ERROR] Task failed: " + e.getMessage());
            }
        }, "blant-send").start();
    }
}
