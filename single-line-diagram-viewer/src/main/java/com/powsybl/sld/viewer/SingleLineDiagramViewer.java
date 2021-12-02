/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.viewer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.sld.GraphBuilder;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.SubstationDiagram;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.cgmes.dl.iidm.extensions.NetworkDiagramData;
import com.powsybl.sld.cgmes.layout.CgmesSubstationLayoutFactory;
import com.powsybl.sld.cgmes.layout.CgmesVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.layout.positionbyclustering.PositionByClustering;
import com.powsybl.sld.layout.positionfromextension.PositionFromExtension;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.*;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 */
public class SingleLineDiagramViewer extends Application implements DisplayVoltageLevel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramViewer.class);

    private static final String SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY = "selectedVoltageLevelIds";
    private static final String SELECTED_SUBSTATION_IDS_PROPERTY = "selectedSubstationIds";
    private static final String CASE_PATH_PROPERTY = "casePath";
    private static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private final Map<String, VoltageLevelLayoutFactory> voltageLevelsLayouts = new LinkedHashMap<>();

    private final Map<String, DiagramStyleProvider> styles = new LinkedHashMap<>();

    private final Map<String, SubstationLayoutFactory> substationsLayouts = new LinkedHashMap<>();

    private final Map<String, ComponentLibrary> svgLibraries
            = ComponentLibrary.findAll().stream().collect(Collectors.toMap(ComponentLibrary::getName, Function.identity()));

    private final ObservableList<SelectableSubstation> selectableSubstations = FXCollections.observableArrayList();

    private final ObservableList<SelectableVoltageLevel> selectableVoltageLevels = FXCollections.observableArrayList();

    private final TextField filterInput = new TextField();

    private final TreeView<Container> substationsTree = new TreeView<>();

    private final TabPane diagramsPane = new TabPane();
    private Tab tabSelected;
    private Tab tabChecked;
    private final BorderPane selectedDiagramPane = new BorderPane();
    private final TabPane checkedDiagramsPane = new TabPane();
    private GridPane parametersPane;
    private final Button caseLoadingStatus = new Button("  ");
    private final TextField casePathTextField = new TextField();

    private final ObjectProperty<Network> networkProperty = new SimpleObjectProperty<>();

    private final ObjectProperty<LayoutParameters> layoutParameters = new SimpleObjectProperty<>(new LayoutParameters()
            .setShowGrid(true)
            .setAdaptCellHeightToContent(true));

    protected final Preferences preferences = Preferences.userNodeForPackage(SingleLineDiagramViewer.class);

    private final ObjectMapper objectMapper = JsonUtil.createObjectMapper();

    protected final ComboBox<String> voltageLevelLayoutComboBox = new ComboBox<>();

    private final ComboBox<String> substationLayoutComboBox = new ComboBox<>();

    private final ComboBox<String> styleComboBox = new ComboBox<>();

    private final ComboBox<String> svgLibraryComboBox = new ComboBox<>();

    private final CheckBox showNames = new CheckBox("Show names");

    private final CheckBox hideSubstations = new CheckBox("Hide substations");

    private final CheckBox hideVoltageLevels = new CheckBox("Hide voltage levels");

    private final ComboBox<String> diagramNamesComboBox = new ComboBox<>();

    private class ContainerDiagramPane extends BorderPane {
        private final WebView diagramView = new WebView();

        private final TextArea infoArea = new TextArea();

        private final VBox svgArea = new VBox();
        private final TextField svgSearchField = new TextField();
        private final Button svgSearchButton = new Button("Search");
        private final TextArea svgTextArea = new TextArea();
        private AtomicReference<Integer> svgSearchStart = new AtomicReference<>(0);
        private final Button svgSaveButton = new Button("Save");

        private final VBox metadataArea = new VBox();
        private final TextField metadataSearchField = new TextField();
        private final Button metadataSearchButton = new Button("Search");
        private final TextArea metadataTextArea = new TextArea();
        private final AtomicReference<Integer> metadataSearchStart = new AtomicReference<>(0);
        private final Button metadataSaveButton = new Button("Save");

        private final VBox jsonArea = new VBox();
        private final TextField jsonSearchField = new TextField();
        private final Button jsonSearchButton = new Button("Search");
        private final TextArea jsonTextArea = new TextArea();
        private final AtomicReference<Integer> jsonSearchStart = new AtomicReference<>(0);
        private final Button jsonSaveButton = new Button("Save");

        private final Tab tab1 = new Tab("Diagram", diagramView);

        private final Tab tab2 = new Tab("SVG", svgArea);

        private final Tab tab3 = new Tab("Metadata", metadataArea);

        private final Tab tab4 = new Tab("Graph", jsonArea);

        private final TabPane tabPane = new TabPane(tab1, tab2, tab3, tab4);

        private final TitledPane titledPane = new TitledPane("Infos", infoArea);

        private final ChangeListener<LayoutParameters> listener;

        ContainerDiagramPane(Container c) {
            createArea(svgSearchField, svgSearchButton, svgSaveButton, "SVG file", "*.svg", svgTextArea, svgArea, svgSearchStart);
            createArea(metadataSearchField, metadataSearchButton, metadataSaveButton, "JSON file", "*.json", metadataTextArea, metadataArea, metadataSearchStart);
            createArea(jsonSearchField, jsonSearchButton, jsonSaveButton, "JSON file", "*.json", jsonTextArea, jsonArea, jsonSearchStart);

            infoArea.setEditable(false);
            infoArea.setText(String.join(System.lineSeparator(),
                    "id: " + c.getId(),
                    "name: " + c.getName()));
            tabPane.setSide(Side.BOTTOM);
            tab1.setClosable(false);
            tab2.setClosable(false);
            tab3.setClosable(false);
            tab4.setClosable(false);
            setCenter(tabPane);
            setBottom(titledPane);
            listener = (observable, oldValue, newValue) -> loadDiagram(c);
            layoutParameters.addListener(new WeakChangeListener<>(listener));
            loadDiagram(c);

            // Add Zoom management
            diagramView.addEventFilter(ScrollEvent.SCROLL, (ScrollEvent e) -> {
                if (e.isControlDown()) {
                    double deltaY = e.getDeltaY();
                    double zoom = diagramView.getZoom();
                    if (deltaY < 0) {
                        zoom /= 1.1;
                    } else if (deltaY > 0) {
                        zoom *= 1.1;
                    }
                    diagramView.setZoom(zoom);
                    e.consume();
                }
            });
            // Avoid the useless right click on the image
            diagramView.setContextMenuEnabled(false);
        }

        class ContainerDiagramResult {

            private final String svgData;

            private final String metadataData;

            private final String jsonData;

            ContainerDiagramResult(String svgData, String metadataData, String jsonData) {
                this.svgData = svgData;
                this.metadataData = metadataData;
                this.jsonData = jsonData;
            }

            String getSvgData() {
                return svgData;
            }

            String getMetadataData() {
                return metadataData;
            }

            String getJsonData() {
                return jsonData;
            }
        }

        private WebView getDiagramView() {
            return diagramView;
        }

        private String getSelectedDiagramName() {
            return diagramNamesComboBox.getSelectionModel().getSelectedItem();
        }

        private ContainerDiagramResult createContainerDiagramView(WebView diagramView, Container c) {
            String svgData;
            String metadataData;
            String jsonData;
            try (StringWriter svgWriter = new StringWriter();
                 StringWriter metadataWriter = new StringWriter();
                 StringWriter jsonWriter = new StringWriter()) {
                DiagramStyleProvider styleProvider = styles.get(styleComboBox.getSelectionModel().getSelectedItem());

                String dName = getSelectedDiagramName();
                LayoutParameters diagramLayoutParameters = new LayoutParameters(layoutParameters.get()).setDiagramName(dName)
                        .setCssLocation(LayoutParameters.CssLocation.INSERTED_IN_SVG)
                        .setSvgWidthAndHeightAdded(true);

                DiagramLabelProvider initProvider = new DefaultDiagramLabelProvider(networkProperty.get(), getComponentLibrary(), diagramLayoutParameters);
                GraphBuilder graphBuilder = new NetworkGraphBuilder(networkProperty.get());

                if (c.getContainerType() == ContainerType.VOLTAGE_LEVEL) {
                    VoltageLevelDiagram diagram = VoltageLevelDiagram.build(graphBuilder, c.getId(), getVoltageLevelLayoutFactory(), showNames.isSelected());
                    diagram.writeSvg("",
                            new DefaultSVGWriter(getComponentLibrary(), diagramLayoutParameters),
                            initProvider,
                            styleProvider,
                            svgWriter,
                            metadataWriter);
                    diagram.getGraph().writeJson(jsonWriter);
                } else if (c.getContainerType() == ContainerType.SUBSTATION) {
                    SubstationDiagram diagram = SubstationDiagram.build(graphBuilder, c.getId(), getSubstationLayoutFactory(), getVoltageLevelLayoutFactory(), showNames.isSelected());
                    diagram.writeSvg("",
                            new DefaultSVGWriter(getComponentLibrary(), diagramLayoutParameters),
                            initProvider,
                            styleProvider,
                            svgWriter,
                            metadataWriter);
                    diagram.getSubGraph().writeJson(jsonWriter);
                }

                svgWriter.flush();
                metadataWriter.flush();
                svgData = svgWriter.toString();
                metadataData = metadataWriter.toString();
                jsonData = jsonWriter.toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            loadContent(diagramView, svgData);

            return new ContainerDiagramResult(svgData, metadataData, jsonData);
        }

        public void loadContent(WebView diagramView, String svg) {
            // Need to set HTML body margin to 0 to avoid margin around SVG displayed
            String content = "<html>\n" +
                    "<body style='margin: 0'>" +
                    svg +
                    "</div></body></html>";
            diagramView.getEngine().loadContent(content);
        }

        private void loadDiagram(Container c) {
            ContainerDiagramResult result = createContainerDiagramView(diagramView, c);

            svgTextArea.setText(result.getSvgData());
            metadataTextArea.setText(result.getMetadataData());
            jsonTextArea.setText(result.getJsonData());
        }

        private ComponentLibrary getComponentLibrary() {
            String selectedItem = svgLibraryComboBox.getSelectionModel().getSelectedItem();
            return svgLibraries.get(selectedItem);
        }

        private SubstationLayoutFactory getSubstationLayoutFactory() {
            String selectedItem = substationLayoutComboBox.getSelectionModel().getSelectedItem();
            return substationsLayouts.get(selectedItem);
        }

        private void createArea(TextField searchField, Button searchButton, Button saveButton,
                                String descrSave, String extensionSave,
                                TextArea textArea, VBox area,
                                AtomicReference<Integer> searchStart) {
            HBox searchBox = new HBox();
            searchBox.setSpacing(20);
            searchBox.setPadding(new Insets(10));
            searchField.setPrefColumnCount(35);
            searchBox.getChildren().add(searchField);
            searchBox.getChildren().add(searchButton);
            searchBox.getChildren().add(saveButton);

            searchStart.set(0);
            searchButton.setOnAction(evh -> {
                String txtPattern = searchField.getText();
                Pattern pattern = Pattern.compile(txtPattern);
                Matcher matcher = pattern.matcher(textArea.getText());
                boolean found = matcher.find(searchStart.get());
                if (found) {
                    textArea.selectRange(matcher.start(), matcher.end());
                    searchStart.set(matcher.end());
                } else {
                    textArea.deselect();
                    searchStart.set(0);
                    found = matcher.find(searchStart.get());
                    if (found) {
                        textArea.selectRange(matcher.start(), matcher.end());
                        searchStart.set(matcher.end());
                    }
                }
            });
            searchField.textProperty().addListener((observable, oldValue, newValue) ->
                    searchStart.set(0)
            );

            saveButton.setOnAction(evh -> {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(descrSave, extensionSave);
                fileChooser.getExtensionFilters().add(extFilter);
                File file = fileChooser.showSaveDialog(getScene().getWindow());

                if (file != null) {
                    try {
                        PrintWriter writer;
                        writer = new PrintWriter(file);
                        writer.println(textArea.getText());
                        writer.close();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
            });

            area.setSpacing(8);
            area.getChildren().add(searchBox);
            area.getChildren().add(textArea);
            VBox.setVgrow(searchBox, Priority.NEVER);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            textArea.setEditable(false);
        }
    }

    abstract class AbstractSelectableContainer {

        protected final String id;

        protected final String name;

        protected final BooleanProperty checkedProperty = new SimpleBooleanProperty();

        protected boolean saveDiagrams = true;

        AbstractSelectableContainer(String id, String name) {
            this.id = id;
            this.name = name;
            checkedProperty.addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    addDiagramTab();
                } else {
                    removeDiagramTab();
                }
                if (saveDiagrams) {
                    saveSelectedDiagrams();
                }
            });
        }

        private void removeDiagramTab() {
            checkedDiagramsPane.getTabs().removeIf(tab -> tab.getText().equals(id));
        }

        abstract void addDiagramTab();

        protected String getId() {
            return id;
        }

        protected String getIdOrName() {
            return showNames.isSelected() ? name : id;
        }

        public BooleanProperty checkedProperty() {
            return checkedProperty;
        }

        public void setCheckedProperty(Boolean b) {
            checkedProperty.setValue(b);
        }

        public void setSaveDiagrams(boolean saveDiagrams) {
            this.saveDiagrams = saveDiagrams;
        }

        @Override
        public String toString() {
            return getIdOrName();
        }
    }

    private class SelectableVoltageLevel extends AbstractSelectableContainer {

        SelectableVoltageLevel(String id, String name) {
            super(id, name);
        }

        @Override
        protected void addDiagramTab() {
            VoltageLevel vl = networkProperty.get().getVoltageLevel(id);
            if (vl != null) {
                Tab tab = new Tab(id, new ContainerDiagramPane(vl));
                tab.setTooltip(new Tooltip(vl.getName()));
                tab.setOnCloseRequest(e -> closeTab());

                ContextMenu menu = new ContextMenu();
                MenuItem itemCloseTab = new MenuItem("Close tab");
                itemCloseTab.setOnAction(e -> closeTab());
                MenuItem itemCloseAllTabs = new MenuItem("Close all tabs");
                itemCloseAllTabs.setOnAction(e -> closeAllTabs());

                menu.getItems().add(itemCloseTab);
                menu.getItems().add(itemCloseAllTabs);
                tab.setContextMenu(menu);
                checkedDiagramsPane.getTabs().add(tab);
                checkedDiagramsPane.getSelectionModel().select(tab);
            } else {
                LOGGER.warn("Voltage level {} not found", id);
            }
        }

        public void closeTab() {
            checkedProperty.set(false);
            checkvItemTree(id, false);
        }
    }

    private class SelectableSubstation extends AbstractSelectableContainer {
        SelectableSubstation(String id, String name) {
            super(id, name);
        }

        @Override
        protected void addDiagramTab() {
            Substation s = networkProperty.get().getSubstation(id);
            if (s != null) {
                Tab tab = new Tab(id, new ContainerDiagramPane(s));
                tab.setTooltip(new Tooltip(s.getName()));
                tab.setOnCloseRequest(e -> closeTab());

                ContextMenu menu = new ContextMenu();
                MenuItem itemCloseTab = new MenuItem("Close tab");
                itemCloseTab.setOnAction(e -> closeTab());

                MenuItem itemCloseAllTabs = new MenuItem("Close all tabs");
                itemCloseAllTabs.setOnAction(e -> closeAllTabs());

                menu.getItems().add(itemCloseTab);
                menu.getItems().add(itemCloseAllTabs);
                tab.setContextMenu(menu);
                checkedDiagramsPane.getTabs().add(tab);
                checkedDiagramsPane.getSelectionModel().select(tab);
            } else {
                LOGGER.warn("Substation {} not found", id);
            }
        }

        private void checksItemTree(String id, boolean selected) {
            substationsTree.getRoot().getChildren().stream().forEach(child -> {
                if (child.getValue().getId().equals(id)) {
                    ((CheckBoxTreeItem) child).setSelected(selected);
                }
            });
        }

        public void closeTab() {
            checkedProperty.set(false);
            checksItemTree(id, false);
        }
    }

    private VoltageLevelLayoutFactory getVoltageLevelLayoutFactory() {
        String selectedItem = voltageLevelLayoutComboBox.getSelectionModel().getSelectedItem();
        return voltageLevelsLayouts.get(selectedItem);
    }

    private void setParameters(LayoutParameters layoutParameters) {
        this.layoutParameters.set(new LayoutParameters(layoutParameters));
    }

    private void addSpinner(String label, double min, double max, double amountToStepBy, int row,
                            ToDoubleFunction<LayoutParameters> initializer,
                            BiFunction<LayoutParameters, Double, LayoutParameters> updater) {
        Spinner<Double> spinner = new Spinner<>(min, max, initializer.applyAsDouble(layoutParameters.get()), amountToStepBy);
        spinner.setEditable(true);
        spinner.valueProperty().addListener((observable, oldValue, newValue) -> setParameters(updater.apply(layoutParameters.get(), newValue)));
        parametersPane.add(new Label(label), 0, row);
        parametersPane.add(spinner, 0, row + 1);
    }

    private void addCheckBox(String label, int row,
                             Predicate<LayoutParameters> initializer,
                             BiFunction<LayoutParameters, Boolean, LayoutParameters> updater) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(initializer.test(layoutParameters.get()));
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> setParameters(updater.apply(layoutParameters.get(), newValue)));
        parametersPane.add(cb, 0, row);
    }

    private void initPositionLayoutCheckBox(Predicate<PositionVoltageLevelLayoutFactory> initializer, CheckBox stackCb) {
        VoltageLevelLayoutFactory layoutFactory = getVoltageLevelLayoutFactory();
        stackCb.setSelected(layoutFactory instanceof PositionVoltageLevelLayoutFactory && initializer.test((PositionVoltageLevelLayoutFactory) layoutFactory));
        stackCb.setDisable(!(layoutFactory instanceof PositionVoltageLevelLayoutFactory));
    }

    private void addPositionLayoutCheckBox(String label, int rowIndex, Predicate<PositionVoltageLevelLayoutFactory> initializer,
                                           BiFunction<PositionVoltageLevelLayoutFactory, Boolean, PositionVoltageLevelLayoutFactory> updater) {
        CheckBox stackCb = new CheckBox(label);
        initPositionLayoutCheckBox(initializer, stackCb);
        voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> initPositionLayoutCheckBox(initializer, stackCb));
        stackCb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            VoltageLevelLayoutFactory layoutFactory = getVoltageLevelLayoutFactory();
            if (layoutFactory instanceof PositionVoltageLevelLayoutFactory) {
                updater.apply((PositionVoltageLevelLayoutFactory) layoutFactory, newValue);
                // just to trigger diagram update
                refreshDiagram();
            }
        });

        parametersPane.add(stackCb, 0, rowIndex);
    }

    private void createParametersPane() {
        parametersPane = new GridPane();
        parametersPane.setHgap(5);
        parametersPane.setVgap(5);
        parametersPane.setPadding(new Insets(5, 5, 5, 5));

        int rowIndex = 0;

        Button fitToContent = new Button("Fit to content");
        fitToContent.setOnAction(event -> {
            ContainerDiagramPane pane = getContainerDiagramPane();
            if (pane != null) {
                String svgData = pane.svgTextArea.getText();
                Optional<String> svgLine = svgData.lines().filter(l -> l.contains("<svg")).findAny();
                if (svgLine.isPresent()) {
                    String valuePattern = "\"([^\"]*)\"";
                    Pattern pH = Pattern.compile("height=" + valuePattern);
                    Matcher mH = pH.matcher(svgLine.get());
                    Pattern pW = Pattern.compile("width=" + valuePattern);
                    Matcher mW = pW.matcher(svgLine.get());
                    if (mH.find() && mW.find()) {
                        double svgHeight = Double.parseDouble(mH.group(1));
                        double svgWidth = Double.parseDouble(mW.group(1));
                        double paneHeight = pane.getDiagramView().heightProperty().get();
                        double paneWidth = pane.getDiagramView().widthProperty().get();
                        if (paneHeight < svgHeight || paneWidth < svgWidth) {
                            double zoomH = paneHeight / svgHeight;
                            double zoomW = paneWidth / svgWidth;
                            pane.getDiagramView().setZoom(Math.min(zoomH, zoomW));
                        }
                    }
                }
            }
        });

        Button resetZoom = new Button("Reset zoom");
        resetZoom.setOnAction(event -> {
            ContainerDiagramPane pane = getContainerDiagramPane();
            if (pane != null) {
                pane.getDiagramView().setZoom(1.0); // 100%
            }
        });
        FlowPane buttonsPane = new FlowPane(fitToContent, resetZoom);
        buttonsPane.setHgap(10);
        parametersPane.add(buttonsPane, 0, rowIndex++);

        // svg library list
        svgLibraryComboBox.getItems().addAll(svgLibraries.keySet());
        svgLibraryComboBox.getSelectionModel().selectFirst();
        svgLibraryComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Design:"), 0, rowIndex++);
        parametersPane.add(svgLibraryComboBox, 0, rowIndex++);

        styleComboBox.getItems().addAll(styles.keySet());
        styleComboBox.getSelectionModel().select(1);
        styleComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Style:"), 0, rowIndex++);
        parametersPane.add(styleComboBox, 0, rowIndex++);

        // substation layout list
        substationLayoutComboBox.getItems().addAll(substationsLayouts.keySet());
        substationLayoutComboBox.getSelectionModel().selectFirst();
        substationLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Substation Layout:"), 0, rowIndex++);
        parametersPane.add(substationLayoutComboBox, 0, rowIndex++);

        // voltageLevel layout list
        voltageLevelLayoutComboBox.getItems().addAll(voltageLevelsLayouts.keySet());
        voltageLevelLayoutComboBox.getSelectionModel().selectFirst();
        voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());

        parametersPane.add(new Label("VoltageLevel Layout:"), 0, rowIndex++);
        parametersPane.add(voltageLevelLayoutComboBox, 0, rowIndex++);

        //CGMES-DL diagrams names list
        parametersPane.add(new Label("CGMES-DL Diagrams:"), 0, ++rowIndex);
        parametersPane.add(diagramNamesComboBox, 0, ++rowIndex);
        diagramNamesComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        diagramNamesComboBox.setDisable(true);
        voltageLevelLayoutComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setDiagramsNamesContent(networkProperty.get(), false));
        rowIndex += 1;

        addSpinner("Diagram padding top/bottom:", 0, 300, 5, rowIndex, lp -> lp.getDiagramPadding().getTop(), (lp, value) -> lp.setDiagrammPadding(lp.getDiagramPadding().getLeft(), value, lp.getDiagramPadding().getRight(), value));
        rowIndex += 2;
        addSpinner("Diagram padding left/right:", 0, 300, 5, rowIndex, lp -> lp.getDiagramPadding().getLeft(), (lp, value) -> lp.setDiagrammPadding(value, lp.getDiagramPadding().getTop(), value, lp.getDiagramPadding().getBottom()));
        rowIndex += 2;
        addSpinner("Voltage padding top/bottom:", 0, 300, 5, rowIndex, lp -> lp.getVoltageLevelPadding().getTop(), (lp, value) -> lp.setVoltageLevelPadding(lp.getVoltageLevelPadding().getLeft(), value, lp.getVoltageLevelPadding().getRight(), value));
        rowIndex += 2;
        addSpinner("Voltage padding left/right:", 0, 300, 5, rowIndex, lp -> lp.getVoltageLevelPadding().getLeft(), (lp, value) -> lp.setVoltageLevelPadding(value, lp.getVoltageLevelPadding().getTop(), value, lp.getVoltageLevelPadding().getBottom()));
        rowIndex += 2;
        addSpinner("Busbar vertical space:", 10, 100, 5, rowIndex, LayoutParameters::getVerticalSpaceBus, LayoutParameters::setVerticalSpaceBus);
        rowIndex += 2;
        addSpinner("Horizontal busbar padding:", 10, 100, 5, rowIndex, LayoutParameters::getHorizontalBusPadding, LayoutParameters::setHorizontalBusPadding);
        rowIndex += 2;
        addSpinner("Cell width:", 10, 100, 5, rowIndex, LayoutParameters::getCellWidth, LayoutParameters::setCellWidth);
        rowIndex += 2;
        addSpinner("Extern cell height:", 100, 500, 10, rowIndex, LayoutParameters::getExternCellHeight, LayoutParameters::setExternCellHeight);
        rowIndex += 2;
        addSpinner("Intern cell height:", 10, 100, 5, rowIndex, LayoutParameters::getInternCellHeight, LayoutParameters::setInternCellHeight);
        rowIndex += 2;
        addSpinner("Stack height:", 10, 100, 5, rowIndex, LayoutParameters::getStackHeight, LayoutParameters::setStackHeight);
        rowIndex += 2;
        addCheckBox("Show grid", rowIndex, LayoutParameters::isShowGrid, LayoutParameters::setShowGrid);
        rowIndex += 1;
        addCheckBox("Add svg tooltip", rowIndex, LayoutParameters::isTooltipEnabled, LayoutParameters::setTooltipEnabled);
        rowIndex += 1;
        addCheckBox("Show internal nodes", rowIndex, LayoutParameters::isShowInternalNodes, LayoutParameters::setShowInternalNodes);
        rowIndex += 1;
        addCheckBox("Draw straight wires", rowIndex, LayoutParameters::isDrawStraightWires, LayoutParameters::setDrawStraightWires);
        rowIndex += 1;
        addPositionLayoutCheckBox("Stack feeders", rowIndex, PositionVoltageLevelLayoutFactory::isFeederStacked, PositionVoltageLevelLayoutFactory::setFeederStacked);
        rowIndex += 1;
        addPositionLayoutCheckBox("Exception when pattern unhandled", rowIndex, PositionVoltageLevelLayoutFactory::isExceptionIfPatternNotHandled, PositionVoltageLevelLayoutFactory::setExceptionIfPatternNotHandled);
        rowIndex += 1;
        addPositionLayoutCheckBox("Handle shunts", rowIndex, PositionVoltageLevelLayoutFactory::isHandleShunts, PositionVoltageLevelLayoutFactory::setHandleShunts);
        rowIndex += 1;
        addPositionLayoutCheckBox("Remove fictitious nodes", rowIndex, PositionVoltageLevelLayoutFactory::isRemoveUnnecessaryFictitiousNodes, PositionVoltageLevelLayoutFactory::setRemoveUnnecessaryFictitiousNodes);
        rowIndex += 1;
        addPositionLayoutCheckBox("Substitute singular fictitious nodes", rowIndex, PositionVoltageLevelLayoutFactory::isSubstituteSingularFictitiousByFeederNode, PositionVoltageLevelLayoutFactory::setSubstituteSingularFictitiousByFeederNode);
        rowIndex += 2;
        addSpinner("Scale factor:", 1, 20, 1, rowIndex, LayoutParameters::getScaleFactor, LayoutParameters::setScaleFactor);
        rowIndex += 2;
        addSpinner("Arrows distance:", 0, 200, 1, rowIndex, LayoutParameters::getArrowDistance, LayoutParameters::setArrowDistance);
        rowIndex += 2;
        addCheckBox("Avoid SVG components duplication", rowIndex, LayoutParameters::isAvoidSVGComponentsDuplication, LayoutParameters::setAvoidSVGComponentsDuplication);

        rowIndex += 1;
        addCheckBox("Adapt cell height to content", rowIndex, LayoutParameters::isAdaptCellHeightToContent, LayoutParameters::setAdaptCellHeightToContent);
        rowIndex += 2;
        addSpinner("Min space between components:", 8, 60, 1, rowIndex, LayoutParameters::getMinSpaceBetweenComponents, LayoutParameters::setMinSpaceBetweenComponents);
        rowIndex += 2;
        addSpinner("Minimum extern cell height:", 80, 300, 10, rowIndex, LayoutParameters::getMinExternCellHeight, LayoutParameters::setMinExternCellHeight);

        rowIndex += 2;
        addCheckBox("Center label:", rowIndex, LayoutParameters::isLabelCentered, LayoutParameters::setLabelCentered);
        rowIndex += 2;
        addCheckBox("Label diagonal:", rowIndex, LayoutParameters::isLabelDiagonal, LayoutParameters::setLabelDiagonal);
        rowIndex += 2;
        addSpinner("Angle Label:", -360, 360, 1, rowIndex, LayoutParameters::getAngleLabelShift, LayoutParameters::setAngleLabelShift);

        rowIndex += 2;
        addCheckBox("HighLight line state", rowIndex, LayoutParameters::isHighlightLineState, LayoutParameters::setHighlightLineState);
        rowIndex += 2;
        addCheckBox("Add nodes infos", rowIndex, LayoutParameters::isAddNodesInfos, LayoutParameters::setAddNodesInfos);
        rowIndex += 2;
        addCheckBox("Feeder arrow symmetry", rowIndex, LayoutParameters::isFeederArrowSymmetry, LayoutParameters::setFeederArrowSymmetry);
    }

    private ContainerDiagramPane getContainerDiagramPane() {
        ContainerDiagramPane pane = null;
        Tab tab = diagramsPane.getSelectionModel().getSelectedItem();
        if (tab != null) {
            if (tab == tabChecked) {
                if (checkedDiagramsPane.getSelectionModel().getSelectedItem() != null) {
                    pane = (ContainerDiagramPane) checkedDiagramsPane.getSelectionModel().getSelectedItem().getContent();
                }
            } else {
                pane = (ContainerDiagramPane) selectedDiagramPane.getCenter();
            }
        }
        return pane;
    }

    private void setDiagramsNamesContent(Network network, boolean setValues) {
        if (network != null && NetworkDiagramData.checkNetworkDiagramData(network)) {
            if (setValues) {
                diagramNamesComboBox.getItems().setAll(NetworkDiagramData.getDiagramsNames(network));
                diagramNamesComboBox.getSelectionModel().clearSelection();
                diagramNamesComboBox.setValue(null);
            }
            diagramNamesComboBox.setDisable(!(getVoltageLevelLayoutFactory() instanceof CgmesVoltageLevelLayoutFactory));
        } else {
            diagramNamesComboBox.getItems().clear();
            diagramNamesComboBox.setDisable(true);
        }
    }

    private void refreshDiagram() {
        layoutParameters.set(new LayoutParameters(layoutParameters.get()));
    }

    private void loadSelectedVoltageLevelsDiagrams() {
        String selectedIdsPropertyValue = preferences.get(SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY, null);
        if (selectedIdsPropertyValue != null) {
            try {
                Set<String> selectedIds = new HashSet<>(objectMapper.readValue(selectedIdsPropertyValue, new TypeReference<List<String>>() {
                }));
                selectableVoltageLevels.stream()
                        .filter(selectableObject -> selectedIds.contains(selectableObject.getId()))
                        .forEach(selectableVoltageLevel -> {
                            selectableVoltageLevel.setSaveDiagrams(false);
                            selectableVoltageLevel.checkedProperty().set(true);
                            selectableVoltageLevel.setSaveDiagrams(true);
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void loadSelectedSubstationsDiagrams() {
        String selectedPropertyValue = preferences.get(SELECTED_SUBSTATION_IDS_PROPERTY, null);

        if (selectedPropertyValue != null) {
            try {
                Set<String> selectedSubstationIds = new HashSet<>(objectMapper.readValue(selectedPropertyValue, new TypeReference<List<String>>() {
                }));
                selectableSubstations.stream()
                        .filter(selectableSubstation -> selectedSubstationIds.contains(selectableSubstation.getId()))
                        .forEach(selectableSubstation -> {
                            selectableSubstation.setSaveDiagrams(false);
                            selectableSubstation.checkedProperty().set(true);
                            selectableSubstation.setSaveDiagrams(true);
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /*
        Handling the display of names/id in the substations tree
     */
    private void initTreeCellFactory() {
        substationsTree.setCellFactory(param -> {
            CheckBoxTreeCell<Container> treeCell = new CheckBoxTreeCell<>();
            StringConverter<TreeItem<Container>> strConvert = new StringConverter<TreeItem<Container>>() {
                @Override
                public String toString(TreeItem<Container> c) {
                    if (c.getValue() != null) {
                        return getString(c.getValue());
                    } else {
                        return "";
                    }
                }

                @Override
                public TreeItem<Container> fromString(String string) {
                    return null;
                }
            };
            treeCell.setConverter(strConvert);
            return treeCell;
        });
    }

    private String getString(Container<?> value) {
        String cNameOrId = showNames.isSelected() ? value.getNameOrId() : value.getId();
        if (value instanceof Substation && hideVoltageLevels.isSelected()) {
            long nbVoltageLevels = ((Substation) value).getVoltageLevelStream().count();
            return cNameOrId + " [" + nbVoltageLevels + "]";
        }
        return cNameOrId;
    }

    @Override
    public void start(Stage primaryStage) {
        initLayoutsFactory();
        initStylesProvider();

        initTreeCellFactory();

        showNames.setSelected(true);
        showNames.selectedProperty().addListener((observable, oldValue, newValue) -> {
            substationsTree.refresh();
            refreshDiagram();
        });

        hideSubstations.selectedProperty().addListener((observable, oldValue, newValue) -> {
            initSubstationsTree();
            substationsTree.refresh();
        });

        hideVoltageLevels.selectedProperty().addListener((observable, oldValue, newValue) -> {
            initSubstationsTree();
            substationsTree.refresh();
        });

        filterInput.textProperty().addListener(obs ->
                initSubstationsTree()
        );

        // handling the change of the network
        networkProperty.addListener((observable, oldNetwork, newNetwork) -> {
            if (newNetwork == null) {
                selectableVoltageLevels.clear();
                selectableSubstations.clear();
            } else {
                selectableVoltageLevels.setAll(newNetwork.getVoltageLevelStream()
                        .map(vl -> new SelectableVoltageLevel(vl.getId(), vl.getName()))
                        .collect(Collectors.toList()));
                selectableSubstations.setAll(newNetwork.getSubstationStream()
                        .map(s -> new SelectableSubstation(s.getId(), s.getName()))
                        .collect(Collectors.toList()));
            }
        });
        diagramsPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabSelected = new Tab("Selected", selectedDiagramPane);
        tabChecked = new Tab("Checked", checkedDiagramsPane);
        diagramsPane.getTabs().setAll(tabSelected, tabChecked);

        createParametersPane();

        BorderPane voltageLevelPane = new BorderPane();
        Label filterLabel = new Label("Filter:");
        filterLabel.setMinWidth(40);
        GridPane voltageLevelToolBar = new GridPane();
        voltageLevelToolBar.setHgap(5);
        voltageLevelToolBar.setVgap(5);
        voltageLevelToolBar.setPadding(new Insets(5, 5, 5, 5));
        voltageLevelToolBar.add(showNames, 0, 0, 2, 1);
        voltageLevelToolBar.add(hideSubstations, 0, 1, 2, 1);
        voltageLevelToolBar.add(hideVoltageLevels, 0, 2, 2, 1);
        voltageLevelToolBar.add(filterLabel, 0, 3);
        voltageLevelToolBar.add(filterInput, 1, 3);
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        voltageLevelToolBar.getColumnConstraints().addAll(c0, c1);
        voltageLevelPane.setTop(voltageLevelToolBar);
        voltageLevelPane.setCenter(substationsTree);

        SplitPane splitPane = new SplitPane(voltageLevelPane, diagramsPane, new ScrollPane(parametersPane));
        splitPane.setDividerPositions(0.2, 0.7, 0.1);

        Node casePane = createCasePane(primaryStage);
        BorderPane.setMargin(casePane, new Insets(3, 3, 3, 3));
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(splitPane);
        mainPane.setTop(casePane);

        // selected voltegeLevels diagrams reloading
        selectableVoltageLevels.addListener(new ListChangeListener<SelectableVoltageLevel>() {
            @Override
            public void onChanged(Change<? extends SelectableVoltageLevel> c) {
                loadSelectedVoltageLevelsDiagrams();
                selectableVoltageLevels.remove(this);
            }
        });

        // selected substation diagrams reloading
        selectableSubstations.addListener(new ListChangeListener<SelectableSubstation>() {
            @Override
            public void onChanged(Change<? extends SelectableSubstation> c) {
                loadSelectedSubstationsDiagrams();
                selectableSubstations.remove(this);
            }
        });

        // Handling selection of a substation or a voltageLevel in the substations tree
        substationsTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Container>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<Container>> observable, TreeItem<Container> oldValue, TreeItem<Container> newValue) {
                if (newValue == null) {
                    return;
                }
                Container c = newValue.getValue();
                selectedDiagramPane.setCenter(new ContainerDiagramPane(c));
            }
        });

        // case reloading
        loadNetworkFromPreferences();

        Scene scene = new Scene(mainPane, 1000, 800);
        primaryStage.setTitle("Substation diagram viewer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadNetwork(Path file) {
        Service<Network> networkService = new Service<Network>() {
            @Override
            protected Task<Network> createTask() {
                return new Task<Network>() {
                    @Override
                    protected Network call() {
                        Properties properties = new Properties();
                        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
                        return Importers.loadNetwork(file, LocalComputationManager.getDefault(), new ImportConfig(), properties);
                    }
                };
            }
        };
        networkService.setOnRunning(event -> {
            caseLoadingStatus.setStyle("-fx-background-color: yellow");
            casePathTextField.setText(file.toAbsolutePath().toString());
            preferences.put(CASE_FOLDER_PROPERTY, file.getParent().toString());
        });
        networkService.setOnSucceeded(event -> {
            setNetwork((Network) event.getSource().getValue());
            caseLoadingStatus.setStyle("-fx-background-color: green");
            preferences.put(CASE_PATH_PROPERTY, file.toAbsolutePath().toString());
        });
        networkService.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            LOGGER.error(exception.toString(), exception);
            casePathTextField.setText("");
            caseLoadingStatus.setStyle("-fx-background-color: red");
        });
        networkService.start();
    }

    private void loadNetworkFromPreferences() {
        String casePathPropertyValue = preferences.get(CASE_PATH_PROPERTY, null);
        if (casePathPropertyValue != null) {
            loadNetwork(Paths.get(casePathPropertyValue));
        }
    }

    private Node createCasePane(Stage primaryStage) {
        caseLoadingStatus.setStyle("-fx-background-color: red");
        casePathTextField.setEditable(false);

        HBox.setHgrow(casePathTextField, Priority.ALWAYS);

        Button caseButton = new Button("...");
        caseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            String caseFolderPropertyValue = preferences.get(CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null) {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }
            fileChooser.setTitle("Open case File");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                loadNetwork(file.toPath());
            }
        });
        HBox casePane = new HBox(3, caseLoadingStatus, casePathTextField, caseButton);
        BorderPane.setMargin(casePane, new Insets(3, 3, 3, 3));
        return casePane;
    }

    /*
        check/uncheck a voltageLevel in the substations tree
     */
    private void checkVoltageLevel(VoltageLevel v, Boolean checked) {
        selectableVoltageLevels.stream()
                .filter(selectableVoltageLevel -> selectableVoltageLevel.getId().equals(v.getId()))
                .forEach(selectableVoltageLevel -> selectableVoltageLevel.setCheckedProperty(checked));
    }

    /*
        check/uncheck a substation in the substations tree
     */
    private void checkSubstation(Substation s, Boolean checked) {
        selectableSubstations.stream()
                .filter(selectableSubstation -> selectableSubstation.getId().equals(s.getId()))
                .forEach(selectableSubstation -> selectableSubstation.setCheckedProperty(checked));
    }

    private void initVoltageLevelsTree(TreeItem<Container> rootItem,
                                       Substation s, String filter, boolean emptyFilter,
                                       Map<String, SelectableSubstation> mapSubstations,
                                       Map<String, SelectableVoltageLevel> mapVoltageLevels) {
        boolean firstVL = true;
        CheckBoxTreeItem<Container> sItem = null;

        for (VoltageLevel v : s.getVoltageLevels()) {
            boolean vlOk = showNames.isSelected() ? v.getName().contains(filter) : v.getId().contains(filter);

            if (!emptyFilter && !vlOk) {
                continue;
            }

            if (firstVL && !hideSubstations.isSelected()) {
                sItem = new CheckBoxTreeItem<>(s);
                sItem.setIndependent(true);
                sItem.setExpanded(true);
                if (mapSubstations.containsKey(s.getId()) && mapSubstations.get(s.getId()).checkedProperty().get()) {
                    sItem.setSelected(true);
                }
                rootItem.getChildren().add(sItem);
                sItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                        checkSubstation(s, newVal)
                );
            }

            firstVL = false;

            if (!hideVoltageLevels.isSelected()) {
                CheckBoxTreeItem<Container> vItem = new CheckBoxTreeItem<>(v);
                vItem.setIndependent(true);
                if (mapVoltageLevels.containsKey(v.getId()) && mapVoltageLevels.get(v.getId()).checkedProperty().get()) {
                    vItem.setSelected(true);
                }
                if (sItem != null) {
                    sItem.getChildren().add(vItem);
                } else {
                    rootItem.getChildren().add(vItem);
                }

                vItem.selectedProperty().addListener((obs, oldVal, newVal) ->
                    checkVoltageLevel(v, newVal));
            }

        }
    }

    private void initSubstationsTree() {
        String filter = filterInput.getText();
        boolean emptyFilter = StringUtils.isEmpty(filter);

        Network n = networkProperty.get();
        TreeItem<Container> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        Map<String, SelectableSubstation> mapSubstations = selectableSubstations.stream()
                .collect(Collectors.toMap(SelectableSubstation::getId, Function.identity()));
        Map<String, SelectableVoltageLevel> mapVoltageLevels = selectableVoltageLevels.stream()
                .collect(Collectors.toMap(SelectableVoltageLevel::getId, Function.identity()));

        for (Substation s : n.getSubstations()) {
            initVoltageLevelsTree(rootItem, s, filter, emptyFilter, mapSubstations, mapVoltageLevels);
        }

        if (substationsTree.getRoot() != null) {
            substationsTree.getRoot().getChildren().clear();
        }

        substationsTree.setRoot(rootItem);
        substationsTree.setShowRoot(false);
    }

    @Override
    public void display(String voltageLevelId) {
        VoltageLevel v = networkProperty.get().getVoltageLevel(voltageLevelId);
        if (diagramsPane.getSelectionModel().getSelectedItem() == tabChecked) {
            checkVoltageLevel(v, true);
            checkvItemTree(voltageLevelId, true);
            checkedDiagramsPane.getTabs().stream().forEach(tab -> {
                if (tab.getText().equals(voltageLevelId)) {
                    checkedDiagramsPane.getSelectionModel().select(tab);
                }
            });
        } else if (diagramsPane.getSelectionModel().getSelectedItem() == tabSelected) {
            selectedDiagramPane.setCenter(new ContainerDiagramPane(v));
        }
    }

    private void checkvItemTree(String id, boolean selected) {
        substationsTree.getRoot().getChildren().stream().forEach(childS ->
                childS.getChildren().stream().forEach(childV -> {
                    if (childV.getValue().getId().equals(id)) {
                        ((CheckBoxTreeItem) childV).setSelected(selected);
                    }
                })
        );
    }

    public void saveSelectedDiagrams() {
        try {
            String selectedVoltageLevelIdsPropertyValue = objectMapper.writeValueAsString(selectableVoltageLevels.stream()
                    .filter(selectableVoltageLevel -> selectableVoltageLevel.checkedProperty().get())
                    .map(SelectableVoltageLevel::getId)
                    .collect(Collectors.toList()));
            preferences.put(SELECTED_VOLTAGE_LEVEL_IDS_PROPERTY, selectedVoltageLevelIdsPropertyValue);

            String selectedSubstationIdsPropertyValue = objectMapper.writeValueAsString(selectableSubstations.stream()
                    .filter(selectableSubstation -> selectableSubstation.checkedProperty().get())
                    .map(SelectableSubstation::getId)
                    .collect(Collectors.toList()));
            preferences.put(SELECTED_SUBSTATION_IDS_PROPERTY, selectedSubstationIdsPropertyValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void closeAllTabs() {
        selectableVoltageLevels.stream().filter(s -> s.checkedProperty().get()).forEach(s -> s.closeTab());
        selectableSubstations.stream().filter(s -> s.checkedProperty().get()).forEach(s -> s.closeTab());
    }

    protected void setNetwork(Network network) {
        closeAllTabs();
        updateLayoutsFactory(network);
        updateStylesProvider(network);
        networkProperty.setValue(network);
        initSubstationsTree();
        setDiagramsNamesContent(network, true);
    }

    private void initLayoutsFactory() {
        voltageLevelsLayouts.put("Smart", null);
        voltageLevelsLayouts.put("Auto extensions", new PositionVoltageLevelLayoutFactory(new PositionFromExtension()));
        voltageLevelsLayouts.put("Auto without extensions Clustering", new PositionVoltageLevelLayoutFactory(new PositionByClustering()));
        voltageLevelsLayouts.put("Random", new RandomVoltageLevelLayoutFactory(500, 500));
        voltageLevelsLayouts.put("Cgmes", null);

        substationsLayouts.put("Horizontal", new HorizontalSubstationLayoutFactory());
        substationsLayouts.put("Vertical", new VerticalSubstationLayoutFactory());
        substationsLayouts.put("Cgmes", null);
        substationsLayouts.put("Smart", new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.NONE));
        substationsLayouts.put("Smart with horizontal compaction", new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.HORIZONTAL));
        substationsLayouts.put("Smart with vertical compaction", new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.VERTICAL));
    }

    private void updateLayoutsFactory(Network network) {
        voltageLevelsLayouts.put("Smart", new SmartVoltageLevelLayoutFactory(network));
        voltageLevelsLayouts.put("Cgmes", new CgmesVoltageLevelLayoutFactory(network));

        substationsLayouts.put("Cgmes", new CgmesSubstationLayoutFactory(network));
    }

    private void initStylesProvider() {
        styles.put("Default", new DefaultDiagramStyleProvider());
        styles.put("Nominal voltage", null);
        styles.put("Topology", null);
    }

    private void updateStylesProvider(Network network) {
        styles.put("Nominal voltage", new NominalVoltageDiagramStyleProvider(network));
        styles.put("Topology", new TopologicalStyleProvider(network));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
