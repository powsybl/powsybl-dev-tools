package com.powsybl.ad.viewer.view.diagram;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;


public class ContainerDiagramPane extends BorderPane
{
    // Components for diagramPane
    /* private final WebView diagramView = new WebView();
    private final TextField svgSearchField = new TextField();
    private final Button svgSearchButton = new Button("Search");
    private final TextArea svgTextArea = new TextArea();
    private AtomicReference <Integer> svgSearchStart = new AtomicReference<>(0);
    private final Button svgSaveButton = new Button("Save"); */

    private TitledPane infoPane;
    private TextArea infoArea;


    private TabPane diagramTabPane;
    private Tab diagramTab;

    private Tab svgTab;
    private VBox svgArea;


    //private final ChangeListener<LayoutParameters> listener;

    public ContainerDiagramPane()
    {
        createInfoPane();

        createDiagramPane();

        this.setCenter(diagramTabPane);
        this.setBottom(infoPane);

    }

    private void createDiagramPane()
    {
        createDiagramTab();
        createSVGTab();

        diagramTabPane = new TabPane();
        diagramTabPane.getTabs().addAll(svgTab, diagramTab);
    }

    private void createSVGTab()
    {
        svgArea = new VBox();

        svgTab = new Tab("SVG", svgArea);
        svgTab.setClosable(false);
    }

    private void createDiagramTab()
    {
        diagramTab = new Tab("Diagram");
        diagramTab.setClosable(false);
    }

    private void createInfoPane()
    {
        infoArea = new TextArea();
        infoArea.setEditable(false);

        infoPane = new TitledPane("Voltage Level Infos", infoArea);
    }

    public TextArea getInfoArea()
    {
        return infoArea;
    }

    public VBox getSVGArea()
    {
        return svgArea;
    }

    // TODO: this ?
    // For communication from the Javascript engine.
    /*
    private final JsHandler jsHandler;

    InfoDiagramPane(Container<?> c) {
        jsHandler = new JsHandler(substationsTree, swId -> {
            Switch sw = c.getNetwork().getSwitch(swId);
            if (sw != null) {
                sw.setOpen(!sw.isOpen());
                DiagramStyleProvider styleProvider = styles.get(styleComboBox.getSelectionModel().getSelectedItem());
                styleProvider.reset();
                loadDiagram(c);
            }
        });

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

     */

        // TODO: extract Zoom to controller?
        // Add Zoom management
        /*
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
        }); */

        // Avoid the useless right click on the image
       // diagramView.setContextMenuEnabled(false);

        // Set up the listener on WebView changes
        // TODO: this
        /*
        diagramView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // Set an interface object named 'jsHandler' in the web engine's page
                JSObject window = (JSObject) diagramView.getEngine().executeScript("window");
                window.setMember("jsHandler", jsHandler);
            }
        }); */
    }

/*    class ContainerDiagramResult {

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

    private ContainerDiagramResult createContainerDiagramView(WebView diagramView, Container<?> c) {
        String svgData;
        String metadataData;
        String jsonData;
        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter();
             StringWriter jsonWriter = new StringWriter()) {
            DiagramStyleProvider styleProvider = styles.get(styleComboBox.getSelectionModel().getSelectedItem());

            String dName = getSelectedDiagramName();
            LayoutParameters diagramLayoutParameters = new LayoutParameters(layoutParameters.get())
                    .setUseName(showNames.isSelected())
                    .setDiagramName(dName)
                    .setCssLocation(LayoutParameters.CssLocation.INSERTED_IN_SVG)
                    .setSvgWidthAndHeightAdded(true);

            DiagramLabelProvider initProvider = new DefaultDiagramLabelProvider(networkProperty.get(), getComponentLibrary(), diagramLayoutParameters);

            SingleLineDiagram.draw(networkProperty.get(), c.getId(),
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

    private void loadDiagram(Container<?> c) {
        ContainerDiagramResult result = createContainerDiagramView(diagramView, c);

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
} */
