package com.blant.edgepredict.internal;

import java.util.Properties;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.work.TaskManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.osgi.framework.BundleContext;

import com.blant.edgepredict.internal.ui.EdgeDetailPanel;
import com.blant.edgepredict.internal.ui.MenuAction;
import com.blant.edgepredict.internal.util.VisualUtil;

public class CyActivator extends AbstractCyActivator {

    @Override
    public void start(BundleContext bc) {
        CyApplicationManager appManager         = getService(bc, CyApplicationManager.class);
        TaskManager taskManager                 = getService(bc, TaskManager.class);
        CyNetworkViewWriterManager writerManager = getService(bc, CyNetworkViewWriterManager.class);
        FileUtil fileUtil                        = getService(bc, FileUtil.class);
        VisualMappingManager vmm                 = getService(bc, VisualMappingManager.class);
        VisualStyleFactory vsFactory             = getService(bc, VisualStyleFactory.class);
        CyNetworkFactory networkFactory          = getService(bc, CyNetworkFactory.class);
        CyNetworkManager networkManager          = getService(bc, CyNetworkManager.class);
        CyNetworkViewFactory networkViewFactory  = getService(bc, CyNetworkViewFactory.class);
        CyNetworkViewManager networkViewManager  = getService(bc, CyNetworkViewManager.class);
        CyLayoutAlgorithmManager layoutManager   = getService(bc, CyLayoutAlgorithmManager.class);

        VisualMappingFunctionFactory vmfDiscrete =
                getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        VisualMappingFunctionFactory vmfPassthrough =
                getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

        if (appManager.getCurrentNetworkView() != null) {
            VisualUtil.applyStyles(
                    appManager.getCurrentNetworkView(),
                    vmm, vmfDiscrete, vmfPassthrough, vsFactory);
        }

        // Register edge selection listener for EdgeDetailPanel popup
        EdgeDetailPanel.EdgeSelectionListener edgeListener =
                new EdgeDetailPanel.EdgeSelectionListener(appManager);
        registerService(bc, edgeListener, RowsSetListener.class, new Properties());

        MenuAction menuAction = new MenuAction(
                appManager, taskManager, writerManager, fileUtil,
                vmm, vmfDiscrete, vmfPassthrough, vsFactory,
                networkFactory, networkManager,
                networkViewFactory, networkViewManager,
                layoutManager);

        registerService(bc, menuAction, CyAction.class, new Properties());
    }
}