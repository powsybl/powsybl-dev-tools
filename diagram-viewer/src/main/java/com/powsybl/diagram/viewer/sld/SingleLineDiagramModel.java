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
import com.powsybl.sld.library.ComponentTypeName;
import com.powsybl.sld.svg.BasicStyleProvider;
import com.powsybl.sld.svg.DiagramStyleProvider;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
import javafx.beans.property.*;
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

    private static final String BASIC_STYLE = "Basic";
    private static final String NOMINAL_STYLE = "Nominal voltage";
    private static final String TOPOLOGY_STYLE = "Topology (default)";

    private static final String HORIZONTAL_SUBSTATION_LAYOUT = "Horizontal";
    private static final String VERTICAL_SUBSTATION_LAYOUT = "Vertical";
    private static final String CGMES_SUBSTATION_LAYOUT = "Cgmes";
    private static final String SMART_VOLTAGELEVEL_LAYOUT = "Smart";
    private static final String AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT = "Auto extensions";
    private static final String AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT = "Auto without extensions Clustering";
    private static final String RANDOM_VOLTAGELEVEL_LAYOUT = "Random";
    private static final String CGMES_VOLTAGELEVEL_LAYOUT = "Cgmes";

    private final ContainerResult selectedContainerResult = new ContainerResult();

    private final Map<Container<?>, ContainerResult> containerToResultMap = new HashMap<>();

    // Layout Parameters
    private final DoubleProperty diagramPaddingTopBottom = new SimpleDoubleProperty(new LayoutParameters().getDiagramPadding().getTop());

    private final DoubleProperty diagramPaddingLeftRight = new SimpleDoubleProperty(new LayoutParameters().getDiagramPadding().getLeft());

    private final DoubleProperty voltagePaddingTopBottom = new SimpleDoubleProperty(new LayoutParameters().getVoltageLevelPadding().getTop());

    private final DoubleProperty voltagePaddingLeftRight = new SimpleDoubleProperty(new LayoutParameters().getVoltageLevelPadding().getLeft());

    private final DoubleProperty busbarVerticalSpace = new SimpleDoubleProperty(new LayoutParameters().getVerticalSpaceBus());

    private final DoubleProperty busbarHorizontalSpace = new SimpleDoubleProperty(new LayoutParameters().getHorizontalBusPadding());

    private final DoubleProperty cellWidth = new SimpleDoubleProperty();

    private final DoubleProperty externCellHeight = new SimpleDoubleProperty();

    private final DoubleProperty internCellHeight = new SimpleDoubleProperty();

    private final DoubleProperty stackHeight = new SimpleDoubleProperty();

    private final BooleanProperty showGrid = new SimpleBooleanProperty();

    private final BooleanProperty showInternalNodes = new SimpleBooleanProperty();

    private final BooleanProperty drawStraightWires = new SimpleBooleanProperty();

    private final BooleanProperty disconnectorsOnBus = new SimpleBooleanProperty();

    private final DoubleProperty scaleFactor = new SimpleDoubleProperty();

    private final BooleanProperty avoidSVGComponentsDuplication = new SimpleBooleanProperty();

    private final BooleanProperty adaptCellHeightToContent = new SimpleBooleanProperty();

    private final DoubleProperty minSpaceBetweenComponents = new SimpleDoubleProperty();

    private final DoubleProperty minimumExternCellHeight = new SimpleDoubleProperty();

    private final ObjectProperty<LayoutParameters.Alignment> busBarAlignment = new SimpleObjectProperty<>();

    private final BooleanProperty centerLabel = new SimpleBooleanProperty();

    private final BooleanProperty labelDiagonal = new SimpleBooleanProperty();

    private final DoubleProperty angleLabel = new SimpleDoubleProperty();

    private final BooleanProperty highLightLineState = new SimpleBooleanProperty();

    private final BooleanProperty addNodesInfos = new SimpleBooleanProperty();

    private final BooleanProperty feederInfoSymmetry = new SimpleBooleanProperty();

    private final DoubleProperty spaceForFeederInfos = new SimpleDoubleProperty();

    private final DoubleProperty feederInfosOuterMargin = new SimpleDoubleProperty(new LayoutParameters().getFeederInfosOuterMargin());

    private final DoubleProperty feederInfosIntraMargin = new SimpleDoubleProperty(new LayoutParameters().getFeederInfosIntraMargin());

    // PositionVoltageLevelLayoutFactory
    private final BooleanProperty stackFeeders = new SimpleBooleanProperty();

    private final BooleanProperty exceptionWhenPatternUnhandled = new SimpleBooleanProperty();

    private final BooleanProperty handleShunts = new SimpleBooleanProperty();

    private final BooleanProperty removeFictitiousNodes = new SimpleBooleanProperty();

    private final BooleanProperty substituteSingularFictitiousNodes = new SimpleBooleanProperty();

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

    private final ObjectProperty<VoltageLevelLayoutFactory> currentVoltageLevelLayoutFactory = new SimpleObjectProperty<>();

    // Substation layout provider
    private final Map<String, SubstationLayoutFactory> nameToSubstationLayoutFactoryMap = new TreeMap<>(); // ordered

    private final ObservableList<SubstationLayoutFactory> substationLayouts = FXCollections.observableArrayList();

    private final ObjectProperty<SubstationLayoutFactory> currentSubstationLayoutFactory = new SimpleObjectProperty<>();

    // CGMES-DL names
    private final ObservableList<String> cgmesDLDiagramNames = FXCollections.observableArrayList();

    private final StringProperty currentCgmesDLDiagramName = new SimpleStringProperty();

    public SingleLineDiagramModel(ReadOnlyObjectProperty<ComponentLibrary> componentLibrary,
                                  ReadOnlyObjectProperty<DiagramStyleProvider> styleProvider,
                                  ReadOnlyObjectProperty<VoltageLevelLayoutFactory> voltageLevelLayoutFactory,
                                  ReadOnlyObjectProperty<SubstationLayoutFactory> substationLayoutFactory,
                                  ReadOnlyObjectProperty<String> cgmesDLDiagramName,
                                  ReadOnlyObjectProperty<Double> diagramPaddingTopBottom,
                                  ReadOnlyObjectProperty<Double> diagramPaddingLeftRight,
                                  ReadOnlyObjectProperty<Double> voltagePaddingTopBottom,
                                  ReadOnlyObjectProperty<Double> voltagePaddingLeftRight,
                                  ReadOnlyObjectProperty<Double> busbarVerticalSpace,
                                  ReadOnlyObjectProperty<Double> busbarHorizontalSpace,
                                  ReadOnlyObjectProperty<Double> cellWidth,
                                  ReadOnlyObjectProperty<Double> externCellHeight,
                                  ReadOnlyObjectProperty<Double> internCellHeight,
                                  ReadOnlyObjectProperty<Double> stackHeight,
                                  ReadOnlyBooleanProperty showGrid,
                                  ReadOnlyBooleanProperty showInternalNodes,
                                  ReadOnlyBooleanProperty drawStraightWires,
                                  ReadOnlyBooleanProperty disconnectorsOnBus,
                                  BooleanProperty stackFeeders,
                                  BooleanProperty exceptionWhenPatternUnhandled,
                                  BooleanProperty handleShunts,
                                  BooleanProperty removeFictitiousNodes,
                                  BooleanProperty substituteSingularFictitiousNodes,
                                  ReadOnlyObjectProperty<Double> scaleFactor,
                                  ReadOnlyBooleanProperty avoidSVGComponentsDuplication,
                                  ReadOnlyBooleanProperty adaptCellHeightToContent,
                                  ReadOnlyObjectProperty<Double> minSpaceBetweenComponents,
                                  ReadOnlyObjectProperty<Double> minimumExternCellHeight,
                                  ReadOnlyObjectProperty<LayoutParameters.Alignment> busBarAlignment,
                                  ReadOnlyBooleanProperty centerLabel,
                                  ReadOnlyBooleanProperty labelDiagonal,
                                  ReadOnlyObjectProperty<Double> angleLabel,
                                  ReadOnlyBooleanProperty highLightLineState,
                                  ReadOnlyBooleanProperty addNodesInfos,
                                  ReadOnlyBooleanProperty feederInfoSymmetry,
                                  ReadOnlyObjectProperty<Double> spaceForFeederInfos,
                                  ReadOnlyObjectProperty<Double> feederInfosOuterMargin,
                                  ReadOnlyObjectProperty<Double> feederInfosIntraMargin
                                  ) {
        // Providers
        this.currentComponentLibrary.bind(componentLibrary);
        this.currentStyleProvider.bind(styleProvider);
        this.currentVoltageLevelLayoutFactory.bind(voltageLevelLayoutFactory);
        this.currentVoltageLevelLayoutFactory.addListener((observable, oldValue, newValue) -> {
            boolean selected = newValue instanceof PositionVoltageLevelLayoutFactory;
            if (selected) {
                this.stackFeeders.setValue(((PositionVoltageLevelLayoutFactory) newValue).isFeederStacked());
                this.exceptionWhenPatternUnhandled.setValue(((PositionVoltageLevelLayoutFactory) newValue).isExceptionIfPatternNotHandled());
                this.handleShunts.setValue(((PositionVoltageLevelLayoutFactory) newValue).isHandleShunts());
                this.removeFictitiousNodes.setValue(((PositionVoltageLevelLayoutFactory) newValue).isRemoveUnnecessaryFictitiousNodes());
                this.substituteSingularFictitiousNodes.setValue(((PositionVoltageLevelLayoutFactory) newValue).isSubstituteSingularFictitiousByFeederNode());
            }
        });

        this.currentSubstationLayoutFactory.bind(substationLayoutFactory);
        this.currentCgmesDLDiagramName.bind(cgmesDLDiagramName);

        // Layout Parameters
        this.diagramPaddingTopBottom.bind(diagramPaddingTopBottom);
        this.diagramPaddingLeftRight.bind(diagramPaddingLeftRight);
        this.voltagePaddingTopBottom.bind(voltagePaddingTopBottom);
        this.voltagePaddingLeftRight.bind(voltagePaddingLeftRight);
        this.busbarVerticalSpace.bind(busbarVerticalSpace);
        this.busbarHorizontalSpace.bind(busbarHorizontalSpace);
        this.cellWidth.bind(cellWidth);
        this.externCellHeight.bind(externCellHeight);
        this.internCellHeight.bind(internCellHeight);
        this.stackHeight.bind(stackHeight);
        this.showGrid.bind(showGrid);
        this.showInternalNodes.bind(showInternalNodes);
        this.drawStraightWires.bind(drawStraightWires);
        this.disconnectorsOnBus.bind(disconnectorsOnBus);
        this.scaleFactor.bind(scaleFactor);
        this.avoidSVGComponentsDuplication.bind(avoidSVGComponentsDuplication);
        this.adaptCellHeightToContent.bind(adaptCellHeightToContent);
        this.minSpaceBetweenComponents.bind(minSpaceBetweenComponents);
        this.minimumExternCellHeight.bind(minimumExternCellHeight);
        this.busBarAlignment.bind(busBarAlignment);
        this.centerLabel.bind(centerLabel);
        this.labelDiagonal.bind(labelDiagonal);
        this.angleLabel.bind(angleLabel);
        this.highLightLineState.bind(highLightLineState);
        this.addNodesInfos.bind(addNodesInfos);
        this.feederInfoSymmetry.bind(feederInfoSymmetry);
        this.spaceForFeederInfos.bind(spaceForFeederInfos);
        this.feederInfosOuterMargin.bind(feederInfosOuterMargin);
        this.feederInfosIntraMargin.bind(feederInfosIntraMargin);

        // PositionVoltageLevelLayoutFactory
        this.stackFeeders.bindBidirectional(stackFeeders);
        this.exceptionWhenPatternUnhandled.bindBidirectional(exceptionWhenPatternUnhandled);
        this.handleShunts.bindBidirectional(handleShunts);
        this.removeFictitiousNodes.bindBidirectional(removeFictitiousNodes);
        this.substituteSingularFictitiousNodes.bindBidirectional(substituteSingularFictitiousNodes);

        // Update observable lists
        initProviders();
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

    public ComponentLibrary getComponentLibrary() {
        return currentComponentLibrary.getValue();
    }

    public DiagramStyleProvider getStyleProvider() {
        return currentStyleProvider.get();
    }

    public VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        VoltageLevelLayoutFactory layoutFactory = currentVoltageLevelLayoutFactory.get();
        if (layoutFactory instanceof PositionVoltageLevelLayoutFactory) {
            // PositionVoltageLevelLayoutFactory
            ((PositionVoltageLevelLayoutFactory)layoutFactory).setFeederStacked(stackFeeders.get());
            ((PositionVoltageLevelLayoutFactory)layoutFactory).setExceptionIfPatternNotHandled(exceptionWhenPatternUnhandled.get());
            ((PositionVoltageLevelLayoutFactory)layoutFactory).setHandleShunts(handleShunts.get());
            ((PositionVoltageLevelLayoutFactory)layoutFactory).setRemoveUnnecessaryFictitiousNodes(removeFictitiousNodes.get());
            ((PositionVoltageLevelLayoutFactory)layoutFactory).setSubstituteSingularFictitiousByFeederNode(substituteSingularFictitiousNodes.get());
        }

        return layoutFactory;
    }

    public SubstationLayoutFactory getSubstationLayoutFactory() {
        return currentSubstationLayoutFactory.get();
    }

    public LayoutParameters getLayoutParameters(boolean showNames) {
        return new LayoutParameters()
                .setDiagrammPadding(diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get(),
                        diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get())
                .setVoltageLevelPadding(voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get(),
                        voltagePaddingLeftRight.get(),
                        voltagePaddingTopBottom.get())
                .setUseName(showNames)
                .setDiagramName(currentCgmesDLDiagramName.get())
                .setVerticalSpaceBus(busbarVerticalSpace.get())
                .setHorizontalBusPadding(busbarHorizontalSpace.get())
                .setCellWidth(cellWidth.get())
                .setExternCellHeight(externCellHeight.get())
                .setInternCellHeight(internCellHeight.get())
                .setStackHeight(stackHeight.get())
                .setShowGrid(showGrid.get())
                .setShowInternalNodes(showInternalNodes.get())
                .setDrawStraightWires(drawStraightWires.get())
                .setComponentsOnBusbars(disconnectorsOnBus.get() ? List.of(ComponentTypeName.DISCONNECTOR) : Collections.emptyList())
                .setScaleFactor(scaleFactor.get())
                .setAvoidSVGComponentsDuplication(avoidSVGComponentsDuplication.get())
                .setAdaptCellHeightToContent(adaptCellHeightToContent.get())
                .setMinSpaceBetweenComponents(minSpaceBetweenComponents.get())
                .setMinExternCellHeight(minimumExternCellHeight.get())
                .setBusbarsAlignment(busBarAlignment.get())
                .setLabelCentered(centerLabel.get())
                .setLabelDiagonal(labelDiagonal.get())
                .setAngleLabelShift(angleLabel.get())
                .setHighlightLineState(highLightLineState.get())
                .setAddNodesInfos(addNodesInfos.get())
                .setFeederInfoSymmetry(feederInfoSymmetry.get())
                .setSpaceForFeederInfos(spaceForFeederInfos.get())
                .setFeederInfosOuterMargin(feederInfosOuterMargin.get())
                .setFeederInfosIntraMargin(feederInfosIntraMargin.get())
                // Forced values
                .setCssLocation(LayoutParameters.CssLocation.INSERTED_IN_SVG)
                .setSvgWidthAndHeightAdded(true);
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
                return value != null ? value.getName() : "???";
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
                return label.orElse("???");
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
                return label.orElse("???");
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
                return label.orElse("???");
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
