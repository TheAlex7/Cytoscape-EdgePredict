package com.blant.edgepredict.internal;

import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import java.util.Properties;

public class CyActivator extends AbstractCyActivator {
    @Override
    public void start(BundleContext bc) {
        // 1. Get the Application Manager (this is a built-in Cytoscape service)
        CyApplicationManager appManager = getService(bc, CyApplicationManager.class);

        // 2. Initialize your Menu Action
        MenuAction menuAction = new MenuAction(appManager);

        // 3. Register it so it appears in the menu
        registerService(bc, menuAction, CyAction.class, new Properties());
    }
}