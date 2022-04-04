/**
* Copyright (c) 2022, RTE (http://www.rte-france.com)
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.powsybl.ad.viewer.model;
import com.powsybl.ad.viewer.controller.ControllerParameters;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.Padding;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

/**
* @author Louis Lhotte <louis.lhotte@student-cs.fr>
*/
public final class NadCalls {

    public static final ObjectProperty <Network> networkProperty = new SimpleObjectProperty<>();
    public static final ObjectProperty<LayoutParameters> layoutParametersProperty = new SimpleObjectProperty<>(new LayoutParameters());
    public static final ObjectProperty<SvgParameters> svgParametersProperty = new SimpleObjectProperty<>(new SvgParameters().setSvgWidthAndHeightAdded(true));
    public static Service <Network> networkService;

    private static StringWriter svgWriter = new StringWriter();


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
                        return Importers.loadNetwork(file);
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

    private static void cleanSVG() {
        svgWriter = new StringWriter();
    }

    public static void drawNetwork() {
        cleanSVG();
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get()).draw(svgWriter,
                                             svgParametersProperty.get(),
                                             layoutParametersProperty.get(),
                                             ControllerParameters.styleProvider);
    }


    public static void loadUniqueSubstation(List<String> voltageLevelIds, int depth){
        // draw when clicking on a substation, with the list of voltage levels within the substation
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get(), voltageLevelIds, depth).draw(svgWriter,
                                                                     svgParametersProperty.get(),
                                                                     layoutParametersProperty.get(),
                                                                     ControllerParameters.styleProvider);
    }


    public static void loadSubgraph(String voltageLevelId, int depth) {
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get(), voltageLevelId, depth).draw(svgWriter,
                                                                    svgParametersProperty.get(),
                                                                    layoutParametersProperty.get(),
                                                                    ControllerParameters.styleProvider);
    }


    public static void runLoadFlow() {
        // A button "Run loadflow" should be added, as often the power flow values in the input file are missing.
        // Requires a maven clean install of powsybl-open-loadflow
        LoadFlow.run(networkProperty.get());
        setDefaultStyleProvider();
        new NetworkAreaDiagram(networkProperty.get()).draw(svgWriter,
                                             svgParametersProperty.get(),
                                             layoutParametersProperty.get(),
                                             ControllerParameters.styleProvider);
    }

    public static StringWriter getSvgWriter() {
        return svgWriter;
    }

    public static void setSvgWriter(StringWriter svgWriter) {
        NadCalls.svgWriter = svgWriter;
    }

}
