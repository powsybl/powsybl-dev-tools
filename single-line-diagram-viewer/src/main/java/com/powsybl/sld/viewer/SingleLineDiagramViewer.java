/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.viewer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.cgmes.dl.iidm.extensions.NetworkDiagramData;
import com.powsybl.sld.cgmes.layout.CgmesSubstationLayoutFactory;
import com.powsybl.sld.cgmes.layout.CgmesVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.layout.positionbyclustering.PositionByClustering;
import com.powsybl.sld.layout.positionfromextension.PositionFromExtension;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.library.ComponentTypeName;
import com.powsybl.sld.svg.*;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit Jeanson <benoit.jeanson at rte-france.com>
 * @author Nicolas Duchene
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 * @author Florian Dupuy <florian.dupuy at rte-france.com>
 */
public class SingleLineDiagramViewer extends Application {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramViewer.class);

    private static final String SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY = "selectedVoltageLevelAndSubstationIds";
    private static final String CASE_PATH_PROPERTY = "casePath";
    private static final String CASE_FOLDER_PROPERTY = "caseFolder";

    private final Map<String, VoltageLevelLayoutFactory> voltageLevelsLayouts = new LinkedHashMap<>();

    private final Map<String, DiagramStyleProvider> styles = new LinkedHashMap<>();

    private final Map<String, SubstationLayoutFactory> substationsLayouts = new LinkedHashMap<>();

    private final Map<String, ComponentLibrary> svgLibraries
            = ComponentLibrary.findAll().stream().collect(Collectors.toMap(ComponentLibrary::getName, Function.identity()));

    private final ObservableList<SelectableContainer> selectableContainers = FXCollections.observableArrayList();

    private final TextField filterInput = new TextField();

    private final TreeView<Container<?>> substationsTree = new TreeView<>();

    private final TabPane diagramsPane = new TabPane();
    private Tab tabChecked;
    private Tab tabSelected;
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
        private final TextArea svgTextArea = new TextArea();
        private final TextArea metadataTextArea = new TextArea();
        private final TextArea jsonTextArea = new TextArea();

        /** For communication from the Javascript engine. */
        private final JsHandler jsHandler;

        /** We have to keep a reference to the following ChangeListener, which is passed to a WeakChangeListener,
         * otherwise it will be garbage collected too soon. */
        private final ChangeListener<LayoutParameters> listener;

        ContainerDiagramPane(Container<?> c) {
            jsHandler = new JsHandler(substationsTree, swId -> {
                Switch sw = c.getNetwork().getSwitch(swId);
                if (sw != null) {
                    sw.setOpen(!sw.isOpen());
                    DiagramStyleProvider styleProvider = styles.get(styleComboBox.getSelectionModel().getSelectedItem());
                    styleProvider.reset();
                    loadDiagram(c.getId(), layoutParameters.get());
                }
            });

            VBox svgArea = new VBox();
            VBox metadataArea = new VBox();
            VBox jsonArea = new VBox();
            createArea("SVG file", "*.svg", svgTextArea, svgArea);
            createArea("JSON file", "*.json", metadataTextArea, metadataArea);
            createArea("JSON file", "*.json", jsonTextArea, jsonArea);

            TabPane tabPane = new TabPane(
                    createNonClosableTab("Diagram", diagramView),
                    createNonClosableTab("SVG", svgArea),
                    createNonClosableTab("Metadata", metadataArea),
                    createNonClosableTab("Graph", jsonArea));
            tabPane.setSide(Side.BOTTOM);
            setCenter(tabPane);

            TextArea infoArea = new TextArea();
            infoArea.setEditable(false);
            infoArea.setText(String.join(System.lineSeparator(), "id: " + c.getId(), "name: " + c.getNameOrId()));
            setBottom(new TitledPane("Infos", infoArea));

            listener = (observable, oldValue, newValue) -> loadDiagram(c.getId(), newValue);
            layoutParameters.addListener(new WeakChangeListener<>(listener));
            loadDiagram(c.getId(), layoutParameters.get());

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

            // Set up the listener on WebView changes
            diagramView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (Worker.State.SUCCEEDED == newValue) {
                    // Set an interface object named 'jsHandler' in the web engine's page
                    JSObject window = (JSObject) diagramView.getEngine().executeScript("window");
                    window.setMember("jsHandler", jsHandler);
                }
            });
        }

        private Tab createNonClosableTab(String name, Node node) {
            Tab tab = new Tab(name, node);
            tab.setClosable(false);
            return tab;
        }

        private WebView getDiagramView() {
            return diagramView;
        }

        private String getSelectedDiagramName() {
            return diagramNamesComboBox.getSelectionModel().getSelectedItem();
        }

        private ContainerDiagramResult createContainerDiagramView(WebView diagramView, String sOrVlId, LayoutParameters layoutParameters) {
            String svgData;
            String metadataData;
            String jsonData;
            try (StringWriter svgWriter = new StringWriter();
                 StringWriter metadataWriter = new StringWriter();
                 StringWriter jsonWriter = new StringWriter()) {
                DiagramStyleProvider styleProvider = styles.get(styleComboBox.getSelectionModel().getSelectedItem());

                String dName = getSelectedDiagramName();
                LayoutParameters diagramLayoutParameters = new LayoutParameters(layoutParameters)
                        .setUseName(showNames.isSelected())
                        .setDiagramName(dName)
                        .setCssLocation(LayoutParameters.CssLocation.INSERTED_IN_SVG)
                        .setSvgWidthAndHeightAdded(true);

                DiagramLabelProvider initProvider = new DefaultDiagramLabelProvider(networkProperty.get(), getComponentLibrary(), diagramLayoutParameters);

                updateStylesProvider(networkProperty.get());
                SingleLineDiagram.draw(networkProperty.get(), sOrVlId,
                        svgWriter,
                        metadataWriter,
                        diagramLayoutParameters,
                        getComponentLibrary(),
                        getSubstationLayoutFactory(),
                        getVoltageLevelLayoutFactory(),
                        initProvider,
                        styleProvider,
                        "");

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
            try {
                String html = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/svg.html"))));
                String js = new String(ByteStreams.toByteArray(Objects.requireNonNull(getClass().getResourceAsStream("/svg.js"))));
                // Need to set HTML body margin to 0 to avoid margin around SVG displayed
                String content = html.replace("%__JS__%", js).replace("%__SVG__%", svg);
                diagramView.getEngine().loadContent(content);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void loadDiagram(String sOrVlId, LayoutParameters layoutParameters) {
            ContainerDiagramResult result = createContainerDiagramView(diagramView, sOrVlId, layoutParameters);

            svgTextArea.setText(result.getSvgData());
            metadataTextArea.setText(result.getMetadataData());
            jsonTextArea.setText(result.getJsonData());

            jsHandler.setMetadata(result.getMetadataData());
        }

        private ComponentLibrary getComponentLibrary() {
            String selectedItem = svgLibraryComboBox.getSelectionModel().getSelectedItem();
            return svgLibraries.get(selectedItem);
        }

        private SubstationLayoutFactory getSubstationLayoutFactory() {
            String selectedItem = substationLayoutComboBox.getSelectionModel().getSelectedItem();
            return substationsLayouts.get(selectedItem);
        }

        private void createArea(String descrSave, String extensionSave, TextArea textArea, VBox area) {
            TextField searchField = new TextField();
            Button searchButton = new Button("Search");
            Button saveButton = new Button("Save");

            HBox searchBox = new HBox();
            searchBox.setSpacing(20);
            searchBox.setPadding(new Insets(10));
            searchField.setPrefColumnCount(35);
            searchBox.getChildren().add(searchField);
            searchBox.getChildren().add(searchButton);
            searchBox.getChildren().add(saveButton);

            AtomicReference<Integer> searchStart = new AtomicReference<>(0);
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

    final class SelectableContainer {
        private final Container<?> container;
        private final BooleanProperty checkedProperty = new SimpleBooleanProperty();
        private boolean saveDiagrams = true;

        private SelectableContainer(Container<?> c) {
            this.container = c;
            checkedProperty.addListener((obs, wasSelected, isNowSelected) -> {
                if (Boolean.TRUE.equals(isNowSelected)) {
                    addDiagramTab();
                } else {
                    removeDiagramTab();
                }
                if (saveDiagrams) {
                    saveSelectedDiagrams();
                }
            });
        }

        private void addDiagramTab() {
            Tab tab = new Tab(container.getId(), new ContainerDiagramPane(container));
            tab.setTooltip(new Tooltip(container.getNameOrId()));
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
        }

        public void closeTab() {
            checkedProperty.set(false);
            if (container.getContainerType() == ContainerType.VOLTAGE_LEVEL) {
                unselectItemTree(substationsTree.getRoot().getChildren().stream().flatMap(childS -> childS.getChildren().stream()), getId());
            } else {
                unselectItemTree(substationsTree.getRoot().getChildren().stream(), getId());
            }
        }

        private void unselectItemTree(Stream<TreeItem<Container<?>>> treeItemStream, String id) {
            treeItemStream.filter(childV -> childV.getValue().getId().equals(id))
                    .findFirst()
                    .ifPresent(childV -> ((CheckBoxTreeItem<?>) childV).setSelected(false));
        }

        private void removeDiagramTab() {
            checkedDiagramsPane.getTabs().removeIf(tab -> tab.getText().equals(container.getId()));
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

        public String getId() {
            return container.getId();
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

    private <E> void addComboBox(String label, int row,
                                 E[] initializer,
                                 StringConverter<E> converter,
                                 BiFunction<LayoutParameters, E, LayoutParameters> updater) {

        ComboBox<E> cb = new ComboBox<>();
        cb.getItems().setAll(initializer);
        cb.getSelectionModel().select(initializer[0]);
        cb.setConverter(converter);
        cb.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> setParameters(updater.apply(layoutParameters.get(), newValue)));
        cb.valueProperty().addListener((observable, oldValue, newValue) -> setParameters(updater.apply(layoutParameters.get(), newValue)));
        parametersPane.add(new Label(label), 0, row);
        parametersPane.add(cb, 0, row + 1);
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
                        double zoomH = paneHeight / svgHeight;
                        double zoomW = paneWidth / svgWidth;
                        pane.getDiagramView().setZoom(Math.min(zoomH, zoomW));
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
        GridPane buttonsPane = new GridPane();
        buttonsPane.add(fitToContent, 0, 0);
        buttonsPane.add(resetZoom, 1, 0);
        buttonsPane.setHgap(10);
        parametersPane.add(buttonsPane, 0, rowIndex++);

        // svg library list
        svgLibraryComboBox.getItems().addAll(svgLibraries.keySet());
        svgLibraryComboBox.getSelectionModel().selectFirst();
        svgLibraryComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDiagram());
        parametersPane.add(new Label("Design:"), 0, rowIndex++);
        parametersPane.add(svgLibraryComboBox, 0, rowIndex++);

        styleComboBox.getItems().addAll(styles.keySet());
        styleComboBox.getSelectionModel().select(2);
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
        addCheckBox("Show internal nodes", rowIndex, LayoutParameters::isShowInternalNodes, LayoutParameters::setShowInternalNodes);
        rowIndex += 1;
        addCheckBox("Draw straight wires", rowIndex, LayoutParameters::isDrawStraightWires, LayoutParameters::setDrawStraightWires);
        rowIndex += 1;
        addCheckBox("Disconnectors on bus", rowIndex, lp -> lp.getComponentsOnBusbars().equals(List.of(ComponentTypeName.DISCONNECTOR)),
            (lp, b) -> lp.setComponentsOnBusbars(Boolean.TRUE.equals(b) ? List.of(ComponentTypeName.DISCONNECTOR) : Collections.emptyList()));
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
        addCheckBox("Avoid SVG components duplication", rowIndex, LayoutParameters::isAvoidSVGComponentsDuplication, LayoutParameters::setAvoidSVGComponentsDuplication);

        rowIndex += 1;
        addCheckBox("Adapt cell height to content", rowIndex, LayoutParameters::isAdaptCellHeightToContent, LayoutParameters::setAdaptCellHeightToContent);
        rowIndex += 2;
        addSpinner("Min space between components:", 8, 60, 1, rowIndex, LayoutParameters::getMinSpaceBetweenComponents, LayoutParameters::setMinSpaceBetweenComponents);
        rowIndex += 2;
        addSpinner("Minimum extern cell height:", 80, 300, 10, rowIndex, LayoutParameters::getMinExternCellHeight, LayoutParameters::setMinExternCellHeight);

        rowIndex += 2;
        StringConverter<LayoutParameters.Alignment> converter = new StringConverter<>() {
            @Override
            public String toString(LayoutParameters.Alignment object) {
                return object.name();
            }

            @Override
            public LayoutParameters.Alignment fromString(String string) {
                return LayoutParameters.Alignment.valueOf(string);
            }
        };
        addComboBox("BusBar alignment:", rowIndex, LayoutParameters.Alignment.values(), converter, LayoutParameters::setBusbarsAlignment);

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
        addCheckBox("Feeder info symmetry", rowIndex, LayoutParameters::isFeederInfoSymmetry, LayoutParameters::setFeederInfoSymmetry);
        rowIndex += 2;
        addSpinner("Space for feeder infos", 0, 200, 1, rowIndex, LayoutParameters::getSpaceForFeederInfos, LayoutParameters::setSpaceForFeederInfos);
        rowIndex += 2;
        addSpinner("Feeder Infos outer margin:", 0, 200, 1, rowIndex, LayoutParameters::getFeederInfosOuterMargin, LayoutParameters::setFeederInfosOuterMargin);
        rowIndex += 2;
        addSpinner("Feeder infos intra margin", 0, 200, 1, rowIndex, LayoutParameters::getFeederInfosIntraMargin, LayoutParameters::setFeederInfosIntraMargin);
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

    private void loadSelectedContainersDiagrams() {
        String selectedIdsPropertyValue = preferences.get(SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY, null);
        if (selectedIdsPropertyValue != null) {
            try {
                Set<String> selectedIds = new HashSet<>(objectMapper.readValue(selectedIdsPropertyValue, new TypeReference<List<String>>() {
                }));
                selectableContainers.stream()
                        .filter(selectableObject -> selectedIds.contains(selectableObject.getId()))
                        .forEach(selectableContainer -> {
                            selectableContainer.setSaveDiagrams(false);
                            selectableContainer.setCheckedProperty(true);
                            selectableContainer.setSaveDiagrams(true);
                        });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Handling the display of names/id in the substations tree
     */
    private void initTreeCellFactory() {
        substationsTree.setCellFactory(param -> {
            CheckBoxTreeCell<Container<?>> treeCell = new CheckBoxTreeCell<>();
            StringConverter<TreeItem<Container<?>>> strConvert = new StringConverter<>() {
                @Override
                public String toString(TreeItem<Container<?>> c) {
                    if (c.getValue() != null) {
                        return getString(c.getValue());
                    } else {
                        return "";
                    }
                }

                @Override
                public TreeItem<Container<?>> fromString(String string) {
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

        primaryStage.getIcons().add(new Image("/images/logo.png"));

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
                selectableContainers.clear();
            } else {
                List<SelectableContainer> vlAndSubstations = Stream.concat(newNetwork.getVoltageLevelStream(), newNetwork.getSubstationStream())
                                .map(SelectableContainer::new)
                                .collect(Collectors.toList());
                selectableContainers.setAll(vlAndSubstations);
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

        // selected voltageLevels / substations diagrams reloading
        selectableContainers.addListener((ListChangeListener<SelectableContainer>) c -> loadSelectedContainersDiagrams());

        // Handling selection of a substation or a voltageLevel in the substations tree
        substationsTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            Container<?> c = newValue.getValue();
            selectedDiagramPane.setCenter(new ContainerDiagramPane(c));
            diagramsPane.getSelectionModel().select(tabSelected);
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
                        return Network.read(file, LocalComputationManager.getDefault(), new ImportConfig(), properties);
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
            fileChooser.setTitle("Open case File");

            String caseFolderPropertyValue = preferences.get(CASE_FOLDER_PROPERTY, null);
            if (caseFolderPropertyValue != null) {
                fileChooser.setInitialDirectory(new File(caseFolderPropertyValue));
            }

            File file;
            try {
                file = fileChooser.showOpenDialog(primaryStage);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Could not set initial directory to last used file directory");
                fileChooser.setInitialDirectory(null);
                file = fileChooser.showOpenDialog(primaryStage);
            }

            if (file != null) {
                loadNetwork(file.toPath());
            }
        });
        HBox casePane = new HBox(3, caseLoadingStatus, casePathTextField, caseButton);
        BorderPane.setMargin(casePane, new Insets(3, 3, 3, 3));
        return casePane;
    }

    /**
     * Check/uncheck a voltageLevel or substation in the substations tree
     */
    private void checkContainer(Container<?> c, Boolean checked) {
        selectableContainers.stream()
                .filter(selectableContainer -> selectableContainer.getId().equals(c.getId()))
                .forEach(selectableContainer -> selectableContainer.setCheckedProperty(checked));
    }

    private void initVoltageLevelsTree(TreeItem<Container<?>> rootItem, CheckBoxTreeItem<Container<?>> sItem,
                                       Collection<VoltageLevel> voltageLevels, Map<String, SelectableContainer> mapVoltageLevels) {

        for (VoltageLevel v : voltageLevels) {

            if (!hideVoltageLevels.isSelected()) {
                CheckBoxTreeItem<Container<?>> vItem = new CheckBoxTreeItem<>(v);
                vItem.setIndependent(true);
                if (mapVoltageLevels.containsKey(v.getId()) && mapVoltageLevels.get(v.getId()).checkedProperty().get()) {
                    vItem.setSelected(true);
                }
                if (sItem != null) {
                    sItem.getChildren().add(vItem);
                } else {
                    rootItem.getChildren().add(vItem);
                }

                vItem.selectedProperty().addListener((obs, oldVal, newVal) -> checkContainer(v, newVal));
            }

        }
    }

    private void initSubstationsTree() {
        String filter = filterInput.getText();
        boolean emptyFilter = StringUtils.isEmpty(filter);

        Network n = networkProperty.get();
        TreeItem<Container<?>> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        Map<String, SelectableContainer> mapContainers = selectableContainers.stream()
                .collect(Collectors.toMap(SelectableContainer::getId, Function.identity()));

        for (Substation s : n.getSubstations()) {
            CheckBoxTreeItem<Container<?>> sItem = null;
            boolean sFilterOk = emptyFilter || testPassed(filter, s);
            List<VoltageLevel> voltageLevels = s.getVoltageLevelStream()
                    .filter(v -> sFilterOk || testPassed(filter, v))
                    .collect(Collectors.toList());
            if ((sFilterOk || !voltageLevels.isEmpty()) && !hideSubstations.isSelected()) {
                sItem = new CheckBoxTreeItem<>(s);
                sItem.setIndependent(true);
                sItem.setExpanded(true);
                if (mapContainers.containsKey(s.getId()) && mapContainers.get(s.getId()).checkedProperty().get()) {
                    sItem.setSelected(true);
                }
                rootItem.getChildren().add(sItem);
                sItem.selectedProperty().addListener((obs, oldVal, newVal) -> checkContainer(s, newVal));
            }

            initVoltageLevelsTree(rootItem, sItem, voltageLevels, mapContainers);
        }

        List<VoltageLevel> emptySubstationVoltageLevels = n.getVoltageLevelStream()
                .filter(v -> v.getSubstation().isEmpty())
                .filter(v -> testPassed(filter, v))
                .collect(Collectors.toList());
        initVoltageLevelsTree(rootItem, null, emptySubstationVoltageLevels, mapContainers);

        if (substationsTree.getRoot() != null) {
            substationsTree.getRoot().getChildren().clear();
        }

        substationsTree.setRoot(rootItem);
        substationsTree.setShowRoot(false);
    }

    private Function<Identifiable<?>, String> getIdentifiableStringSupplier() {
        return showNames.isSelected() ? Identifiable::getNameOrId : Identifiable::getId;
    }

    private boolean testPassed(String filter, Identifiable<?> identifiable) {
        return getIdentifiableStringSupplier().apply(identifiable)
                .toLowerCase(Locale.getDefault())
                .contains(filter.toLowerCase(Locale.getDefault()));
    }

    public void saveSelectedDiagrams() {
        try {
            String selectedVoltageLevelIdsPropertyValue = objectMapper.writeValueAsString(selectableContainers.stream()
                    .filter(selectableVoltageLevel -> selectableVoltageLevel.checkedProperty().get())
                    .map(SelectableContainer::getId)
                    .collect(Collectors.toList()));
            preferences.put(SELECTED_VOLTAGE_LEVEL_AND_SUBSTATION_IDS_PROPERTY, selectedVoltageLevelIdsPropertyValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void closeAllTabs() {
        selectableContainers.stream().filter(s -> s.checkedProperty().get()).forEach(SelectableContainer::closeTab);
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
    }

    private void updateLayoutsFactory(Network network) {
        voltageLevelsLayouts.put("Smart", new SmartVoltageLevelLayoutFactory(network));
        voltageLevelsLayouts.put("Cgmes", new CgmesVoltageLevelLayoutFactory(network));

        substationsLayouts.put("Cgmes", new CgmesSubstationLayoutFactory(network));
    }

    private void initStylesProvider() {
        styles.put("Basic", new BasicStyleProvider());
        styles.put("Nominal voltage", null);
        styles.put("Topology (default)", null);
    }

    private void updateStylesProvider(Network network) {
        styles.put("Nominal voltage", new NominalVoltageDiagramStyleProvider(network));
        styles.put("Topology (default)", new TopologicalStyleProvider(network));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
