/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.diagram.viewer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.diagram.viewer.nad.NetworkAreaDiagramViewController;
import com.powsybl.diagram.viewer.sld.SingleLineDiagramJsHandler;
import com.powsybl.diagram.viewer.sld.SingleLineDiagramViewController;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.*;
import com.powsybl.loadflow.LoadFlow;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class MainViewController {

    public enum ComponentFilterType {
        ALL(null, "All"),
        HVDC_LINE(null, "HVDC line"),
        SWITCH(null, "Switch"),
        TIE_LINE(null, "Tie line"),
        BUSBAR_SECTION(BusbarSection.class, "Busbar section"),
        LINE(Line.class, "Line"),
        TWO_WINDINGS_TRANSFORMER(TwoWindingsTransformer.class, "Two-winding transformer"),
        PHASE_SHIFT_TRANSFORMER(TwoWindingsTransformer.class, "Phase-shift transformer"),
        RATIO_TAP_CHANGER_TRANSFORMER(TwoWindingsTransformer.class, "Ratio-tap changer transformer"),
        THREE_WINDINGS_TRANSFORMER(ThreeWindingsTransformer.class, "Three-winding transformer"),
        GENERATOR(Generator.class, "Generator"),
        BATTERY(Battery.class, "Battery"),
        LOAD(Load.class, "Load"),
        SHUNT_COMPENSATOR(ShuntCompensator.class, "Shunt compensator"),
        DANGLING_LINE(DanglingLine.class, "Dangling line"),
        STATIC_VAR_COMPENSATOR(StaticVarCompensator.class, "Static VAR compensator"),
        LCC_CONVERTER_STATION(LccConverterStation.class, "LCC converter station"),
        VSC_CONVERTER_STATION(VscConverterStation.class, "VSC converter station"),
        GROUND(Ground.class, "Ground");

        private final Class<? extends Connectable<?>> connectableClass;
        private final String description;

        ComponentFilterType(Class<? extends Connectable<?>> connectableClass, String description) {
            this.connectableClass = connectableClass;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewController.class);

    private static final String SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY = "selectedVoltageLevelAndSubstationIds";
    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private final Preferences preferences = Preferences.userNodeForPackage(DiagramViewer.class);
    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    private Service<Network> networkService;

    @FXML
    public TextField filePath;
    @FXML
    public TextField filterField;

    @FXML
    public ChoiceBox<ComponentFilterType> componentTypeFilterChoice;

    @FXML
    public ComboBox<String> countriesFilterComboBox;

    @FXML
    public TreeView<Container<?>> vlTree;
    @FXML
    public CheckBox showNames;

    // Selection between sld and nad
    @FXML
    public TabPane sldOrNad;

    // Status info
    @FXML
    private Node loadingStatus;

    @FXML
    private MenuButton networkFactoryMenuButton;

    private Model model;

    /**
     * NetworkAreaDiagram tab content is included from nadView.fxml.
     * To get associated controller we have to use "Controller" suffix to nadView.fxml associated fx:id.
     */
    @FXML
    private NetworkAreaDiagramViewController nadViewController;

    /**
     * SingleLineDiagram tab content is included from sldView.fxml.
     * To get associated controller we have to use "Controller" suffix to sldView.fxml associated fx:id.
     */
    @FXML
    private SingleLineDiagramViewController sldViewController;

    private SingleLineDiagramJsHandler sldJsHandler;

    /**
     * Filtered list for the substation tree view.
     * We need to keep a reference to this list, otherwise it gets garbage collected
     */
    private FilteredList<TreeItem<Container<?>>> filteredList;

    private Set<String> containersChecked = new HashSet<>();

    @FXML
    private void initialize() {
        initializeNetworkFactories();

        sldJsHandler = new SingleLineDiagramJsHandler(vlTree);

        // to avoid bug in TreeView: it does not calculate properly the selection shift, so clearing selection
        filterField.textProperty().addListener((observable, oldValue, newValue) -> clearSelection());
        componentTypeFilterChoice.valueProperty().addListener((observable, oldValue, newValue) -> clearSelection());
        countriesFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> clearSelection());

        String casePathPropertyValue = preferences.get(CASE_PATH_PROPERTY, null);
        if (casePathPropertyValue != null) {
            loadFile(new File(casePathPropertyValue));
        }

        model = new Model(showNames.selectedProperty(), nadViewController.getModel(), sldViewController.getModel());

        model.networkProperty().addListener((observableValue, oldNetwork, newNetwork) -> {
            sldViewController.updateFrom(model.networkProperty());
            initSubstationsTree(newNetwork);
        });

        showNames.selectedProperty().addListener((observable, oldValue, newValue) -> vlTree.refresh());

        countriesFilterComboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> model.getCountriesNames()));
        countriesFilterComboBox.disableProperty().bind(Bindings.createBooleanBinding(() -> model.getCountriesNames().isEmpty(), model.networkProperty()));

        vlTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Container<?> c = newValue.getValue();
                model.setSelectedContainer(c);
                nadViewController.createDiagram(model.getNetwork(), c);
                sldViewController.createDiagram(sldJsHandler, model.getNetwork(), c);
            }
        });
        vlTree.setCellFactory(param -> {
            CheckBoxTreeCell<Container<?>> treeCell = new CheckBoxTreeCell<>();
            treeCell.setConverter(new StringConverter<>() {
                @Override
                public String toString(TreeItem<Container<?>> item) {
                    var c = item.getValue();
                    if (c == null) {
                        return "";
                    }
                    if (c.getContainerType() == ContainerType.NETWORK) {
                        return "Full Network";
                    }
                    String cNameOrId = getIdentifiableStringSupplier().apply(c);
                    if (c instanceof Substation substation) {
                        long nbVoltageLevels = substation.getVoltageLevelStream().count();
                        cNameOrId += " [" + nbVoltageLevels + "]";
                    }
                    return cNameOrId;
                }

                @Override
                public TreeItem<Container<?>> fromString(String string) {
                    return null;
                }
            });
            return treeCell;
        });

        nadViewController.addListener((observable, oldValue, newValue) -> updateNadDiagrams());
        sldViewController.addListener((observable, oldValue, newValue) -> updateSldDiagrams());
    }

    private void clearSelection() {
        if (!vlTree.getSelectionModel().getSelectedItems().isEmpty()) {
            vlTree.getSelectionModel().clearSelection();
        }
    }

    private void initializeNetworkFactories() {
        Map<String, Supplier<Network>> suppliers = new HashMap<>();
        suppliers.put("BatteryNetwork", BatteryNetworkFactory::create);
        suppliers.put("DanglingLineNetwork", DanglingLineNetworkFactory::create);
        suppliers.put("EuropeanLvTestFeeder", EuropeanLvTestFeederFactory::create);
        suppliers.put("EurostagTutorialExample1", EurostagTutorialExample1Factory::create);
        suppliers.put("FictitiousSwitch", FictitiousSwitchFactory::create);
        suppliers.put("FourSubstationsNodeBreaker", FourSubstationsNodeBreakerFactory::create);
        suppliers.put("FourSubstationsNodeBreakerWithExtensions", FourSubstationsNodeBreakerWithExtensionsFactory::create);
        suppliers.put("MultipleExtensionsTestNetwork", MultipleExtensionsTestNetworkFactory::create);
        suppliers.put("NetworkBusBreakerTest1", NetworkBusBreakerTest1Factory::create);
        suppliers.put("NetworkTest1", NetworkTest1Factory::create);
        suppliers.put("NoEquipmentNetwork", NoEquipmentNetworkFactory::create);
        suppliers.put("PhaseShifterTestCase", PhaseShifterTestCaseFactory::create);
        suppliers.put("ReactiveLimitsTestNetwork", ReactiveLimitsTestNetworkFactory::create);
        suppliers.put("ScadaNetwork", ScadaNetworkFactory::create);
        suppliers.put("SecurityAnalysisTestNetwork", SecurityAnalysisTestNetworkFactory::create);
        suppliers.put("ShuntTestCase", ShuntTestCaseFactory::create);
        suppliers.put("SvcTestCase", SvcTestCaseFactory::create);
        suppliers.put("ThreeWindingsTransformerNetwork", ThreeWindingsTransformerNetworkFactory::create);
        suppliers.put("TwoVoltageLevelNetwork", TwoVoltageLevelNetworkFactory::create);

        // Populate Networks list
        for (Map.Entry<String, Supplier<Network>> entry : suppliers.entrySet()) {
            // Build menu item
            MenuItem item = new MenuItem(entry.getKey());
            item.setOnAction(event -> loadFactory(entry.getKey(), entry.getValue()));
            networkFactoryMenuButton.getItems().add(item);
        }
    }

    private void updateSldDiagrams() {
        sldViewController.updateAllDiagrams(model.getNetwork(), model.getSelectedContainer());
    }

    private void updateNadDiagrams() {
        nadViewController.updateAllDiagrams(model.getNetwork(), model.getSelectedContainer());
    }

    @FXML
    private void onClickLoadFile(MouseEvent event) {
        event.consume();
        loadFile(selectFile());
    }

    private File selectFile() {
        FileChooser fileChooser = new FileChooser();
        String caseFolderPropertyValue = preferences.get(CASE_FOLDER_PROPERTY, null);
        if (caseFolderPropertyValue != null && Files.isDirectory(Path.of(caseFolderPropertyValue))) {
            fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
        }
        fileChooser.setTitle("Open case File");
        return fileChooser.showOpenDialog(loadingStatus.getScene().getWindow());
    }

    public void loadFile(File file) {
        if (file != null) {
            if (networkService != null && networkService.isRunning()) {
                networkService.cancel();
            }
            networkService = new Service<>() {
                @Override
                protected Task<Network> createTask() {
                    return new Task<>() {
                        @Override
                        protected Network call() {
                            Properties properties = new Properties();
                            properties.put("iidm.import.cgmes.post-processors", List.of("cgmesDLImport", "cgmesGLImport"));
                            properties.put("matpower.import.ignore-base-voltage", "false");
                            return Network.read(file.toPath(), LocalComputationManager.getDefault(), new ImportConfig(), properties);
                        }
                    };
                }
            };
            handleLoadingFileResult(file, networkService);
            networkService.start();
        }
    }

    public void loadFactory(String name, Supplier<Network> supplier) {
        if (networkService != null && networkService.isRunning()) {
            networkService.cancel();
        }
        networkService = new Service<>() {
            @Override
            protected Task<Network> createTask() {
                return new Task<>() {
                    @Override
                    protected Network call() {
                        // Get Network instance
                        return supplier.get();
                    }
                };
            }
        };
        handleLoadingFactoryResult(name, networkService);
        networkService.start();
    }

    private void handleLoadingFileResult(File file, Service<Network> networkService) {
        networkService.setOnRunning(event -> {
            loadingStatus.setStyle("-fx-background-color: yellow");
            filePath.setText(file.getAbsolutePath());
            preferences.put(CASE_FOLDER_PROPERTY, file.getParent());
        });

        networkService.setOnSucceeded(event -> {
            clean();
            model.setNetwork((Network) event.getSource().getValue());
            loadingStatus.setStyle("-fx-background-color: green");
            preferences.put(CASE_PATH_PROPERTY, file.getAbsolutePath());
        });

        networkService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
            filePath.setText("");
            loadingStatus.setStyle("-fx-background-color: red");
        });

        networkService.setOnCancelled(event -> {
            filePath.setText("");
            loadingStatus.setStyle("-fx-background-color: orange");
        });
    }

    private void handleLoadingFactoryResult(String name, Service<Network> networkService) {
        networkService.setOnRunning(event -> {
            loadingStatus.setStyle("-fx-background-color: yellow");
            filePath.setText(name);
        });

        networkService.setOnSucceeded(event -> {
            clean();
            Network network = (Network) event.getSource().getValue();
            model.setNetwork(network);
            loadingStatus.setStyle("-fx-background-color: green");
        });

        networkService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
            filePath.setText("");
            loadingStatus.setStyle("-fx-background-color: red");
        });
    }

    private void clean() {
        // View
        filterField.setText("");
        // Controller (sld & nad)
        nadViewController.clean();
        sldViewController.clean();
        // Model (sld & nad)
        model.clean();
    }

    @FXML
    private void onClickLoadFlow(MouseEvent actionEvent) {
        LoadFlow.run(model.getNetwork());
        // forces update -> better done with NetworkListener
        updateSldDiagrams();
        updateNadDiagrams();
        actionEvent.consume();
    }

    @FXML
    private void collapseSubstationsTree() {
        setSubstationsTreeExpanded(false);
    }

    @FXML
    private void expandSubstationsTree() {
        setSubstationsTreeExpanded(true);
    }

    private void setSubstationsTreeExpanded(boolean value) {
        vlTree.getRoot().getChildren().forEach(item -> item.setExpanded(value));
    }

    private void initSubstationsTree(Network network) {
        if (network == null) {
            return;
        }

        containersChecked = readSavedSelection();

        ObservableList<TreeItem<Container<?>>> substationItems = FXCollections.observableArrayList();
        for (Substation s : network.getSubstations()) {
            var sItem = createSubLevelCheckBoxTreeItem(s, substationItems, containersChecked);
            s.getVoltageLevelStream().forEach(v -> createSubLevelCheckBoxTreeItem(v, sItem.getChildren(), containersChecked));
        }
        network.getVoltageLevelStream()
                .filter(v -> v.getSubstation().isEmpty())
                .forEach(v -> createSubLevelCheckBoxTreeItem(v, substationItems, containersChecked));

        filteredList = new FilteredList<>(substationItems);
        filteredList.predicateProperty().bind(Bindings.createObjectBinding(() -> this::treeItemFilter,
                filterField.textProperty(),
                componentTypeFilterChoice.valueProperty(),
                countriesFilterComboBox.valueProperty()));

        var rootTreeItem = createCheckBoxTreeItem(network, containersChecked);
        Bindings.bindContent(rootTreeItem.getChildren(), filteredList);
        vlTree.setRoot(rootTreeItem);
        vlTree.setShowRoot(true);
    }

    private CheckBoxTreeItem<Container<?>> createCheckBoxTreeItem(Container<?> c, Set<String> selectedIds) {
        CheckBoxTreeItem<Container<?>> cItem = new CheckBoxTreeItem<>(c);
        cItem.setIndependent(true);
        cItem.setExpanded(true);
        addListenerOnContainerItem(cItem);
        if (selectedIds.contains(c.getId())) {
            cItem.setSelected(true);
        }
        return cItem;
    }

    private boolean treeItemFilter(TreeItem<Container<?>> item) {
        String filter = filterField.getText();
        ComponentFilterType idType = componentTypeFilterChoice.getValue();
        String countryValue = countriesFilterComboBox.getValue();
        Country country = countryValue != null ? Country.valueOf(countryValue) : null;
        var container = item.getValue();
        if (StringUtils.isEmpty(filter)) {
            return containsComponentType(idType, container) && locatedIn(country, container);
        } else {
            boolean filterOk = getIdentifiableStringSupplier().apply(container)
                    .toLowerCase(Locale.getDefault())
                    .contains(filter.toLowerCase(Locale.getDefault()));
            return (filterOk || item.getChildren().stream().anyMatch(this::treeItemFilter))
                    && containsComponentType(idType, container)
                    && locatedIn(country, container);
        }
    }

    private static boolean locatedIn(Country country, Container<? extends Identifiable<?>> container) {
        if (country == null) {
            return true; // no selection
        }
        if (container instanceof Substation s) {
            return s.getCountry().map(c -> c == country).orElse(false);
        } else if (container instanceof VoltageLevel v) {
            return v.getSubstation().flatMap(Substation::getCountry).map(c -> c == country).orElse(false);
        } else {
            return true; // network
        }
    }

    private static boolean containsComponentType(ComponentFilterType type, Container<?> container) {
        if (container instanceof Substation s) {
            return s.getVoltageLevelStream().anyMatch(v -> containsComponentType(type, v));
        } else if (container instanceof VoltageLevel v) {
            return switch (type) {
                case ALL -> true;
                case HVDC_LINE -> v.getConnectableStream(HvdcConverterStation.class).map(HvdcConverterStation::getHvdcLine).anyMatch(Objects::nonNull);
                case SWITCH -> v.getSwitchCount() != 0;
                case TIE_LINE -> v.getDanglingLineStream(DanglingLineFilter.PAIRED).findFirst().isPresent();
                case DANGLING_LINE -> v.getDanglingLineStream(DanglingLineFilter.UNPAIRED).findFirst().isPresent();
                case PHASE_SHIFT_TRANSFORMER -> v.getConnectableStream(TwoWindingsTransformer.class).anyMatch(TwoWindingsTransformer::hasPhaseTapChanger);
                case RATIO_TAP_CHANGER_TRANSFORMER -> v.getConnectableStream(TwoWindingsTransformer.class).anyMatch(TwoWindingsTransformer::hasRatioTapChanger);
                default -> v.getConnectableStream(type.connectableClass).findFirst().isPresent();
            };
        } else {
            return true; // network
        }
    }

    private CheckBoxTreeItem<Container<?>> createSubLevelCheckBoxTreeItem(Container<?> c, ObservableList<TreeItem<Container<?>>> treeItems, Set<String> selectedIds) {
        var cItem = createCheckBoxTreeItem(c, selectedIds);
        treeItems.add(cItem);
        return cItem;
    }

    private Function<Identifiable<?>, String> getIdentifiableStringSupplier() {
        return showNames.isSelected() ? Identifiable::getNameOrId : Identifiable::getId;
    }

    private void addListenerOnContainerItem(CheckBoxTreeItem<Container<?>> containerTreeItem) {
        containerTreeItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
            String cId = containerTreeItem.getValue().getId();
            if (Boolean.TRUE.equals(newVal)) {
                createCheckedTab(containerTreeItem);
                containersChecked.add(cId);
            } else {
                containersChecked.remove(cId);
            }
            saveSelectedDiagrams();
        });
    }

    public void createCheckedTab(CheckBoxTreeItem<Container<?>> containerTreeItem) {
        Container<?> container = containerTreeItem.getValue();
        String tabName = getIdentifiableStringSupplier().apply(container);
        nadViewController.createCheckedTab(model.getNetwork(), containerTreeItem, tabName);
        sldViewController.createCheckedTab(sldJsHandler, model.getNetwork(), containerTreeItem, tabName);
    }

    private void saveSelectedDiagrams() {
        try {
            String selectedVoltageLevelIdsPropertyValue = objectMapper.writeValueAsString(containersChecked);
            preferences.put(SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY, selectedVoltageLevelIdsPropertyValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Set<String> readSavedSelection() {
        Set<String> selectedIds = new HashSet<>();
        String selectedIdsPropertyValue = preferences.get(SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY, null);
        if (selectedIdsPropertyValue != null) {
            try {
                selectedIds = new HashSet<>(objectMapper.readValue(selectedIdsPropertyValue, new TypeReference<List<String>>() {
                }));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return selectedIds;
    }

    public void processExit(ActionEvent actionEvent) {
        actionEvent.consume();
        Platform.exit();
    }

    public void processOpen(ActionEvent actionEvent) {
        actionEvent.consume();
        loadFile(selectFile());
    }
}
