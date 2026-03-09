package com.blant.edgepredict.internal.util;

import java.awt.Color;
import java.awt.Paint;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import org.cytoscape.view.vizmap.*;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

public class VisualUtil {

    public static void applyStyles(
            CyNetworkView view,
            VisualMappingManager vmm,
            VisualMappingFunctionFactory vmf,
            VisualStyleFactory vsFactory) {

        if (view == null || vmm == null || vmf == null || vsFactory == null) {
            return;
        }

        VisualStyle style = null;

        for (VisualStyle s : vmm.getAllVisualStyles()) {
            if (s.getTitle().equals("BLANT Style")) {
                style = s;
                break;
            }
        }

        if (style == null) {
            style = vsFactory.createVisualStyle("BLANT Style");
            vmm.addVisualStyle(style);
        }

        String colName = "interaction";

        if (view.getModel().getDefaultEdgeTable().getColumn(colName) == null) {
            return;
        }

        DiscreteMapping<String, Paint> edgeColor =
                (DiscreteMapping<String, Paint>) vmf.createVisualMappingFunction(
                        colName,
                        String.class,
                        BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);

        edgeColor.putMapValue("predicted", Color.GREEN);
        edgeColor.putMapValue("Predicted", Color.GREEN);

        style.addVisualMappingFunction(edgeColor);

        DiscreteMapping<String, Paint> edgePaint =
                (DiscreteMapping<String, Paint>) vmf.createVisualMappingFunction(
                        colName,
                        String.class,
                        BasicVisualLexicon.EDGE_PAINT);

        edgePaint.putMapValue("predicted", Color.GREEN);
        edgePaint.putMapValue("Predicted", Color.GREEN);

        style.addVisualMappingFunction(edgePaint);

        style.setDefaultValue(BasicVisualLexicon.EDGE_WIDTH, 4.0);

        vmm.setVisualStyle(style, view);

        style.apply(view);

        view.updateView();
    }
}
