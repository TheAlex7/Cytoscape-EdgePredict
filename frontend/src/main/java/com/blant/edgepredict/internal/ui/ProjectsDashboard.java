package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;

import com.blant.edgepredict.internal.task.ImportGraph;
import com.blant.edgepredict.internal.util.BlantConfig;
import com.blant.edgepredict.internal.util.CacheUtil;
import com.blant.edgepredict.internal.util.ProjectStore;

public class ProjectsDashboard extends JFrame {
    private static ProjectsDashboard instance;

    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final CyNetworkViewFactory networkViewFactory;
    private final CyNetworkViewManager networkViewManager;
    private final CyLayoutAlgorithmManager layoutManager;
    private final VisualMappingManager vmm;
    private final VisualMappingFunctionFactory vmfDiscrete;
    private final VisualMappingFunctionFactory vmfPassthrough;
    private final VisualStyleFactory vsFactory;

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> projectList;

    private ProjectsDashboard(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory,
            CyNetworkViewManager networkViewManager,
            CyLayoutAlgorithmManager layoutManager,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory vmfDiscrete,
            VisualMappingFunctionFactory vmfPassthrough,
            VisualStyleFactory vsFactory) {
        super("BLANT Projects");
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.networkViewFactory = networkViewFactory;
        this.networkViewManager = networkViewManager;
        this.layoutManager = layoutManager;
        this.vmm = vmm;
        this.vmfDiscrete = vmfDiscrete;
        this.vmfPassthrough = vmfPassthrough;
        this.vsFactory = vsFactory;

        projectList = new JList<>(listModel);
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(projectList);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Saved Projects"));

        JButton loadBtn = new JButton("Load Project");
        JButton deleteBtn = new JButton("Delete Project");
        JButton renameBtn = new JButton("Rename Project");
        JButton refreshBtn = new JButton("Load All Projects");

        loadBtn.addActionListener(e -> loadSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        renameBtn.addActionListener(e -> renameSelected());
        refreshBtn.addActionListener(e -> refresh());

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        buttonPanel.add(loadBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(renameBtn);
        buttonPanel.add(refreshBtn);

        JLabel hint = new JLabel("<html><i>Select a project then choose an action.</i></html>");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.EAST);
        content.add(hint, BorderLayout.SOUTH);

        setContentPane(content);
        refresh();
        pack();
        setLocationRelativeTo((Component) null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private void refresh() {
        listModel.clear();
        Map<String, String> projects = ProjectStore.getProjects();
        for (String name : projects.keySet()) {
            listModel.addElement(name);
        }
    }

    private String selectedProject() {
        String selected = projectList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a project first.");
        }
        return selected;
    }

    private void loadSelected() {
        String name = selectedProject();
        if (name == null) return;

        Map<String, String> projects = ProjectStore.getProjects();
        String jobId = projects.get(name);
        if (jobId == null) {
            JOptionPane.showMessageDialog(this, "Project data not found.");
            return;
        }

        String output = CacheUtil.getOutput(jobId);
        if (output.startsWith("[ERROR]")) {
            JOptionPane.showMessageDialog(this, "Saved result file not found for \"" + name + "\".\nThe output may have been deleted.");
            return;
        }

        BlantConfig.setJobId(jobId);
        BlantConfig.setLoad(true);
        BlantConfig.setInputFile(null);

        BlantLogWindow logWindow = BlantLogWindow.getInstance();
        logWindow.appendLog("[INFO] Loading project: " + name + " (job: " + jobId + ")");
        logWindow.setVisible(true);

        new Thread(() -> {
            try {
                new ImportGraph(networkFactory, networkManager, networkViewFactory, networkViewManager,
                        layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory, false, logWindow)
                        .importFile();
            } catch (Exception ex) {
                logWindow.appendLog("[ERROR] Failed to load project: " + ex.getMessage());
            } finally {
                BlantConfig.setLoad(false);
            }
        }).start();
    }

    private void deleteSelected() {
        String name = selectedProject();
        if (name == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete project \"" + name + "\" and its saved files?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ProjectStore.deleteProject(name);
            refresh();
        }
    }

    private void renameSelected() {
        String name = selectedProject();
        if (name == null) return;

        JTextField field = new JTextField(name, 25);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("New name for \"" + name + "\":"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Rename Project",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;
        String newName = field.getText().trim();
        if (newName.isEmpty() || newName.equals(name)) return;

        if (ProjectStore.getProjects().containsKey(newName)) {
            JOptionPane.showMessageDialog(this, "A project named \"" + newName + "\" already exists.");
            return;
        }

        ProjectStore.renameProject(name, newName);
        refresh();
    }

    public static ProjectsDashboard getInstance(
            CyNetworkFactory networkFactory,
            CyNetworkManager networkManager,
            CyNetworkViewFactory networkViewFactory,
            CyNetworkViewManager networkViewManager,
            CyLayoutAlgorithmManager layoutManager,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory vmfDiscrete,
            VisualMappingFunctionFactory vmfPassthrough,
            VisualStyleFactory vsFactory) {
        if (instance == null || !instance.isDisplayable()) {
            instance = new ProjectsDashboard(networkFactory, networkManager, networkViewFactory,
                    networkViewManager, layoutManager, vmm, vmfDiscrete, vmfPassthrough, vsFactory);
        } else {
            instance.refresh();
        }
        SwingUtilities.invokeLater(() -> instance.setVisible(true));
        return instance;
    }
}
