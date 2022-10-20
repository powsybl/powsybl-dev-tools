/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.nad.viewer.controller;

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
    private final BooleanProperty infoAlongEdge = new SimpleBooleanProperty();
    private final BooleanProperty textNodesIncluded = new SimpleBooleanProperty();
    private final DoubleProperty springRepulsionFactor = new SimpleDoubleProperty();
    private final IntegerProperty depth = new SimpleIntegerProperty();
    private final StringProperty svgContent = new SimpleStringProperty();
    private final StringProperty labelProvider = new SimpleStringProperty();
    private final StringProperty layoutFactory = new SimpleStringProperty();

    private final Map<Container<?>, StringProperty> containerToSvgMap = new HashMap<>();

    public Model(ReadOnlyObjectProperty<Integer> depth, ObjectProperty<String> layout, ObjectProperty<String> label,
                 BooleanProperty textNodesIncluded, ReadOnlyObjectProperty<Double> springRepulsionFactor, BooleanProperty infoAlongEdge, BooleanProperty showNames) {
        this.depth.bind(depth);
        this.layoutFactory.bind(layout);
        this.labelProvider.bind(label);
        this.textNodesIncluded.bind(textNodesIncluded);
        this.springRepulsionFactor.bind(springRepulsionFactor);
        this.infoAlongEdge.bind(infoAlongEdge);
        this.showNames.bind(showNames);
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
        return new SvgParameters()
                .setSvgWidthAndHeightAdded(true)
                .setEdgeInfoAlongEdge(infoAlongEdge.get());
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
