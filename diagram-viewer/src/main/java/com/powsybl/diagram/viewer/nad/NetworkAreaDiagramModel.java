/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.nad;

import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.nad.layout.BasicForceLayoutFactory;
import com.powsybl.nad.layout.LayoutFactory;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.LabelProvider;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class NetworkAreaDiagramModel {
    private static final String DEFAULT_LABEL_PROVIDER = "Default";
    private static final String BASIC_LAYOUT = "Basic";
    private static final String TOPOLOGICAL_STYLE_PROVIDER = "Topological";

    private final StringProperty svgContent = new SimpleStringProperty();

    private final Map<Container<?>, StringProperty> containerToSvgMap = new HashMap<>();

    // Layout Parameters
    private final LayoutParametersBean layoutParameters;

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
                                   Property<Double> fixedSize
    ) {
        this.depth.bind(depth);
        this.labelProvider.bind(label);
        this.styleProvider.bind(style);
        this.layoutFactory.bind(layout);

        // Layout parameters
        layoutParameters = new LayoutParametersBean(textNodesIncluded, springRepulsionFactor);

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
                fixedSize);
    }

    public int getDepth() {
        return depth.get();
    }

    public SvgParameters getSvgParameters() {
        return svgParameters.getSvgParameters();
    }

    public LayoutParameters getLayoutParameters() {
        return layoutParameters.getLayoutParameters();
    }

    public LabelProvider getLabelProvider(Network network) {
        return DEFAULT_LABEL_PROVIDER.equals(labelProvider.getValue()) ? new DefaultLabelProvider(network, getSvgParameters()) : null;
    }

    public StyleProvider getStyleProvider(Network network) {
        return TOPOLOGICAL_STYLE_PROVIDER.equals(styleProvider.getValue())
                ? new TopologicalStyleProvider(network)
                : new NominalVoltageStyleProvider(network);
    }

    public LayoutFactory getLayoutFactory() {
        return BASIC_LAYOUT.equals(layoutFactory.getValue()) ? new BasicForceLayoutFactory() : null;
    }

    public StringProperty getSvgContent() {
        return svgContent;
    }

    public StringProperty getCheckedSvgContent(Container<?> container) {
        return containerToSvgMap.computeIfAbsent(container, c -> new SimpleStringProperty());
    }

    public Stream<Container<?>> getCheckedContainerStream() {
        return containerToSvgMap.keySet().stream();
    }

    public void removeSvgContent(Container<?> container) {
        containerToSvgMap.remove(container);
    }

    public void clean() {
        svgContent.set("");
    }

    public SvgParametersBean getSvgParametersBean() {
        return svgParameters;
    }

    public void addListener(ChangeListener<Object> changeListener) {
        svgParameters.addListener(changeListener);
        layoutParameters.addListener(changeListener);
    }
}
