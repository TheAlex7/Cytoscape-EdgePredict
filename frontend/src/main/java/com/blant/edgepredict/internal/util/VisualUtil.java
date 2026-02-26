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

        // 1. Get the current visual style or create a new one based on it
        VisualStyle currentStyle = vmm.getVisualStyle(view);
        VisualStyle style = null;
        for (VisualStyle s : vmm.getAllVisualStyles()) {
            if (s.getTitle().equals("BLANT Style")) {
                style = s;
                break;
            }
        }

        // If the style doesn't exist, create it based on the current style
        if (style == null) {
            style = vsFactory.createVisualStyle(currentStyle); 
            style.setTitle("BLANT Style");
            vmm.addVisualStyle(style);
        }

        // 2. Determine the correct column name for edge types (try "interaction" first, then "interaction_type")
        String colName = "interaction";
        if (view.getModel().getDefaultEdgeTable().getColumn(colName) == null) {
            colName = "interaction_type";
        }
        // If neither column exists, we can't apply the mapping, so we return early
        if (view.getModel().getDefaultEdgeTable().getColumn(colName) == null) return;

        // 3. Create a discrete mapping for edge colors based on the interaction type
        DiscreteMapping<String, Paint> colorMapping = (DiscreteMapping<String, Paint>) 
            vmf.createVisualMappingFunction(colName, String.class, BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
        
        // Set colors for edges (case-insensitive)
        colorMapping.putMapValue("predicted", Color.GREEN);
        colorMapping.putMapValue("Predicted", Color.GREEN);
        style.addVisualMappingFunction(colorMapping);
        
        // Make edges thick so they are visible against nodes
        style.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 4.0);

        // 4. Set the style to the view and refresh
        vmm.setVisualStyle(style, view);
        style.apply(view);
        view.updateView();
    }
}