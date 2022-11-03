# Network-area-diagram-viewer

This is a viewer meant for helping developers to add new features in [powsybl-network-area-diagram](https://github.com/powsybl/powsybl-network-area-diagram/).

To launch the viewer, use the following command line: 
```
mvn javafx:run
```

You can also launch the viewer by running `NetworkAreaDiagramViewer::main` with your favorite IDE.
Then you would need to:
* install [JavaFX 17](https://openjfx.io/) or above
* add the following vm options in the launch configuration:
  ```
  --module-path /path/to/javafx/lib --add-modules=javafx.controls,javafx.fxml,javafx.web
  ```
