/**
* Copyright (c) 2022, RTE (http://www.rte-france.com)
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.powsybl.ad.viewer.model;
import com.powsybl.ad.viewer.controller.ControllerParameters;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
* @author Louis Lhotte <louis.lhotte@student-cs.fr>
*/
public final class NadCalls {

    public static final ObjectProperty <Network> networkProperty = new SimpleObjectProperty<>();
    public static Service <Network> networkService;

    private static StringWriter svgWriter;


    public static void loadNetwork(Path file) {
        networkService = new Service () {
            @Override
            protected Task<Network> createTask()
            {
                return new Task ()
                {
                    @Override
                    protected Network call()
                    {
                        Properties properties = new Properties();
                        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");
                        return Importers.loadNetwork(file, LocalComputationManager.getDefault(),
                                new ImportConfig(), properties);
                    }
                };
            }
        };
    }


    public static void setDefaultStyleProvider() {
        if (ControllerParameters.styleProvider == null) {
            // set variable styleProvider to its default value if the dropdown menu's never been clicked on
            ControllerParameters.styleProvider = new TopologicalStyleProvider(networkProperty.get());
        }
    }


    public static void drawNetwork() {
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get()).draw(svgWriter,
                                             new SvgParameters().setSvgWidthAndHeightAdded(true),
                                             new LayoutParameters(),
                                             ControllerParameters.styleProvider);
    }


    public static void loadUniqueSubstation(List<String> voltageLevelIds, int depth){
        // draw when clicking on a substation, with the list of voltage levels within the substation
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get(), voltageLevelIds, depth).draw(svgWriter,
                                                                     new SvgParameters().setSvgWidthAndHeightAdded(true),
                                                                     new LayoutParameters(),
                                                                     ControllerParameters.styleProvider);
    }


    public static void loadSubgraph(String voltageLevelId, int depth) {
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get(), voltageLevelId, depth).draw(svgWriter,
                                                                    new SvgParameters().setSvgWidthAndHeightAdded(true),
                                                                    new LayoutParameters(),
                                                                    ControllerParameters.styleProvider);
    }


    public static void runLoadFlow() {
        // A button "Run loadflow" should be added, as often the power flow values in the input file are missing.
        // Requires a maven clean install of powsybl-open-loadflow
        LoadFlow.run(networkProperty.get());
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get()).draw(svgWriter,
                                             new SvgParameters().setSvgWidthAndHeightAdded(true),
                                             new LayoutParameters(),
                                             ControllerParameters.styleProvider);
    }

    public static StringWriter getSvgWriter() {
        return svgWriter;
    }

    public static void setSvgWriter(StringWriter svgWriter) {
        NadCalls.svgWriter = svgWriter;
    }

}
