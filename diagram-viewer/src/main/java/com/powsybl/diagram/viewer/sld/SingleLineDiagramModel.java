/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.diagram.viewer.sld;

import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.Network;
import com.powsybl.sld.cgmes.dl.iidm.extensions.NetworkDiagramData;
import com.powsybl.sld.cgmes.layout.CgmesSubstationLayoutFactory;
import com.powsybl.sld.cgmes.layout.CgmesVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.*;

import com.powsybl.sld.layout.positionbyclustering.PositionByClustering;
import com.powsybl.sld.layout.positionfromextension.PositionFromExtension;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.BasicStyleProvider;
import com.powsybl.sld.svg.DiagramStyleProvider;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class SingleLineDiagramModel {

    public static class ContainerResult {
        private final StringProperty svgContent = new SimpleStringProperty();

        private final StringProperty metadataContent = new SimpleStringProperty();

        private final StringProperty jsonContent = new SimpleStringProperty();

        public void clean() {
            svgContent.set("");
            metadataContent.set("");
            jsonContent.set("");
        }

        public StringProperty svgContentProperty() {
            return svgContent;
        }

        public StringProperty metadataContentProperty() {
            return metadataContent;
        }

        public StringProperty jsonContentProperty() {
            return jsonContent;
        }

        public void setValue(ContainerResult value) {
            svgContent.setValue(value.svgContent.getValue());
            metadataContent.setValue(value.metadataContent.getValue());
            jsonContent.setValue(value.jsonContent.getValue());
        }
    }

    private static final String UNKNOWN_ITEM = "???";
    private static final String BASIC_STYLE = "Basic";
    private static final String NOMINAL_STYLE = "Nominal voltage";
    private static final String TOPOLOGY_STYLE = "Topology (default)";

    private static final String HORIZONTAL_SUBSTATION_LAYOUT = "Horizontal";
    private static final String VERTICAL_SUBSTATION_LAYOUT = "Vertical";
    private static final String CGMES_SUBSTATION_LAYOUT = "CGMES";
    private static final String SMART_VOLTAGELEVEL_LAYOUT = "Smart";
    private static final String AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT = "Auto extensions";
    private static final String AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT = "Auto without extensions Clustering";
    private static final String RANDOM_VOLTAGELEVEL_LAYOUT = "Random";
    private static final String CGMES_VOLTAGELEVEL_LAYOUT = "CGMES";

    private final ContainerResult selectedContainerResult = new ContainerResult();

    private final Map<Container<?>, ContainerResult> containerToResultMap = new HashMap<>();

    // Layout Parameters
    private final LayoutParametersBean layoutParameters;

    // Component library provider
    private final ObservableList<ComponentLibrary> componentLibraries = FXCollections.observableArrayList();

    private final ObjectProperty<ComponentLibrary> currentComponentLibrary = new SimpleObjectProperty<>();

    // Style provider
    private final Map<String, DiagramStyleProvider> nameToDiagramStyleProviderMap = new TreeMap<>(); // ordered

    private final ObservableList<DiagramStyleProvider> styleProviders = FXCollections.observableArrayList();

    private final ObjectProperty<DiagramStyleProvider> currentStyleProvider = new SimpleObjectProperty<>();

    // VoltageLevel layout provider
    private final Map<String, VoltageLevelLayoutFactory> nameToVoltageLevelLayoutFactoryMap = new TreeMap<>(); // ordered

    private final ObservableList<VoltageLevelLayoutFactory> voltageLevelLayouts = FXCollections.observableArrayList();

    private final VoltageLevelLayoutFactoryBean voltageLevelLayoutFactory;

    // Substation layout provider
    private final Map<String, SubstationLayoutFactory> nameToSubstationLayoutFactoryMap = new TreeMap<>(); // ordered

    private final ObservableList<SubstationLayoutFactory> substationLayouts = FXCollections.observableArrayList();

    private final ObjectProperty<SubstationLayoutFactory> currentSubstationLayoutFactory = new SimpleObjectProperty<>();

    // CGMES-DL names
    private final ObservableList<String> cgmesDLDiagramNames = FXCollections.observableArrayList();

    private final StringProperty currentCgmesDLDiagramName = new SimpleStringProperty();

    public SingleLineDiagramModel(// Providers
                                  ReadOnlyObjectProperty<ComponentLibrary> componentLibrary,
                                  ReadOnlyObjectProperty<DiagramStyleProvider> styleProvider,
                                  ReadOnlyObjectProperty<VoltageLevelLayoutFactory> voltageLevelLayoutFactory,
                                  ReadOnlyObjectProperty<SubstationLayoutFactory> substationLayoutFactory,
                                  ReadOnlyObjectProperty<String> cgmesDLDiagramName,
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
                                  BooleanProperty showGrid,
                                  BooleanProperty showInternalNodes,
                                  BooleanProperty drawStraightWires,
                                  BooleanProperty disconnectorsOnBus,
                                  Property<Double> scaleFactor,
                                  BooleanProperty avoidSVGComponentsDuplication,
                                  BooleanProperty adaptCellHeightToContent,
                                  Property<Double> minSpaceBetweenComponents,
                                  Property<Double> minimumExternCellHeight,
                                  Property<LayoutParameters.Alignment> busBarAlignment,
                                  BooleanProperty centerLabel,
                                  BooleanProperty labelDiagonal,
                                  Property<Double> angleLabel,
                                  BooleanProperty highLightLineState,
                                  BooleanProperty addNodesInfos,
                                  BooleanProperty feederInfoSymmetry,
                                  Property<Double> spaceForFeederInfos,
                                  Property<Double> feederInfosOuterMargin,
                                  Property<Double> feederInfosIntraMargin
    ) {
        // Update providers observable lists
        initProviders();

        // Providers
        this.currentComponentLibrary.bind(componentLibrary);
        this.currentStyleProvider.bind(styleProvider);
        this.currentSubstationLayoutFactory.bind(substationLayoutFactory);
        this.currentCgmesDLDiagramName.bind(cgmesDLDiagramName);
        this.voltageLevelLayoutFactory = new VoltageLevelLayoutFactoryBean(voltageLevelLayoutFactory,
                stackFeeders,
                exceptionWhenPatternUnhandled,
                handleShunts,
                removeFictitiousNodes,
                substituteSingularFictitiousNodes);

        // Layout Parameters
        this.layoutParameters = new LayoutParametersBean(diagramPaddingTopBottom,
                diagramPaddingLeftRight,
                voltagePaddingTopBottom,
                voltagePaddingLeftRight,
                busbarVerticalSpace,
                busbarHorizontalSpace,
                cellWidth,
                externCellHeight,
                internCellHeight,
                stackHeight,
                showGrid,
                showInternalNodes,
                drawStraightWires,
                disconnectorsOnBus,
                scaleFactor,
                avoidSVGComponentsDuplication,
                adaptCellHeightToContent,
                minSpaceBetweenComponents,
                minimumExternCellHeight,
                busBarAlignment,
                centerLabel,
                labelDiagonal,
                angleLabel,
                highLightLineState,
                addNodesInfos,
                feederInfoSymmetry,
                spaceForFeederInfos,
                feederInfosOuterMargin,
                feederInfosIntraMargin);
    }

    public void initProviders() {
        // StyleProviders
        nameToDiagramStyleProviderMap.put(BASIC_STYLE, new BasicStyleProvider());
        // VoltageLevelLayouts
        nameToVoltageLevelLayoutFactoryMap.put(AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionFromExtension()));
        nameToVoltageLevelLayoutFactoryMap.put(AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionByClustering()));
        nameToVoltageLevelLayoutFactoryMap.put(RANDOM_VOLTAGELEVEL_LAYOUT, new RandomVoltageLevelLayoutFactory(500, 500));
        // SubstationLayouts
        nameToSubstationLayoutFactoryMap.put(HORIZONTAL_SUBSTATION_LAYOUT, new HorizontalSubstationLayoutFactory());
        nameToSubstationLayoutFactoryMap.put(VERTICAL_SUBSTATION_LAYOUT, new VerticalSubstationLayoutFactory());

        // Set all providers list
        componentLibraries.setAll(ComponentLibrary.findAll());
        styleProviders.setAll(nameToDiagramStyleProviderMap.values());
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
        voltageLevelLayouts.setAll(nameToVoltageLevelLayoutFactoryMap.values());
    }

    public void updateFrom(Network network) {
        if (network != null) {
            // Styles
            nameToDiagramStyleProviderMap.put(NOMINAL_STYLE, new NominalVoltageDiagramStyleProvider(network));
            nameToDiagramStyleProviderMap.put(TOPOLOGY_STYLE, new TopologicalStyleProvider(network));
            // VoltageLevelLayouts
            nameToVoltageLevelLayoutFactoryMap.put(SMART_VOLTAGELEVEL_LAYOUT, new SmartVoltageLevelLayoutFactory(network));
            nameToVoltageLevelLayoutFactoryMap.put(CGMES_VOLTAGELEVEL_LAYOUT, new CgmesVoltageLevelLayoutFactory(network));
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
        styleProviders.setAll(nameToDiagramStyleProviderMap.values());
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
        voltageLevelLayouts.setAll(nameToVoltageLevelLayoutFactoryMap.values());
    }

    public void addListener(ChangeListener<Object> changeListener) {
        layoutParameters.addListener(changeListener);
        voltageLevelLayoutFactory.addListener(changeListener);
    }

    public ComponentLibrary getComponentLibrary() {
        return currentComponentLibrary.getValue();
    }

    public DiagramStyleProvider getStyleProvider() {
        return currentStyleProvider.get();
    }

    public VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        return voltageLevelLayoutFactory.getVoltageLevelLayoutFactory();
    }

    public SubstationLayoutFactory getSubstationLayoutFactory() {
        return currentSubstationLayoutFactory.get();
    }

    public LayoutParameters getLayoutParameters(boolean showNames) {
        return layoutParameters.getLayoutParameters(showNames, currentCgmesDLDiagramName.get());
    }

    public ContainerResult getSelectedContainerResult() {
        return selectedContainerResult;
    }

    public Stream<Container<?>> getCheckedContainerStream() {
        return containerToResultMap.keySet().stream();
    }

    public ContainerResult getCheckedContainerResult(Container<?> container) {
        return containerToResultMap.computeIfAbsent(container, c -> new ContainerResult());
    }

    public void removeCheckedContainerResult(Container<?> container) {
        containerToResultMap.remove(container);
    }

    public ObservableList<ComponentLibrary> getComponentLibraries() {
        return componentLibraries;
    }

    public ObservableList<DiagramStyleProvider> getStyleProviders() {
        return styleProviders;
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

    StringConverter<DiagramStyleProvider> getDiagramStyleProviderStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(DiagramStyleProvider object) {
                Optional<String> label = nameToDiagramStyleProviderMap.keySet().stream().filter(name -> nameToDiagramStyleProviderMap.get(name) == object).findFirst();
                return label.orElse(UNKNOWN_ITEM);
            }

            @Override
            public DiagramStyleProvider fromString(String item) {
                return nameToDiagramStyleProviderMap.get(item);
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

    public void clean() {
        selectedContainerResult.clean();
    }
}
