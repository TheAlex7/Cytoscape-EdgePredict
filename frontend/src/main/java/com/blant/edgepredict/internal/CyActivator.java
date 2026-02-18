package com.blant.edgepredict.internal;

import java.util.Properties;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskManager; // Import this
import org.osgi.framework.BundleContext;
import com.blant.edgepredict.internal.ui.MenuAction;

public class CyActivator extends AbstractCyActivator {
    @Override
    public void start(BundleContext bc) {
        TaskManager taskManager = getService(bc, TaskManager.class);
        CyApplicationManager appManager = getService(bc, CyApplicationManager.class);

        MenuAction menuAction = new MenuAction(appManager, taskManager);

        // 4. Register it so it appears in the menu
        registerService(bc, menuAction, CyAction.class, new Properties());
    }
}