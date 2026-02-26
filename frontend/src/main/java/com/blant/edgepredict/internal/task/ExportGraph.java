package com.blant.edgepredict.internal.task;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;

import javax.swing.JOptionPane; 

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class ExportGraph {

    private final CyApplicationManager appManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;

    public ExportGraph(CyApplicationManager appManager,
                       CyNetworkViewWriterManager writerManager,
                       FileUtil fileUtil) {
        this.appManager = appManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;
    }

    public void export() throws Exception {

        CyNetworkView currentView = appManager.getCurrentNetworkView();
        if (currentView == null) {
            JOptionPane.showMessageDialog(null, "No active network view found.");
            return;
        }

        CyFileFilter sifFilter = null;
        for (CyFileFilter filter : writerManager.getAvailableWriterFilters()) {
            if (filter.getExtensions().contains("sif")) {
                sifFilter = filter;
                break;
            }
        }

        if (sifFilter == null) {
            JOptionPane.showMessageDialog(null, "SIF writer not found.");
            return;
        }

        FileChooserFilter chooserFilter =
                new FileChooserFilter("SIF Network (*.sif)", "sif");

        File file = fileUtil.getFile(
                JOptionPane.getRootFrame(),
                "Save Network as SIF",
                FileUtil.SAVE,
                Collections.singletonList(chooserFilter)
        );

        if (file == null) {
            return;  // user cancelled — nothing to block
        }

        if (!file.getName().toLowerCase().endsWith(".sif")) {
            file = new File(file.getAbsolutePath() + ".sif");
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            CyWriter writer = writerManager.getWriter(currentView, sifFilter, out);
            writer.run(null);
        }

        JOptionPane.showMessageDialog(null,
                "Export complete:\n" + file.getAbsolutePath());
    }
}
