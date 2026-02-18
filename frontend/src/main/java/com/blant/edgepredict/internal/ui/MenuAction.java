package com.blant.edgepredict.internal.ui;

import java.awt.event.ActionEvent;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.TaskManager;      // Add this import
import org.cytoscape.work.TaskIterator;   // Add this import
import com.blant.edgepredict.internal.LinkPredictionTask; // Ensure this is imported

public class MenuAction extends AbstractCyAction {
    private final CyApplicationManager appManager;
    private final TaskManager taskManager;

    // This is the Constructor - it must accept TWO arguments
    public MenuAction(CyApplicationManager appManager, TaskManager taskManager) {
        super("Run Link Prediction");
        this.appManager = appManager;
        this.taskManager = taskManager;
        setPreferredMenu("Apps.BLANT");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        // Check if it's already open, if not, create it
        NavDashboard dash = new NavDashboard(taskManager);
        dash.setVisible(true);
        dash.toFront(); // Brings it to the top of Cytoscape
    }
}