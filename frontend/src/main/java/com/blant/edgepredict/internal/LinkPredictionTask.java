package com.blant.edgepredict.internal;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class LinkPredictionTask extends AbstractTask {

    @Override
    public void run(TaskMonitor taskMonitor) {
        // This is where you put your TaskMonitor code!
        taskMonitor.setTitle("BLANT Link Prediction");
        
        taskMonitor.setStatusMessage("Initializing algorithm...");
        taskMonitor.setProgress(0.1);

        try {
            // Simulate your link prediction logic
            Thread.sleep(2000); 
            
            taskMonitor.setStatusMessage("Scanning network nodes...");
            taskMonitor.setProgress(0.5);
            
            Thread.sleep(2000);
            
            taskMonitor.setStatusMessage("Prediction complete!");
            taskMonitor.setProgress(1.0);
            
        } catch (InterruptedException e) {
            taskMonitor.setStatusMessage("Task was interrupted!");
        }
    }
}