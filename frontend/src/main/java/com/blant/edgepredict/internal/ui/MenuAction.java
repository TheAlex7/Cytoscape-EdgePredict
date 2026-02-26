package com.blant.edgepredict.internal.ui;

import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;

public class MenuAction extends AbstractCyAction {

    private final CyApplicationManager appManager;
    private final TaskManager taskManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmf;
    private final VisualStyleFactory vsFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;

    public MenuAction(CyApplicationManager appManager,
                      TaskManager taskManager,
                      CyNetworkViewWriterManager writerManager,
                      FileUtil fileUtil,
                      VisualMappingManager vmm,
                      VisualMappingFunctionFactory vmf,
                      VisualStyleFactory vsFactory,
                      CyNetworkFactory networkFactory,
                      CyNetworkManager networkManager,
                      CyNetworkViewFactory networkViewFactory,
                      CyNetworkViewManager networkViewManager) {
        super("BLANT Prediction Dashboard");
        this.appManager = appManager;
        this.taskManager = taskManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;
        this.vmm = vmm;
        this.vmf = vmf;
        this.vsFactory = vsFactory;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        setPreferredMenu("Apps");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NavDashboard dashboard = new NavDashboard(taskManager, appManager, writerManager, fileUtil, vmm, vmf, vsFactory,
                networkFactory, networkManager, networkViewFactory, networkViewManager);
        dashboard.setVisible(true);
    }
}