package com.blant.edgepredict.internal.task;

import java.lang.System.Logger.Level;
import java.util.List;

import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import com.blant.edgepredict.internal.ui.BlantLogWindow;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.BlantPoller;
import com.blant.edgepredict.internal.util.DockerUtil;

public class PredictTaskManager {
    private final String sampleMethod;
    private final double precisionDigits;
    private final List<String> kVal;
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
    private final DialogTaskManager dialogTaskManager;
    private final boolean isOnline;

    public PredictTaskManager(FileUtil fileUtil, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, DialogTaskManager dialogTaskManager, String sampleMethod, double precisionDigits, List<String> kVal, boolean isSaved, boolean isOnline) {
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
        this.dialogTaskManager = dialogTaskManager;
        this.isOnline = isOnline;
    }

    public void run() {
        if (!isOnline) {
            dialogTaskManager.execute(new TaskIterator(new DockerUtil()), new org.cytoscape.work.TaskObserver() {
                @Override
                public void allFinished(org.cytoscape.work.FinishStatus finishStatus) {
                    // Only show the dashboard if Docker setup succeeded.
                    if (finishStatus.getType() == org.cytoscape.work.FinishStatus.Type.SUCCEEDED) task();
                }
                    @Override
                    public void taskFinished(org.cytoscape.work.ObservableTask task) {}
            });
        } else {
            task();
        }
    }

    private void task() {
        BlantLogWindow logWindow = BlantLogWindow.getInstance();
        logWindow.setVisible(true);

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
                boolean status = sendTask.send(file, isOnline);
                if (!status && BlantConfig.getLoad()) {
                    status = sendTask.send(file, isOnline);
                }
                if (status) {
                    BlantPoller.getInstance().startPolling(BlantConfig.getJobId(), isOnline, () -> {
                        try {
                            new ImportGraph(this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.layoutManager, this.vmm, this.vmfDiscrete, this.vmfPassthrough, this.vsFactory, this.isSaved, logWindow).importFile(isOnline);
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
