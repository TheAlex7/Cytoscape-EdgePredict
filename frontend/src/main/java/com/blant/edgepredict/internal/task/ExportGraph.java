package com.blant.edgepredict.internal.task;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;

import com.blant.edgepredict.internal.ui.BlantLogWindow;

public class ExportGraph{
    
    private static ExportGraph instance;
    private final CyApplicationManager appManager;
    private final CyNetworkViewWriterManager writerManager;

    public ExportGraph(CyApplicationManager appManager, CyNetworkViewWriterManager writerManager) {
        this.appManager = appManager;
        this.writerManager = writerManager;
    }

    public File ExportCurrentGraph(TaskMonitor taskMonitor) throws Exception{
        // bring currently activated graph view
        CyNetworkView currentView = appManager.getCurrentNetworkView();
        if (currentView == null) {
            throw new IllegalStateException("There are no currently activated graph views");
        }

        // get sif file filter && create sif writer
        CyFileFilter sifFilter = null;
        for (CyFileFilter filter : writerManager.getAvailableWriterFilters()) {
            if (filter.getExtensions().contains("sif")) {
                sifFilter = filter;
                break;
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CyWriter writer = writerManager.getWriter(currentView, sifFilter, out);
        
        // Write the graph && return
        BlantLogWindow logWindow = BlantLogWindow.getInstance();
        logWindow.appendLog("[INFO] Exporting current graph...");
        writer.run(taskMonitor);
        
        byte[] graphBytes = out.toByteArray();
        File tempFile = File.createTempFile("blant_graph_", ".sif");
        tempFile.deleteOnExit();

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(graphBytes);
        }
        
        return tempFile;
    }

    public static void setInstance(CyApplicationManager appManager, CyNetworkViewWriterManager writerManager) {
        instance = new ExportGraph(appManager, writerManager);
    }

    public static ExportGraph getInstance() {
        return instance;
    }
}