package com.blant.edgepredict.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ProjectNameDialog {

    private ProjectNameDialog() {}

    /**
     * Shows a modal dialog prompting the user to name a project.
     * Returns the trimmed name, or null if the user cancelled or left it blank.
     */
    public static String show(Component parent) {
        JTextField field = new JTextField(25);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        panel.add(new JLabel("Enter a name for this project:"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
            parent, panel, "Name Project",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return null;
        String name = field.getText().trim();
        return name.isEmpty() ? null : name;
    }
}
