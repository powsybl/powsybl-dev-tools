/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer;

import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.nad.layout.BasicForceLayoutFactory;
import com.powsybl.nad.layout.LayoutFactory;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.LabelProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;
import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class Model {
    private static final String DEFAULT_LABEL_PROVIDER = "Default";
    private static final String BASIC_LAYOUT = "Basic";

    private final BooleanProperty showNames = new SimpleBooleanProperty();
    private final ObjectProperty<Network> network = new SimpleObjectProperty<>();
    private final ObjectProperty<Container<?>> selectedContainer = new SimpleObjectProperty<>();

    // Layout Parameters
    private final BooleanProperty textNodesIncluded = new SimpleBooleanProperty();
    private final DoubleProperty springRepulsionFactor = new SimpleDoubleProperty();

    // SVG Parameters
    private final BooleanProperty idDisplayed = new SimpleBooleanProperty();
    private final BooleanProperty infoAlongEdge = new SimpleBooleanProperty();
    private final BooleanProperty insertNameDesc = new SimpleBooleanProperty();
    private final BooleanProperty substationDescriptionDisplayed = new SimpleBooleanProperty();
    // Diagram size
    private final BooleanProperty widthHeightAdded = new SimpleBooleanProperty();
    private final ObjectProperty<SvgParameters.SizeConstraint> sizeConstraint = new SimpleObjectProperty<>(this, "sizeConstraint", SvgParameters.SizeConstraint.NONE);
    private final DoubleProperty fixedSize = new SimpleDoubleProperty();

    private final IntegerProperty depth = new SimpleIntegerProperty();
    private final StringProperty svgContent = new SimpleStringProperty();
    private final StringProperty labelProvider = new SimpleStringProperty();
    private final StringProperty layoutFactory = new SimpleStringProperty();

    private final Map<Container<?>, StringProperty> containerToSvgMap = new HashMap<>();

    public Model(ReadOnlyObjectProperty<Integer> depth,
                 ObjectProperty<String> label,
                 ObjectProperty<String> layout,
                 BooleanProperty showNames,

                 // Layout parameters
                 ReadOnlyObjectProperty<Double> springRepulsionFactor,
                 BooleanProperty textNodesIncluded,

                 // SVG parameters
                 BooleanProperty idDisplayed,
                 BooleanProperty infoAlongEdge,
                 BooleanProperty insertNameDesc,
                 BooleanProperty substationDescriptionDisplayed,
                 // Diagram size
                 BooleanProperty widthHeightAdded,
                 ReadOnlyObjectProperty<SvgParameters.SizeConstraint> sizeConstraint,
                 ReadOnlyObjectProperty<Double> fixedSize
    ) {
        this.depth.bind(depth);
        this.labelProvider.bind(label);
        this.layoutFactory.bind(layout);
        this.showNames.bind(showNames);

        this.springRepulsionFactor.bind(springRepulsionFactor);
        this.textNodesIncluded.bind(textNodesIncluded);

        // SVG parameters
        this.idDisplayed.bind(idDisplayed);
        this.infoAlongEdge.bind(infoAlongEdge);
        this.insertNameDesc.bind(insertNameDesc);
        this.substationDescriptionDisplayed.bind(substationDescriptionDisplayed);
        // Diagram size
        this.widthHeightAdded.bind(widthHeightAdded);
        this.sizeConstraint.bind(sizeConstraint);
        this.fixedSize.bind(fixedSize);
    }

    public void setNetwork(Network network) {
        this.network.setValue(network);
    }

    public Network getNetwork() {
        return network.getValue();
    }

    public ObjectProperty<Network> getNetworkProperty() {
        return network;
    }

    public StringProperty getSvgContent() {
        return svgContent;
    }

    public Container<?> getSelectedContainer() {
        return selectedContainer.getValue();
    }

    public int getDepth() {
        return depth.get();
    }

    public void setSvgContent(String svgContent) {
        this.svgContent.setValue(svgContent);
    }

    public SvgParameters getSvgParameters() {
        SvgParameters svgParameters = new SvgParameters()
                .setIdDisplayed(idDisplayed.get())
                .setInsertNameDesc(insertNameDesc.get())
                .setSubstationDescriptionDisplayed(substationDescriptionDisplayed.get())
                .setEdgeInfoAlongEdge(infoAlongEdge.get())
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

    public LabelProvider getLabelProvider() {
        return DEFAULT_LABEL_PROVIDER.equals(labelProvider.getValue()) ? new DefaultLabelProvider(network.getValue(), getSvgParameters()) : null;
    }

    public LayoutFactory getLayoutFactory() {
        return BASIC_LAYOUT.equals(layoutFactory.getValue()) ? new BasicForceLayoutFactory() : null;
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

    public void setSelectedContainer(Container<?> container) {
        selectedContainer.setValue(container);
    }
}
