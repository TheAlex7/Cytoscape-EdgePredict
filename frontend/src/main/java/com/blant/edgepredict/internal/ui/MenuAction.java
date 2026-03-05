package com.blant.edgepredict.internal.ui;

import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;


import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.blant.edgepredict.internal.util.BlantConfig;


// This class creates the menu item and opens the dashboard when clicked
public class MenuAction extends AbstractCyAction {

    private final CyApplicationManager appManager;
    private final TaskManager taskManager;
    private final CyNetworkViewWriterManager writerManager;
    private final FileUtil fileUtil;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmf;
    private final VisualStyleFactory vsFactory;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;

    

    public MenuAction(CyApplicationManager appManager,
                      TaskManager taskManager,
                      CyNetworkViewWriterManager writerManager,
                      FileUtil fileUtil,
                      VisualMappingManager vmm,
                      VisualMappingFunctionFactory vmf,
                      VisualStyleFactory vsFactory,
                      CyNetworkFactory networkFactory,
                      CyNetworkManager networkManager,
                      CyNetworkViewFactory networkViewFactory,
                      CyNetworkViewManager networkViewManager) {
        super("BLANT Prediction Dashboard");
        this.appManager = appManager;
        this.taskManager = taskManager;
        this.writerManager = writerManager;
        this.fileUtil = fileUtil;
        this.vmm = vmm;
        this.vmf = vmf;
        this.vsFactory = vsFactory;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        setPreferredMenu("Apps");
    }

    // This method is called when the menu item is clicked, and it opens the NavDashboard
    @Override
    public void actionPerformed(ActionEvent e) {
        // Start the BLANT process in a separate thread to avoid blocking the UI
        new Thread(() -> {
        try {

            URL url = new URL("http://localhost:55161/blant");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            BlantConfig.setJobId(sb.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }).start();

        NavDashboard dashboard = new NavDashboard(taskManager, appManager, writerManager, fileUtil, vmm, vmf, vsFactory,
                networkFactory, networkManager, networkViewFactory, networkViewManager);
        dashboard.setVisible(true);
    }
}