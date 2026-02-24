package com.blant.edgepredict.internal.ui;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;

public class MenuAction extends AbstractCyAction {

    private final CyApplicationManager appManager;
    private final TaskManager taskManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;

    public MenuAction(CyApplicationManager appManager,
                      TaskManager taskManager,
                      CyNetworkViewWriterManager writerManager,
                      FileUtil fileUtil) {

        super("Run Link Prediction");

        this.appManager = appManager;
        this.taskManager = taskManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;

        setPreferredMenu("Apps.BLANT");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NavDashboard dash = new NavDashboard(
                taskManager,
                appManager,
                writerManager,
                fileUtil
        );
        dash.setVisible(true);
        dash.toFront();
    }
}
