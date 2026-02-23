package com.blant.edgepredict.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

import com.blant.edgepredict.internal.ui.MenuAction;

public class CyActivator extends AbstractCyActivator {

    @Override
    public void start(BundleContext bc) {

        TaskManager taskManager = getService(bc, TaskManager.class);
        CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
        CyNetworkViewWriterManager writerManager =
                getService(bc, CyNetworkViewWriterManager.class);
        FileUtil fileUtil = getService(bc, FileUtil.class);

        MenuAction menuAction = new MenuAction(
                appManager,
                taskManager,
                writerManager,
                fileUtil
        );

        registerService(bc, menuAction, CyAction.class, new Properties());
    }
}
