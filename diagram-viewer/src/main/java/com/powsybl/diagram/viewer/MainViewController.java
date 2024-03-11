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
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class MainViewController {

    public enum ComponentFilterType {
        ALL,
        HVDC_LINE,
        SWITCH,
        BUSBAR_SECTION,
        LINE,
        TIE_LINE,
        TWO_WINDINGS_TRANSFORMER,
        THREE_WINDINGS_TRANSFORMER,
        GENERATOR,
        BATTERY,
        LOAD,
        SHUNT_COMPENSATOR,
        DANGLING_LINE,
        STATIC_VAR_COMPENSATOR,
        LCC_CONVERTER_STATION,
        VSC_CONVERTER_STATION,
        GROUND;

        IdentifiableType toIidm() {
            return switch (this) {
                case ALL -> null;
                case HVDC_LINE -> IdentifiableType.HVDC_LINE;
                case SWITCH -> IdentifiableType.SWITCH;
                case BUSBAR_SECTION -> IdentifiableType.BUSBAR_SECTION;
                case LINE -> IdentifiableType.LINE;
                case TIE_LINE -> IdentifiableType.TIE_LINE;
                case TWO_WINDINGS_TRANSFORMER -> IdentifiableType.TWO_WINDINGS_TRANSFORMER;
                case THREE_WINDINGS_TRANSFORMER -> IdentifiableType.THREE_WINDINGS_TRANSFORMER;
                case GENERATOR -> IdentifiableType.GENERATOR;
                case BATTERY -> IdentifiableType.BATTERY;
                case LOAD -> IdentifiableType.LOAD;
                case SHUNT_COMPENSATOR -> IdentifiableType.SHUNT_COMPENSATOR;
                case DANGLING_LINE -> IdentifiableType.DANGLING_LINE;
                case STATIC_VAR_COMPENSATOR -> IdentifiableType.STATIC_VAR_COMPENSATOR;
                case VSC_CONVERTER_STATION, LCC_CONVERTER_STATION -> IdentifiableType.HVDC_CONVERTER_STATION;
                case GROUND -> IdentifiableType.GROUND;
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case ALL -> "All";
                case HVDC_LINE -> "HVDC line";
                case SWITCH -> "Switch";
                case BUSBAR_SECTION -> "Busbar section";
                case LINE -> "Line";
                case TIE_LINE -> "Tie line";
                case TWO_WINDINGS_TRANSFORMER -> " Two-winding transformer";
                case THREE_WINDINGS_TRANSFORMER -> "Three-winding transformer";
                case GENERATOR -> "Generator";
                case BATTERY -> "Battery";
                case LOAD -> "Load";
                case SHUNT_COMPENSATOR -> "Shunt compensator";
                case DANGLING_LINE -> "Dangling line";
                case STATIC_VAR_COMPENSATOR -> "SVC";
                case LCC_CONVERTER_STATION -> "LCC";
                case VSC_CONVERTER_STATION -> "VSC";
                case GROUND -> "Ground";
            };
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewController.class);

    private static final String SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY = "selectedVoltageLevelAndSubstationIds";
    public static final String CASE_PATH_PROPERTY = "casePath";
    public static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private final Preferences preferences = Preferences.userNodeForPackage(DiagramViewer.class);
    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    @FXML
    public TextField filePath;
    @FXML
    public TextField filterField;

    @FXML
    public ChoiceBox<ComponentFilterType> componentTypeFilterChoice;

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
        if (caseFolderPropertyValue != null) {
            fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
        }
        fileChooser.setTitle("Open case File");
        return fileChooser.showOpenDialog(loadingStatus.getScene().getWindow());
    }

    public void loadFile(File file) {
        if (file != null) {
            Service<Network> networkService = new Service<>() {
                @Override
                protected Task<Network> createTask() {
                    return new Task<>() {
                        @Override
                        protected Network call() {
                            Properties properties = new Properties();
                            properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
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
        Service<Network> networkService = new Service<>() {
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
//                item -> new Observable[] {
//                        filterField.textProperty(),
//                        componentTypeFilterChoice.valueProperty()
//                }
//        );
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
                componentTypeFilterChoice.valueProperty()));

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
        var container = item.getValue();
        if (StringUtils.isEmpty(filter)) {
            return containsComponentType(idType, container);
        } else {
            boolean filterOk = getIdentifiableStringSupplier().apply(container)
                    .toLowerCase(Locale.getDefault())
                    .contains(filter.toLowerCase(Locale.getDefault()));
            return (filterOk || item.getChildren().stream().anyMatch(this::treeItemFilter))
                    && containsComponentType(idType, container);
        }
    }

    private static boolean containsComponentType(ComponentFilterType type, Container<?> container) {
        boolean result = false;
        if (container instanceof Substation s) {
            result = s.getVoltageLevelStream().anyMatch(v -> containsComponentType(type, v));
        } else if (container instanceof VoltageLevel v) {
            result = switch (type) {
                case HVDC_LINE, BUSBAR_SECTION, LINE, TIE_LINE, TWO_WINDINGS_TRANSFORMER, THREE_WINDINGS_TRANSFORMER, GENERATOR, BATTERY, LOAD, SHUNT_COMPENSATOR, DANGLING_LINE, STATIC_VAR_COMPENSATOR, GROUND -> v.getConnectableStream().anyMatch(c -> c.getType() == type.toIidm());
                case LCC_CONVERTER_STATION -> v.getLccConverterStationCount() != 0;
                case VSC_CONVERTER_STATION -> v.getVscConverterStationCount() != 0;
                case SWITCH -> v.getSwitchCount() != 0;
                case ALL -> true;
            };
        }
        return result;
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
