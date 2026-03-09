package com.blant.edgepredict.internal.util;

import java.awt.Color;
import java.awt.Paint;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;


public class VisualUtil {
    public static void applyStyles(CyNetworkView view, VisualMappingManager vmm, VisualMappingFunctionFactory vmf, VisualStyleFactory vsFactory) {
        if (view == null || vmm == null || vmf == null || vsFactory == null) return;

        // Get the current visual style or create a new one based on it
        VisualStyle currentStyle = vmm.getVisualStyle(view);
        VisualStyle style = null;
        for (VisualStyle s : vmm.getAllVisualStyles()) {
            if (s.getTitle().equals("BLANT Style")) {
                style = s;
                break;
            }
        }

        if (style == null) {
            style = vsFactory.createVisualStyle(currentStyle);
            style.setTitle("BLANT Style");
            vmm.addVisualStyle(style);
        }

        // Determine the correct column name for edge types
        String colName = "interaction";
        if (view.getModel().getDefaultEdgeTable().getColumn(colName) == null) {
            colName = "interaction_type";
        }
        if (view.getModel().getDefaultEdgeTable().getColumn(colName) == null) return;

        // Map EDGE_STROKE_UNSELECTED_PAINT
        DiscreteMapping<String, Paint> strokeMapping = (DiscreteMapping<String, Paint>)
            vmf.createVisualMappingFunction(colName, String.class, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
        strokeMapping.putMapValue("predicted", Color.GREEN);
        strokeMapping.putMapValue("Predicted", Color.GREEN);
        style.addVisualMappingFunction(strokeMapping);

        // Map EDGE_PAINT
        DiscreteMapping<String, Paint> paintMapping = (DiscreteMapping<String, Paint>)
            vmf.createVisualMappingFunction(colName, String.class, BasicVisualLexicon.EDGE_PAINT);
        paintMapping.putMapValue("predicted", Color.GREEN);
        paintMapping.putMapValue("Predicted", Color.GREEN);
        style.addVisualMappingFunction(paintMapping);

        // Make edges thick so they are visible against nodes
        style.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 4.0);

        // Apply style to view and refresh
        vmm.setVisualStyle(style, view);
        style.apply(view);
        view.updateView();
    }
}