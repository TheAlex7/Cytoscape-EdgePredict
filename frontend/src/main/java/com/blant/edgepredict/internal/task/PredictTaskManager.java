package com.blant.edgepredict.internal.task;

import java.io.File;
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
    private final Boolean isFile;

    public PredictTaskManager(FileUtil fileUtil, CyNetworkFactory networkFactory, CyNetworkManager networkManager, CyNetworkViewFactory networkViewFactory, CyNetworkViewManager networkViewManager, CyLayoutAlgorithmManager layoutManager, VisualMappingManager vmm, VisualMappingFunctionFactory vmfDiscrete, VisualMappingFunctionFactory vmfPassthrough, VisualStyleFactory vsFactory, DialogTaskManager dialogTaskManager, String sampleMethod, double precisionDigits, List<String> kVal, boolean isSaved, Boolean isFile) {
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
        this.isFile = isFile;
    }

    public void run() {
        if (!BlantConfig.getOnline()) {
            if (!DockerUtil.isDockerInstalled()) {
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Docker is not installed. Please install Docker and try again.",
                    "Docker Not Found",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
                DockerUtil.openInstallationPage();
                return;
            }
            dialogTaskManager.execute(new TaskIterator(new DockerUtil()), new org.cytoscape.work.TaskObserver() {
                @Override
                public void allFinished(org.cytoscape.work.FinishStatus finishStatus) {
                    if (finishStatus.getType() == org.cytoscape.work.FinishStatus.Type.SUCCEEDED) {
                        task();
                    }
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
        SendToBlant sendTask = new SendToBlant(this.fileUtil, this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.sampleMethod, this.precisionDigits, this.kVal, this.isSaved, logWindow);
        logWindow.setVisible(true);

        // selectFile() handles EDT dispatch internally via invokeAndWait — safe to call from background
        new Thread(() -> {
            File file = sendTask.selectFile(this.isFile);
            if (file == null) {
                logWindow.appendLog("[INFO] Send to BLANT cancelled.");
                return;
            }
            runSend(sendTask, file, logWindow);
        }).start();
    }

    private void runSend(SendToBlant sendTask, File file, BlantLogWindow logWindow) {
        try {
            boolean status = sendTask.send(file);

            if (status) {
                BlantPoller.getInstance().startPolling(BlantConfig.getJobId(), () -> {
                    if (!BlantConfig.getAborted()) {
                        new Thread(() -> {
                            try {
                                new ImportGraph(this.networkFactory, this.networkManager, this.networkViewFactory, this.networkViewManager, this.layoutManager, this.vmm, this.vmfDiscrete, this.vmfPassthrough, this.vsFactory, this.isSaved, logWindow).importFile();
                            } catch (Exception ex) {
                                System.getLogger(PredictTaskManager.class.getName()).log(Level.ERROR, (String) null, ex);
                                logWindow.appendLog("[ERROR] Failed to import result: " + ex.getMessage());
                                String jobId = BlantConfig.getJobId();
                                if (jobId != null) {
                                    String stderr = BlantPoller.fetchStderr(jobId);
                                    if (stderr != null && !stderr.isBlank()) {
                                        logWindow.appendLog("[STDERR]\n" + stderr);
                                    }
                                }
                            } finally {
                                cleanupResources();
                            }
                        }).start();
                    } else {
                        cleanupResources();
                    }
                });
            } else {
                cleanupResources();
            }
        } catch (Exception e) {
            logWindow.appendLog("[ERROR] Task failed: " + e.getMessage());
            cleanupResources();
        }
    }

    private void cleanupResources() {
        if (!BlantConfig.getOnline()) {
            new DockerUtil().closeDocker();
            BlantPoller.getInstance().stopPolling();
            BlantConfig.setJobId(null);
            BlantConfig.setLoad(false);
            BlantConfig.setProgress(0);
        }
    }
}