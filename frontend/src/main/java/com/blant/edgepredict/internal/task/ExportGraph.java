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

public class ExportGraph {

    private static ExportGraph instance;
    private final CyApplicationManager appManager;
    private final CyNetworkViewWriterManager writerManager;

    public ExportGraph(CyApplicationManager appManager, CyNetworkViewWriterManager writerManager) {
        this.appManager = appManager;
        this.writerManager = writerManager;
    }

    // Must be called on EDT: captures the current view reference only
    public CyNetworkView captureView() {
        return appManager.getCurrentNetworkView();
    }

    // Must be called on a background thread: does all blocking I/O
    public File exportView(CyNetworkView view) throws Exception {
        if (view == null) {
            throw new IllegalStateException("No active graph view");
        }

        CyFileFilter sifFilter = null;
        for (CyFileFilter filter : writerManager.getAvailableWriterFilters()) {
            if (filter.getExtensions().contains("sif")) {
                sifFilter = filter;
                break;
            }
        }
        if (sifFilter == null) {
            throw new IllegalStateException("SIF writer not available");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CyWriter writer = writerManager.getWriter(view, sifFilter, out);
        if (writer == null) {
            throw new IllegalStateException("Could not create SIF writer for current view");
        }

        BlantLogWindow.getInstance().appendLog("[INFO] Exporting current graph...");

        TaskMonitor noOp = new TaskMonitor() {
            public void setTitle(String s) {}
            public void setProgress(double p) {}
            public void setStatusMessage(String s) {}
            public void showMessage(TaskMonitor.Level l, String s) {}
        };
        writer.run(noOp);

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
