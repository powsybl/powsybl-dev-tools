/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
    private final BooleanProperty textNodesIncluded = new SimpleBooleanProperty();
    private final DoubleProperty springRepulsionFactor = new SimpleDoubleProperty();

    // SVG Parameters
    private final BooleanProperty idDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty infoAlongEdge = new SimpleBooleanProperty();
    private final BooleanProperty edgeNameDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty insertNameDesc = new SimpleBooleanProperty();
    private final BooleanProperty substationDescriptionDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty busLegend = new SimpleBooleanProperty();
    private final BooleanProperty vlDetails = new SimpleBooleanProperty();
    // Diagram size
    private final BooleanProperty widthHeightAdded = new SimpleBooleanProperty();
    private final ObjectProperty<SvgParameters.SizeConstraint> sizeConstraint = new SimpleObjectProperty<>(this, "sizeConstraint", SvgParameters.SizeConstraint.NONE);
    private final DoubleProperty fixedSize = new SimpleDoubleProperty();

    private final IntegerProperty depth = new SimpleIntegerProperty();
    private final StringProperty labelProvider = new SimpleStringProperty();
    private final StringProperty styleProvider = new SimpleStringProperty();
    private final StringProperty layoutFactory = new SimpleStringProperty();

    public NetworkAreaDiagramModel(ReadOnlyObjectProperty<Integer> depth,
                                   ObjectProperty<String> label,
                                   ObjectProperty<String> style,
                                   ObjectProperty<String> layout,

                                   // Layout parameters
                                   ReadOnlyObjectProperty<Double> springRepulsionFactor,
                                   BooleanProperty textNodesIncluded,

                                   // SVG parameters
                                   BooleanProperty idDisplayed,
                                   BooleanProperty infoAlongEdge,
                                   BooleanProperty edgeNameDisplayed,
                                   BooleanProperty insertNameDesc,
                                   BooleanProperty substationDescriptionDisplayed,
                                   BooleanProperty busLegend,
                                   BooleanProperty vlDetails,
                                   // Diagram size
                                   BooleanProperty widthHeightAdded,
                                   ReadOnlyObjectProperty<SvgParameters.SizeConstraint> sizeConstraint,
                                   ReadOnlyObjectProperty<Double> fixedSize
    ) {
        this.depth.bind(depth);
        this.labelProvider.bind(label);
        this.styleProvider.bind(style);
        this.layoutFactory.bind(layout);

        this.springRepulsionFactor.bind(springRepulsionFactor);
        this.textNodesIncluded.bind(textNodesIncluded);

        // SVG parameters
        this.idDisplayed.bind(idDisplayed);
        this.infoAlongEdge.bind(infoAlongEdge);
        this.edgeNameDisplayed.bind(edgeNameDisplayed);
        this.insertNameDesc.bind(insertNameDesc);
        this.substationDescriptionDisplayed.bind(substationDescriptionDisplayed);
        this.busLegend.bind(busLegend);
        this.vlDetails.bind(vlDetails);
        // Diagram size
        this.widthHeightAdded.bind(widthHeightAdded);
        this.sizeConstraint.bind(sizeConstraint);
        this.fixedSize.bind(fixedSize);
    }

    public int getDepth() {
        return depth.get();
    }

    public SvgParameters getSvgParameters() {
        SvgParameters svgParameters = new SvgParameters()
                .setIdDisplayed(idDisplayed.get())
                .setInsertNameDesc(insertNameDesc.get())
                .setSubstationDescriptionDisplayed(substationDescriptionDisplayed.get())
                .setBusLegend(busLegend.get())
                .setVoltageLevelDetails(vlDetails.get())
                .setEdgeInfoAlongEdge(infoAlongEdge.get())
                .setEdgeNameDisplayed(edgeNameDisplayed.get())
                .setSvgWidthAndHeightAdded(widthHeightAdded.get())
                .setSizeConstraint(sizeConstraint.get());
        switch (sizeConstraint.get()) {
            case FIXED_HEIGHT:
                svgParameters.setFixedHeight((int) Math.round(fixedSize.get()));
                break;
            case FIXED_WIDTH:
                svgParameters.setFixedWidth((int) Math.round(fixedSize.get()));
                break;
            case FIXED_SCALE:
                svgParameters.setFixedScale(fixedSize.get());
                break;
            default:
                break;
        }
        return svgParameters;
    }

    public LayoutParameters getLayoutParameters() {
        return new LayoutParameters()
                .setTextNodesForceLayout(textNodesIncluded.get())
                .setSpringRepulsionFactorForceLayout(springRepulsionFactor.get());
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

    public void setSvgContent(String svgContent) {
        this.svgContent.setValue(svgContent);
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
}
