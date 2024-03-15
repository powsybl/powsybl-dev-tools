/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.diagram.viewer.common.DiagramModel;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.cgmes.dl.iidm.extensions.*;
import com.powsybl.sld.cgmes.layout.*;
import com.powsybl.sld.layout.*;

import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.styles.*;
import com.powsybl.sld.svg.styles.iidm.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SingleLineDiagramModel extends DiagramModel {

    public enum VoltageLevelLayoutFactoryType {
        SMART, POSITION_WITH_EXTENSIONS, POSITION_BY_CLUSTERING, CGMES, RANDOM;

        @Override
        public String toString() {
            return switch (this) {
                case SMART -> "'Smart' choice";
                case POSITION_WITH_EXTENSIONS -> "Position with extensions";
                case POSITION_BY_CLUSTERING -> "Position by clustering";
                case RANDOM -> "Random";
                case CGMES -> "CGMES";
            };
        }
    }

    private static final String UNKNOWN_ITEM = "???";

    // LayoutParameters
    private final LayoutParametersBean layoutParametersBean;

    // SvgParameters
    private final SvgParametersBean svgParametersBean;

    // Component library provider
    private final ObservableList<ComponentLibrary> componentLibraries = FXCollections.observableArrayList();
    private final ObjectProperty<ComponentLibrary> currentComponentLibrary = new SimpleObjectProperty<>();

    // Style provider
    private final BooleanProperty basicStyleProvider = new SimpleBooleanProperty();
    private final BooleanProperty nominalStyleProvider = new SimpleBooleanProperty();
    private final BooleanProperty animatedStyleProvider = new SimpleBooleanProperty();
    private final Property<Double> animationThreshold1 = new SimpleObjectProperty<>();
    private final Property<Double> animationThreshold2 = new SimpleObjectProperty<>();
    private final BooleanProperty highlightStyleProvider = new SimpleBooleanProperty();
    private final BooleanProperty topologicalStyleProvider = new SimpleBooleanProperty();

    // Substation layout provider
    private static final String HORIZONTAL_SUBSTATION_LAYOUT = "Horizontal";
    private static final String VERTICAL_SUBSTATION_LAYOUT = "Vertical";
    private static final String CGMES_SUBSTATION_LAYOUT = "CGMES";
    private final Map<String, SubstationLayoutFactory> nameToSubstationLayoutFactoryMap = new TreeMap<>(); // ordered
    private final ObservableList<SubstationLayoutFactory> substationLayouts = FXCollections.observableArrayList();
    private final ObjectProperty<SubstationLayoutFactory> currentSubstationLayoutFactory = new SimpleObjectProperty<>();

    // CGMES-DL names
    private final ObservableList<String> cgmesDLDiagramNames = FXCollections.observableArrayList();
    private final StringProperty currentCgmesDLDiagramName = new SimpleStringProperty();

    public SingleLineDiagramModel(// Providers
                                  ReadOnlyObjectProperty<ComponentLibrary> componentLibrary,
                                  ReadOnlyObjectProperty<SubstationLayoutFactory> substationLayoutFactory,
                                  ReadOnlyObjectProperty<String> cgmesDLDiagramName,
                                  // Styles
                                  BooleanProperty basicStyleProvider,
                                  BooleanProperty nominalStyleProvider,
                                  BooleanProperty animatedStyleProvider,
                                  Property<Double> animationThreshold1,
                                  Property<Double> animationThreshold2,
                                  BooleanProperty highlightStyleProvider,
                                  BooleanProperty topologicalStyleProvider,
                                  // LayoutParameters
                                  Property<Double> diagramPaddingTopBottom,
                                  Property<Double> diagramPaddingLeftRight,
                                  Property<Double> voltagePaddingTopBottom,
                                  Property<Double> voltagePaddingLeftRight,
                                  Property<Double> busbarVerticalSpace,
                                  Property<Double> busbarHorizontalSpace,
                                  Property<Double> cellWidth,
                                  Property<Double> externCellHeight,
                                  Property<Double> internCellHeight,
                                  Property<Double> stackHeight,
                                  BooleanProperty disconnectorsOnBus,
                                  Property<Double> scaleFactor,
                                  BooleanProperty adaptCellHeightToContent,
                                  Property<Double> minSpaceBetweenComponents,
                                  Property<Double> minimumExternCellHeight,
                                  Property<LayoutParameters.Alignment> busBarAlignment,
                                  Property<Double> spaceForFeederInfos,
                                  //SvgParameters
                                  BooleanProperty showGrid,
                                  BooleanProperty showInternalNodes,
                                  BooleanProperty drawStraightWires,
                                  BooleanProperty centerLabel,
                                  BooleanProperty labelDiagonal,
                                  BooleanProperty displayEquipmentNodesLabel,
                                  BooleanProperty displayConnectivityNodesId,
                                  Property<Double> angleLabel,
                                  BooleanProperty addNodesInfos,
                                  BooleanProperty feederInfoSymmetry,
                                  BooleanProperty avoidSVGComponentsDuplication,
                                  Property<Double> feederInfosOuterMargin,
                                  Property<Double> feederInfosIntraMargin
    ) {
        // Update providers observable lists
        initProviders();

        // Providers
        this.currentComponentLibrary.bind(componentLibrary);
        this.currentSubstationLayoutFactory.bind(substationLayoutFactory);
        this.currentCgmesDLDiagramName.bind(cgmesDLDiagramName);

        // Styles
        this.basicStyleProvider.bindBidirectional(basicStyleProvider);
        this.nominalStyleProvider.bindBidirectional(nominalStyleProvider);
        this.animatedStyleProvider.bindBidirectional(animatedStyleProvider);
        this.animationThreshold1.bindBidirectional(animationThreshold1);
        this.animationThreshold2.bindBidirectional(animationThreshold2);
        this.highlightStyleProvider.bindBidirectional(highlightStyleProvider);
        this.topologicalStyleProvider.bindBidirectional(topologicalStyleProvider);

        // LayoutParameters
        this.layoutParametersBean = new LayoutParametersBean(diagramPaddingTopBottom,
                diagramPaddingLeftRight,
                voltagePaddingTopBottom,
                voltagePaddingLeftRight,
                busbarVerticalSpace,
                busbarHorizontalSpace,
                cellWidth,
                externCellHeight,
                internCellHeight,
                stackHeight,
                disconnectorsOnBus,
                scaleFactor,
                adaptCellHeightToContent,
                minSpaceBetweenComponents,
                minimumExternCellHeight,
                busBarAlignment,
                spaceForFeederInfos);
        // SvgParameters
        this.svgParametersBean = new SvgParametersBean(showGrid,
                showInternalNodes,
                drawStraightWires,
                avoidSVGComponentsDuplication,
                centerLabel,
                labelDiagonal,
                displayEquipmentNodesLabel,
                displayConnectivityNodesId,
                angleLabel,
                addNodesInfos,
                feederInfoSymmetry,
                feederInfosOuterMargin,
                feederInfosIntraMargin);
    }

    public void initProviders() {
        // SubstationLayouts
        nameToSubstationLayoutFactoryMap.put(HORIZONTAL_SUBSTATION_LAYOUT, new HorizontalSubstationLayoutFactory());
        nameToSubstationLayoutFactoryMap.put(VERTICAL_SUBSTATION_LAYOUT, new VerticalSubstationLayoutFactory());

        // Set all providers list
        componentLibraries.setAll(ComponentLibrary.findAll());
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
    }

    public void updateFrom(Network network) {
        if (network != null) {
            // SubstationLayouts
            nameToSubstationLayoutFactoryMap.put(CGMES_SUBSTATION_LAYOUT, new CgmesSubstationLayoutFactory(network));
            // CGMES-DL names
            if (NetworkDiagramData.checkNetworkDiagramData(network)) {
                cgmesDLDiagramNames.setAll(NetworkDiagramData.getDiagramsNames(network));
            } else {
                cgmesDLDiagramNames.clear();
            }
        }

        // Set all providers list
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        layoutParametersBean.addListener(changeListener);
        svgParametersBean.addListener(changeListener);
    }

    public LayoutParameters getLayoutParameters() {
        return layoutParametersBean.getLayoutParameters();
    }

    public SvgParameters getSvgParameters() {
        return svgParametersBean.getSvgParameters(currentCgmesDLDiagramName.get());
    }

    public SvgParametersBean getSvgParametersBean() {
        return svgParametersBean;
    }

    public ComponentLibrary getComponentLibrary() {
        return currentComponentLibrary.getValue();
    }

    public StyleProvider getStyleProvider(Network network) {
        List<StyleProvider> styles = new ArrayList<>();
        if (this.basicStyleProvider.get()) {
            styles.add(new BasicStyleProvider());
        }
        if (this.nominalStyleProvider.get()) {
            styles.add(new NominalVoltageStyleProvider());
        }
        if (this.animatedStyleProvider.get()) {
            styles.add(new AnimatedFeederInfoStyleProvider(this.animationThreshold1.getValue(), this.animationThreshold2.getValue()));
        }
        if (this.highlightStyleProvider.get()) {
            styles.add(new HighlightLineStateStyleProvider(network));
        }
        if (this.topologicalStyleProvider.get()) {
            styles.add(new TopologicalStyleProvider(network));
        }
        if (styles.isEmpty()) {
            styles.add(new EmptyStyleProvider());
        }
        return new StyleProvidersList(styles);
    }

    public SubstationLayoutFactory getSubstationLayoutFactory() {
        return currentSubstationLayoutFactory.get();
    }

    public ObservableList<ComponentLibrary> getComponentLibraries() {
        return componentLibraries;
    }

    public ObservableList<SubstationLayoutFactory> getSubstationLayouts() {
        return substationLayouts;
    }

    public ObservableList<String> getCgmesDLDiagramNames() {
        return cgmesDLDiagramNames;
    }

    StringConverter<ComponentLibrary> getComponentLibraryStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(ComponentLibrary value) {
                return value != null ? value.getName() : UNKNOWN_ITEM;
            }

            @Override
            public ComponentLibrary fromString(String label) {
                return componentLibraries.stream().filter(c -> c.getName().compareTo(label) == 0).findAny().orElse(null);
            }
        };
    }

    public StringConverter<SubstationLayoutFactory> getSubstationLayoutStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(SubstationLayoutFactory object) {
                Optional<String> label = nameToSubstationLayoutFactoryMap.keySet().stream().filter(name -> nameToSubstationLayoutFactoryMap.get(name) == object).findFirst();
                return label.orElse(UNKNOWN_ITEM);
            }

            @Override
            public SubstationLayoutFactory fromString(String item) {
                return nameToSubstationLayoutFactoryMap.get(item);
            }
        };
    }
}
