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
import com.powsybl.nad.svg.EdgeInfoEnum;
import com.powsybl.nad.svg.LabelProviderParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramModel extends DiagramModel {
    private static final String DEFAULT_LABEL_PROVIDER = "Default";
    private static final String FORCE_LAYOUT = "Force layout";
    static final String GEOGRAPHICAL_LAYOUT = "Geographical";
    private static final String TOPOLOGICAL_STYLE_PROVIDER = "Topological";

    // Layout Parameters
    private final LayoutParametersBean layoutParametersBean;

    // SVG Parameters
    private final SvgParametersBean svgParameters;

    // Label provider Parameters
    private final LabelProviderParametersBean labelProviderParametersBean;

    private final IntegerProperty depth = new SimpleIntegerProperty();
    private final IntegerProperty geoScalingFactor = new SimpleIntegerProperty();
    private final IntegerProperty geoRadiusFactor = new SimpleIntegerProperty();
    private final StringProperty labelProvider = new SimpleStringProperty();
    private final StringProperty styleProvider = new SimpleStringProperty();
    private final StringProperty layoutFactory = new SimpleStringProperty();

    public NetworkAreaDiagramModel(ReadOnlyObjectProperty<Integer> depth,
                                   ReadOnlyObjectProperty<Integer> geoScalingFactor,
                                   ReadOnlyObjectProperty<Integer> geoRadiusFactor,
                                   ObjectProperty<String> label,
                                   ObjectProperty<String> style,
                                   ObjectProperty<String> layout,

                                   // Layout parameters
                                   Property<Double> springRepulsionFactor,
                                   Property<Integer> nbMaxSteps,
                                   BooleanProperty textNodesIncluded,
                                   DoubleProperty scaleFactor,

                                   // SVG parameters
                                   BooleanProperty infoAlongEdge,
                                   BooleanProperty insertNameDesc,

                                   // Diagram size
                                   BooleanProperty widthHeightAdded,
                                   Property<SvgParameters.SizeConstraint> sizeConstraint,
                                   Property<Integer> fixedSize,
                                   Property<Double> fixedScale,

                                   // Label provider parameters
                                   BooleanProperty substationDescriptionDisplayed,
                                   BooleanProperty busLegend,
                                   BooleanProperty vlDetails,
                                   BooleanProperty doubleArrowsDisplayed,
                                   Property<EdgeInfoEnum> infoSideExternal,
                                   Property<EdgeInfoEnum> infoMiddleSide1,
                                   Property<EdgeInfoEnum> infoMiddleSide2,
                                   Property<EdgeInfoEnum> infoSideInternal) {
        this.depth.bind(depth);
        this.geoScalingFactor.bind(geoScalingFactor);
        this.geoRadiusFactor.bind(geoRadiusFactor);
        this.labelProvider.bind(label);
        this.styleProvider.bind(style);
        this.layoutFactory.bind(layout);

        // Layout parameters
        layoutParametersBean = new LayoutParametersBean(textNodesIncluded, springRepulsionFactor, nbMaxSteps, scaleFactor);

        // SVG parameters
        svgParameters = new SvgParametersBean(infoAlongEdge,
                insertNameDesc,
                // Diagram size
                widthHeightAdded,
                sizeConstraint,
                fixedSize,
                fixedScale);

        // Edge info parameters
        EdgeInfoParametersBean edgeInfoParameters = new EdgeInfoParametersBean(infoSideExternal,
            infoMiddleSide1,
            infoMiddleSide2,
            infoSideInternal);

        // Label provider Parameters
        labelProviderParametersBean = new LabelProviderParametersBean(substationDescriptionDisplayed,
            busLegend,
            vlDetails,
            doubleArrowsDisplayed,
            edgeInfoParameters);
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

    public LabelProviderParameters getLabelProviderParameters() {
        return labelProviderParametersBean.getLabelProviderParameters();
    }

    public LabelProviderFactory getLabelProviderFactory() {
        return DEFAULT_LABEL_PROVIDER.equals(labelProvider.getValue()) ?
            (Network network, SvgParameters svgParams) -> new DefaultLabelProvider(network,
                svgParams.createValueFormatter(),
                getLabelProviderParameters()) :
            null;
    }

    public StyleProviderFactory getStyleProviderFactory() {
        return TOPOLOGICAL_STYLE_PROVIDER.equals(styleProvider.getValue())
                ? TopologicalStyleProvider::new
                : NominalVoltageStyleProvider::new;
    }

    public LayoutFactory getLayoutFactory(Network network) {
        return switch (layoutFactory.getValue()) {
            case FORCE_LAYOUT -> new BasicForceLayoutFactory();
            case GEOGRAPHICAL_LAYOUT -> new GeographicalLayoutFactory(network, geoScalingFactor.getValue(), geoRadiusFactor.getValue(), BasicForceLayout::new);
            default -> null;
        };
    }

    public LabelProviderParametersBean getLabelProviderParametersBean() {
        return labelProviderParametersBean;
    }

    public void addListener(ChangeListener<Object> changeListener) {
        svgParameters.addListener(changeListener);
        layoutParametersBean.addListener(changeListener);
        labelProviderParametersBean.addListener(changeListener);
    }
}
