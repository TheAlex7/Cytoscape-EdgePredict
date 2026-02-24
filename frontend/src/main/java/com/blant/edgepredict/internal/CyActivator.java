package com.blant.edgepredict.internal;

import java.util.Properties;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.work.TaskManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.osgi.framework.BundleContext;

import com.blant.edgepredict.internal.ui.MenuAction;
import com.blant.edgepredict.internal.util.VisualUtil;

// This class is the entry point of the app and registers the menu item and dashboard
public class CyActivator extends AbstractCyActivator {
    // This method is called when the app starts, and it registers the menu action to open the dashboard
    @Override
    public void start(BundleContext bc) {
        CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
        TaskManager taskManager = getService(bc, TaskManager.class);
        CyNetworkViewWriterManager writerManager = getService(bc, CyNetworkViewWriterManager.class);
        FileUtil fileUtil = getService(bc, FileUtil.class);
        VisualMappingManager vmm = getService(bc, VisualMappingManager.class);
        VisualStyleFactory vsFactory = getService(bc, VisualStyleFactory.class);
        VisualMappingFunctionFactory vmfFactoryDiscrete = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");

        // Run on launch below to apply styles to any existing network views
        if (appManager.getCurrentNetworkView() != null) {
            VisualUtil.applyStyles(appManager.getCurrentNetworkView(), vmm, vmfFactoryDiscrete, vsFactory);
        }

        // Register the menu action to open the dashboard
        MenuAction menuAction = new MenuAction(appManager, taskManager, writerManager, fileUtil, vmm, vmfFactoryDiscrete, vsFactory);
        registerService(bc, menuAction, CyAction.class, new Properties());
    }
}