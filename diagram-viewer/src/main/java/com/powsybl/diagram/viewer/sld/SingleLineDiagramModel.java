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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private final DoubleProperty diagramPaddingTopBottom = new SimpleDoubleProperty();

    private final DoubleProperty diagramPaddingLeftRight = new SimpleDoubleProperty();

    private final ObservableList<ComponentLibrary> componentLibraries = FXCollections.observableArrayList();

    private final ObservableList<DiagramStyleProvider> styleProviders = FXCollections.observableArrayList();

    private final ObservableList<VoltageLevelLayoutFactory> voltageLevelLayouts = FXCollections.observableArrayList();

    private final ObservableList<SubstationLayoutFactory> substationLayouts = FXCollections.observableArrayList();

    private final ObservableList<String> cgmesDLDiagramNames = FXCollections.observableArrayList();

    private final ObjectProperty<ComponentLibrary> componentLibrary = new SimpleObjectProperty<>();

    private final Map<String, DiagramStyleProvider> nameToDiagramStyleProviderMap = new HashMap<>();

    private final Map<String, VoltageLevelLayoutFactory> nameToVoltageLevelLayoutFactoryMap = new HashMap<>();

    private final Map<String, SubstationLayoutFactory> nameToSubstationLayoutFactoryMap = new HashMap<>();

    private final ObjectProperty<DiagramStyleProvider> styleProvider = new SimpleObjectProperty<>();

    private final ObjectProperty<VoltageLevelLayoutFactory> voltageLevelLayoutFactory = new SimpleObjectProperty<>();

    private final ObjectProperty<SubstationLayoutFactory> substationLayoutFactory = new SimpleObjectProperty<>();

    private final StringProperty cgmesDLDiagramName = new SimpleStringProperty();

    public SingleLineDiagramModel(ReadOnlyObjectProperty<ComponentLibrary> componentLibrary,
                                  ReadOnlyObjectProperty<DiagramStyleProvider> styleProvider,
                                  ReadOnlyObjectProperty<VoltageLevelLayoutFactory> voltageLevelLayoutFactory,
                                  ReadOnlyObjectProperty<SubstationLayoutFactory> substationLayoutFactory,
                                  ReadOnlyObjectProperty<String> cgmesDLDiagramName,
                                  ReadOnlyObjectProperty<Double> diagramPaddingTopBottom,
                                  ReadOnlyObjectProperty<Double> diagramPaddingLeftRight) {
        // Providers
        this.componentLibrary.bind(componentLibrary);
        this.styleProvider.bind(styleProvider);
        this.voltageLevelLayoutFactory.bind(voltageLevelLayoutFactory);
        this.substationLayoutFactory.bind(substationLayoutFactory);
        this.cgmesDLDiagramName.bind(cgmesDLDiagramName);

        // Layout Parameters
        this.diagramPaddingTopBottom.bind(diagramPaddingTopBottom);
        this.diagramPaddingLeftRight.bind(diagramPaddingLeftRight);

        // Update observable lists
        initProviders();
        styleProviders.setAll(nameToDiagramStyleProviderMap.values());
        voltageLevelLayouts.setAll(nameToVoltageLevelLayoutFactoryMap.values());
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
    }

    public void initProviders() {
        componentLibraries.setAll(ComponentLibrary.findAll());
        // StyleProviders
        nameToDiagramStyleProviderMap.put(BASIC_STYLE, new BasicStyleProvider());
        // VoltageLevelLayouts
        nameToVoltageLevelLayoutFactoryMap.put(AUTO_EXTENSIONS_VOLTAGELEVEL_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionFromExtension()));
        nameToVoltageLevelLayoutFactoryMap.put(AUTO_WITHOUT_EXTENSIONS_CLUSTERING_VOLTAGELEVEL_LAYOUT, new PositionVoltageLevelLayoutFactory(new PositionByClustering()));
        nameToVoltageLevelLayoutFactoryMap.put(RANDOM_VOLTAGELEVEL_LAYOUT, new RandomVoltageLevelLayoutFactory(500, 500));
        // SubstationLayouts
        nameToSubstationLayoutFactoryMap.put(HORIZONTAL_SUBSTATION_LAYOUT, new HorizontalSubstationLayoutFactory());
        nameToSubstationLayoutFactoryMap.put(VERTICAL_SUBSTATION_LAYOUT, new VerticalSubstationLayoutFactory());
    }

    public void updateFrom(Network network) {
        initProviders();
        // Styles
        nameToDiagramStyleProviderMap.put(NOMINAL_STYLE, new NominalVoltageDiagramStyleProvider(network));
        nameToDiagramStyleProviderMap.put(TOPOLOGY_STYLE, new TopologicalStyleProvider(network));
        // VoltageLevelLayouts
        nameToVoltageLevelLayoutFactoryMap.put(SMART_VOLTAGELEVEL_LAYOUT, new SmartVoltageLevelLayoutFactory(network));
        nameToVoltageLevelLayoutFactoryMap.put(CGMES_VOLTAGELEVEL_LAYOUT, new CgmesVoltageLevelLayoutFactory(network));
        nameToSubstationLayoutFactoryMap.put(CGMES_SUBSTATION_LAYOUT, new CgmesSubstationLayoutFactory(network));

        styleProviders.setAll(nameToDiagramStyleProviderMap.values());
        voltageLevelLayouts.setAll(nameToVoltageLevelLayoutFactoryMap.values());
        substationLayouts.setAll(nameToSubstationLayoutFactoryMap.values());
        cgmesDLDiagramNames.setAll(NetworkDiagramData.getDiagramsNames(network));
    }

    public ComponentLibrary getComponentLibrary() {
        return componentLibrary.getValue();
    }

    public DiagramStyleProvider getStyleProvider() {
        return styleProvider.get();
    }

    public VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        return voltageLevelLayoutFactory.get();
    }

    public SubstationLayoutFactory getSubstationLayoutFactory() {
        return substationLayoutFactory.get();
    }

    public LayoutParameters getLayoutParameters(boolean showNames) {
        return new LayoutParameters()
                .setDiagrammPadding(diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get(),
                        diagramPaddingLeftRight.get(),
                        diagramPaddingTopBottom.get())
                .setUseName(showNames)
                .setDiagramName(cgmesDLDiagramName.get())
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
