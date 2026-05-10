package com.blant.edgepredict.internal.ui;

import java.awt.event.ActionEvent;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;

import com.blant.edgepredict.internal.util.CacheUtil;

public class MenuAction extends AbstractCyAction {

    private final CyApplicationManager appManager;
    private final TaskManager taskManager;
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
    private final DialogTaskManager dialogTaskManager;

    public MenuAction(CyApplicationManager appManager,
                      TaskManager taskManager,
                      CyNetworkViewWriterManager writerManager,
                      FileUtil fileUtil,
                      VisualMappingManager vmm,
                      VisualMappingFunctionFactory vmfDiscrete,
                      VisualMappingFunctionFactory vmfPassthrough,
                      VisualStyleFactory vsFactory,
                      CyNetworkFactory networkFactory,
                      CyNetworkManager networkManager,
                      CyNetworkViewFactory networkViewFactory,
                      CyNetworkViewManager networkViewManager,
                      CyLayoutAlgorithmManager layoutManager,
                      DialogTaskManager dialogTaskManager) {
        super("BLANT Prediction Dashboard");
        this.appManager = appManager;
        this.taskManager = taskManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;
        this.vmm = vmm;
        this.vmfDiscrete = vmfDiscrete;
        this.vmfPassthrough = vmfPassthrough;
        this.vsFactory = vsFactory;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;
        this.dialogTaskManager = dialogTaskManager;
        setPreferredMenu("Apps");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!CacheUtil.CONFIG_DIR.exists()) CacheUtil.CONFIG_DIR.mkdirs();
        
        NavDashboard dashboard = NavDashboard.getInstance(
            taskManager, appManager, writerManager, fileUtil,
            vmm, vmfDiscrete, vmfPassthrough, vsFactory,
            networkFactory, networkManager,
            networkViewFactory, networkViewManager,
            layoutManager, dialogTaskManager);
            dashboard.setVisible(true);
    }

    
}