/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.diagram.viewer.common.DiagramModel;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.layout.*;
import com.powsybl.nad.model.*;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

import java.util.*;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramModel extends DiagramModel {
    private static final String DEFAULT_LABEL_PROVIDER = "Default";
    private static final String FORCE_LAYOUT = "Force layout";
    private static final String FIXED_LAYOUT = "Fixed";
    private static final String GEOGRAPHICAL_LAYOUT = "Geographical";
    private static final String TOPOLOGICAL_STYLE_PROVIDER = "Topological";

    // Layout Parameters
    private final LayoutParametersBean layoutParametersBean;

    // SVG Parameters
    private final SvgParametersBean svgParameters;

    private final IntegerProperty depth = new SimpleIntegerProperty();
    private final StringProperty labelProvider = new SimpleStringProperty();
    private final StringProperty styleProvider = new SimpleStringProperty();
    private final StringProperty layoutFactory = new SimpleStringProperty();

    public NetworkAreaDiagramModel(ReadOnlyObjectProperty<Integer> depth,
                                   ObjectProperty<String> label,
                                   ObjectProperty<String> style,
                                   ObjectProperty<String> layout,

                                   // Layout parameters
                                   Property<Double> springRepulsionFactor,
                                   Property<Integer> nbMaxSteps,
                                   BooleanProperty textNodesIncluded,

                                   // SVG parameters
                                   BooleanProperty infoAlongEdge,
                                   BooleanProperty edgeNameDisplayed,
                                   BooleanProperty insertNameDesc,
                                   BooleanProperty substationDescriptionDisplayed,
                                   BooleanProperty busLegend,
                                   BooleanProperty vlDetails,
                                   // Diagram size
                                   BooleanProperty widthHeightAdded,
                                   Property<SvgParameters.SizeConstraint> sizeConstraint,
                                   Property<Integer> fixedSize,
                                   Property<Double> fixedScale
    ) {
        this.depth.bind(depth);
        this.labelProvider.bind(label);
        this.styleProvider.bind(style);
        this.layoutFactory.bind(layout);

        // Layout parameters
        layoutParametersBean = new LayoutParametersBean(textNodesIncluded, springRepulsionFactor, nbMaxSteps);

        // SVG parameters
        svgParameters = new SvgParametersBean(infoAlongEdge,
                edgeNameDisplayed,
                insertNameDesc,
                substationDescriptionDisplayed,
                busLegend,
                vlDetails,
                // Diagram size
                widthHeightAdded,
                sizeConstraint,
                fixedSize,
                fixedScale);
    }

    public int getDepth() {
        return depth.get();
    }

    public SvgParameters getSvgParameters() {
        return svgParameters.getSvgParameters();
    }

    public LayoutParameters getLayoutParameters() {
        return layoutParametersBean.getLayoutParameters();
    }

    public LabelProviderFactory getLabelProviderFactory() {
        return DEFAULT_LABEL_PROVIDER.equals(labelProvider.getValue())
                ? DefaultLabelProvider::new
                : null;
    }

    public StyleProviderFactory getStyleProviderFactory() {
        return TOPOLOGICAL_STYLE_PROVIDER.equals(styleProvider.getValue())
                ? TopologicalStyleProvider::new
                : NominalVoltageStyleProvider::new;
    }

    public LayoutFactory getLayoutFactory(Network network) {
        // FIXME : user must be able to customize positions
        Map<String, Point> initialPositions = new HashMap<>();
        return switch (layoutFactory.getValue()) {
            case FORCE_LAYOUT -> new BasicForceLayoutFactory();
            case FIXED_LAYOUT -> new FixedLayoutFactory(initialPositions);
            case GEOGRAPHICAL_LAYOUT -> new GeographicalLayoutFactory(network);
            default -> null;
        };
    }

    public SvgParametersBean getSvgParametersBean() {
        return svgParameters;
    }

    public void addListener(ChangeListener<Object> changeListener) {
        svgParameters.addListener(changeListener);
        layoutParametersBean.addListener(changeListener);
    }
}
