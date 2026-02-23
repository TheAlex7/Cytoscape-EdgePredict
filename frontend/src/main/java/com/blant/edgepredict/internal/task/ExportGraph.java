package com.blant.edgepredict.internal.task;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.awt.Dimension;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.io.CyFileFilter;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class ExportGraph extends AbstractTask {
    
    private final CyApplicationManager appManager;
    private final CyNetworkViewWriterManager writerManager;

    public ExportGraph(CyApplicationManager appManager, CyNetworkViewWriterManager writerManager) {
        this.appManager = appManager;
        this.writerManager = writerManager;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception{
        taskMonitor.setTitle("Export graph into text...");
        
        taskMonitor.setStatusMessage("Initializing algorithm...");
        taskMonitor.setProgress(0.1);
        if (this.cancelled) return;

        // bring currently activated graph view
        CyNetworkView currentView = appManager.getCurrentNetworkView();
        if (currentView == null) {
            throw new IllegalStateException("There are no currently activated graph views");
        }
        taskMonitor.setStatusMessage("Converting network into sif format text...");    
        taskMonitor.setProgress(0.4);
        if (this.cancelled) return;

        // get sif file filter && create sif writer
        CyFileFilter sifFilter = null;
        for (CyFileFilter filter : writerManager.getAvailableWriterFilters()) {
            if (this.cancelled) return;
            if (filter.getExtensions().contains("sif")) {
                sifFilter = filter;
                break;
            }
        }
        if (sifFilter == null) {
            throw new IllegalStateException("Error: Cannot find CyFileFilter for .sif format to write. (./task/getGraph.java)");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CyWriter writer = writerManager.getWriter(currentView, sifFilter, out);
        if (this.cancelled) return;
        
        // Write the graph && return
        writer.run(taskMonitor);
        String sifResult = out.toString("UTF-8");
        taskMonitor.setProgress(1.0);
        if (this.cancelled) return;

        // Divide strings into array
        StringTokenizer lineTokenizer = new StringTokenizer(sifResult, "\n\r");
        List<String[]> edgeList = new ArrayList<>();
        while (lineTokenizer.hasMoreTokens()) {
            if (this.cancelled) return;
            String line = lineTokenizer.nextToken().trim();
            if (line.isEmpty()) continue;
            StringTokenizer fieldTokenizer = new StringTokenizer(line, " \t");
            
            int count = fieldTokenizer.countTokens();
            String[] row = new String[count];
            for (int i = 0; i < count; i++) {
                if (i == 0) row[0] = fieldTokenizer.nextToken();
                else if (i == 1) fieldTokenizer.nextToken();
                else if (i == 2) row[1] = fieldTokenizer.nextToken();
            }

            edgeList.add(row);
        }

        taskMonitor.setStatusMessage("Export complete.");
        if (this.cancelled) return;

        SwingUtilities.invokeLater(() -> {
            String[] columnNames = {"Source", "Target"};
            String[][] data = edgeList.toArray(new String[0][]);
            JTable table = new JTable(data, columnNames);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(null, scrollPane, "SIF Array Data Viewer", JOptionPane.PLAIN_MESSAGE);
        });
    }
}