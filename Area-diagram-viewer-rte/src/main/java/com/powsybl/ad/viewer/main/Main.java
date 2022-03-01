package com.powsybl.ad.viewer.main;

import com.powsybl.ad.viewer.util.Util;
import com.powsybl.ad.viewer.view.AreaDiagramViewer;
import javafx.application.Application;

public class Main {

        public static void main(String[] args)
        {
            Util.logger.info("Starting Area Diagram Viewer application.");

            // Launches application
            Application.launch(AreaDiagramViewer.class);

            Util.logger.info("Ending Area Diagram Viewer application.");
        }
}
