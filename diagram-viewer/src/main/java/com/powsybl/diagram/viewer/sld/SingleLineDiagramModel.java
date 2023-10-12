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
import com.powsybl.sld.cgmes.dl.iidm.extensions.NetworkDiagramData;
import com.powsybl.sld.cgmes.layout.CgmesSubstationLayoutFactory;
import com.powsybl.sld.cgmes.layout.CgmesVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.*;

import com.powsybl.sld.layout.positionbyclustering.PositionByClustering;
import com.powsybl.sld.layout.positionfromextension.PositionFromExtension;
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

    private static final String SMART_VOLTAGELEVEL_LAYOUT = "Smart";
    private static final String AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT = "Auto extensions";
    private static final String AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT = "Auto without extensions Clustering";
    private static final String RANDOM_VOLTAGELEVEL_LAYOUT = "Random";
    private static final String CGMES_VOLTAGELEVEL_LAYOUT = "CGMES";

    // VoltageLevel layout provider
    private final Map<String, VoltageLevelLayoutFactory> nameToVoltageLevelLayoutFactoryMap = new TreeMap<>(); // ordered
    private final Map<String, VoltageLevelLayoutFactoryCreator> nameToVoltageLevelLayoutFactoryCreatorMap = new TreeMap<>(); // ordered
    private final ObservableList<VoltageLevelLayoutFactory> voltageLevelLayouts = FXCollections.observableArrayList();
    private final VoltageLevelLayoutFactoryBean voltageLevelLayoutFactory;

    private static final String HORIZONTAL_SUBSTATION_LAYOUT = "Horizontal";
    private static final String VERTICAL_SUBSTATION_LAYOUT = "Vertical";
    private static final String CGMES_SUBSTATION_LAYOUT = "CGMES";

    // Substation layout provider
    private final Map<String, SubstationLayoutFactory> nameToSubstationLayoutFactoryMap = new TreeMap<>(); // ordered
    private final ObservableList<SubstationLayoutFactory> substationLayouts = FXCollections.observableArrayList();
    private final ObjectProperty<SubstationLayoutFactory> currentSubstationLayoutFactory = new SimpleObjectProperty<>();

    // CGMES-DL names
    private final ObservableList<String> cgmesDLDiagramNames = FXCollections.observableArrayList();
    private final StringProperty currentCgmesDLDiagramName = new SimpleStringProperty();

    public SingleLineDiagramModel(// Providers
                                  ReadOnlyObjectProperty<ComponentLibrary> componentLibrary,
                                  ReadOnlyObjectProperty<VoltageLevelLayoutFactory> voltageLevelLayoutFactory,
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
                                  // PositionVoltageLevelLayoutFactory
                                  BooleanProperty stackFeeders,
                                  BooleanProperty exceptionWhenPatternUnhandled,
                                  BooleanProperty handleShunts,
                                  BooleanProperty removeFictitiousNodes,
                                  BooleanProperty substituteSingularFictitiousNodes,
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
        this.voltageLevelLayoutFactory = new VoltageLevelLayoutFactoryBean(voltageLevelLayoutFactory,
                stackFeeders,
                exceptionWhenPatternUnhandled,
                handleShunts,
                removeFictitiousNodes,
                substituteSingularFictitiousNodes);
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
                angleLabel,
                addNodesInfos,
                feederInfoSymmetry,
                feederInfosOuterMargin,
                feederInfosIntraMargin);
    }

    public void initProviders() {
        // VoltageLevelLayouts
        nameToVoltageLevelLayoutFactoryMap.put(AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionFromExtension()));
        nameToVoltageLevelLayoutFactoryCreatorMap.put(AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT, VoltageLevelLayoutFactoryCreator.newPositionVoltageLevelLayoutFactoryCreator());

        nameToVoltageLevelLayoutFactoryMap.put(AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionByClustering()));
        nameToVoltageLevelLayoutFactoryCreatorMap.put(AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT, VoltageLevelLayoutFactoryCreator.newPositionVoltageLevelLayoutFactoryCreator(new PositionByClustering()));

        nameToVoltageLevelLayoutFactoryMap.put(RANDOM_VOLTAGELEVEL_LAYOUT, new RandomVoltageLevelLayoutFactory(500, 500));
        nameToVoltageLevelLayoutFactoryCreatorMap.put(RANDOM_VOLTAGELEVEL_LAYOUT, i -> new RandomVoltageLevelLayoutFactory(500, 500));
        // SubstationLayouts
        nameToSubstationLayoutFactoryMap.put(HORIZONTAL_SUBSTATION_LAYOUT, new HorizontalSubstationLayoutFactory());
        nameToSubstationLayoutFactoryMap.put(VERTICAL_SUBSTATION_LAYOUT, new VerticalSubstationLayoutFactory());

        // Set all providers list
        componentLibraries.setAll(ComponentLibrary.findAll());
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
        voltageLevelLayouts.setAll(nameToVoltageLevelLayoutFactoryMap.values());
    }

    public void updateFrom(Network network) {
        if (network != null) {
            // VoltageLevelLayouts
            nameToVoltageLevelLayoutFactoryMap.put(SMART_VOLTAGELEVEL_LAYOUT, new SmartVoltageLevelLayoutFactory(network));
            nameToVoltageLevelLayoutFactoryCreatorMap.put(SMART_VOLTAGELEVEL_LAYOUT, VoltageLevelLayoutFactoryCreator.newSmartVoltageLevelLayoutFactoryCreator());

            nameToVoltageLevelLayoutFactoryMap.put(CGMES_VOLTAGELEVEL_LAYOUT, new CgmesVoltageLevelLayoutFactory(network));
            nameToVoltageLevelLayoutFactoryCreatorMap.put(CGMES_VOLTAGELEVEL_LAYOUT, CgmesVoltageLevelLayoutFactory::new);
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
        voltageLevelLayouts.setAll(nameToVoltageLevelLayoutFactoryMap.values());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        voltageLevelLayoutFactory.addListener(changeListener);
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

    public VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        return voltageLevelLayoutFactory.getVoltageLevelLayoutFactory();
    }

    public VoltageLevelLayoutFactoryCreator getVoltageLevelLayoutFactoryCreator() {
        String voltageLevelFactoryCreatorKey = nameToVoltageLevelLayoutFactoryMap.keySet().stream().filter(name -> nameToVoltageLevelLayoutFactoryMap.get(name) == getVoltageLevelLayoutFactory()).findFirst().orElse(SMART_VOLTAGELEVEL_LAYOUT);
        return nameToVoltageLevelLayoutFactoryCreatorMap.get(voltageLevelFactoryCreatorKey);
    }

    public SubstationLayoutFactory getSubstationLayoutFactory() {
        return currentSubstationLayoutFactory.get();
    }

    public ObservableList<ComponentLibrary> getComponentLibraries() {
        return componentLibraries;
    }

    public ObservableList<VoltageLevelLayoutFactory> getVoltageLevelLayouts() {
        return voltageLevelLayouts;
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

    public StringConverter<VoltageLevelLayoutFactory> getVoltageLevelLayoutFactoryStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(VoltageLevelLayoutFactory object) {
                Optional<String> label = nameToVoltageLevelLayoutFactoryMap.keySet().stream().filter(name -> nameToVoltageLevelLayoutFactoryMap.get(name) == object).findFirst();
                return label.orElse(UNKNOWN_ITEM);
            }

            @Override
            public VoltageLevelLayoutFactory fromString(String item) {
                return nameToVoltageLevelLayoutFactoryMap.get(item);
            }
        };
    }
}
