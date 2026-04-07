package com.blant.edgepredict.internal.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class ExportGraph {

    private static final String FORMAT_SIF = "SIF";
    private static final String FORMAT_EL  = "EL (Edge List)";

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

        // 1. Ask which format the user wants
        String[] options = { FORMAT_SIF, FORMAT_EL };
        int choice = JOptionPane.showOptionDialog(
                null,
                "Select export format:",
                "Export Network",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0) return; // dialog closed/cancelled

        if (choice == 0) {
            exportAsSif(currentView);
        } else {
            exportAsEl(currentView);
        }
    }

    // ------------------------------------------------------------------ SIF --

    private void exportAsSif(CyNetworkView view) throws Exception {

        CyFileFilter sifFilter = null;
        for (CyFileFilter f : writerManager.getAvailableWriterFilters()) {
            if (f.getExtensions().contains("sif")) {
                sifFilter = f;
                break;
            }
        }

        if (sifFilter == null) {
            JOptionPane.showMessageDialog(null, "SIF writer not found.");
            return;
        }

        File file = chooseFile("Save Network as SIF",
                new FileChooserFilter("SIF Network (*.sif)", "sif"));
        if (file == null) return;

        if (!file.getName().toLowerCase().endsWith(".sif")) {
            file = new File(file.getAbsolutePath() + ".sif");
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            CyWriter writer = writerManager.getWriter(view, sifFilter, out);
            writer.run(null);
        }

        notify(file);
    }

    // ------------------------------------------------------------------- EL --

    /**
     * Edge-list format: one edge per line as "sourceName targetName".
     * Node names come from CyNetwork.NAME; if absent the SUID is used as fallback.
     */
    private void exportAsEl(CyNetworkView view) throws Exception {

        File file = chooseFile("Save Network as Edge List",
                new FileChooserFilter("Edge List (*.el)", "el"));
        if (file == null) return;

        if (!file.getName().toLowerCase().endsWith(".el")) {
            file = new File(file.getAbsolutePath() + ".el");
        }

        CyNetwork network = view.getModel();

        try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
            for (CyEdge edge : network.getEdgeList()) {
                String src = nodeName(network, edge.getSource());
                String tgt = nodeName(network, edge.getTarget());
                pw.println(src + "\t" + tgt);
            }
        }

        notify(file);
    }

    // ---------------------------------------------------------------- utils --

    private File chooseFile(String title, FileChooserFilter filter) {
        return fileUtil.getFile(
                JOptionPane.getRootFrame(),
                title,
                FileUtil.SAVE,
                Collections.singletonList(filter));
    }

    private String nodeName(CyNetwork network, CyNode node) {
        String name = network.getRow(node).get(CyNetwork.NAME, String.class);
        return (name != null && !name.isBlank()) ? name : String.valueOf(node.getSUID());
    }

    private void notify(File file) {
        JOptionPane.showMessageDialog(null,
                "Export complete:\n" + file.getAbsolutePath());
    }
}