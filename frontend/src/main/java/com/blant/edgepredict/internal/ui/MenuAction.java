package com.blant.edgepredict.internal;

import java.awt.event.ActionEvent;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;
import javax.swing.JOptionPane;

public class MenuAction extends AbstractCyAction {
    private final CyApplicationManager appManager;

    public MenuAction(CyApplicationManager appManager) {
        super("Run Link Prediction");
        this.appManager = appManager;
        // This puts your app in the "Apps" menu at the top of Cytoscape
        setPreferredMenu("Apps.UCI Link Predictor");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // This checks if the user actually has a network open
        if (appManager.getCurrentNetwork() == null) {
            JOptionPane.showMessageDialog(null, "Please open a network first!");
            return;
        }
        
        JOptionPane.showMessageDialog(null, "Predicting links for UCI project...");
        // This is where you will eventually call your algorithm class
    }
}