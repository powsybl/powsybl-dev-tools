/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.ad.viewer.model;

import com.powsybl.ad.viewer.controller.ControllerParameters;
import com.powsybl.ad.viewer.view.ParamPane;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public final class NadCalls {

    public static Network network;

    public static StringWriter svgWriter;

    public static List listSubstationId;
    public static List listSubstationName;
    public static List listVoltageLevelId;
    public static List listVoltageLevelName;

    public static void loadNetwork(Path file) {
        Properties properties = new Properties();
        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");

        network = Importers.loadNetwork(file, LocalComputationManager.getDefault(),
                new ImportConfig(), properties);

        svgWriter = new StringWriter();
    }

    public static void drawNetwork() {
        new NetworkAreaDiagram(network).draw(svgWriter,
                                             new SvgParameters(),
                                             new LayoutParameters(),
                                             ControllerParameters.styleProvider);
//        System.out.println(svgWriter);
        listVoltageLevel();
    }


    public static void loadUniqueSubstation(List<String> voltageLevelIds, int depth){
        // draw when clicking on a substation, with the list of voltage levels within the substation
        new NetworkAreaDiagram(network, voltageLevelIds, depth).draw(svgWriter,
                                                                     new SvgParameters(),
                                                                     new LayoutParameters(),
                                                                     ControllerParameters.styleProvider);
    }


    public static void loadSubgraph(String voltageLevelId, int depth) {
        svgWriter = new StringWriter();
        new NetworkAreaDiagram(network, voltageLevelId, depth).draw(svgWriter,
                                                                    new SvgParameters(),
                                                                    new LayoutParameters(),
                                                                    ControllerParameters.styleProvider);
    }


    public static void listVoltageLevel() {
        // listSubstationId and listSubstationName are generated
        listSubstationId = network.getSubstationStream().map(Identifiable::getName).collect(Collectors.toList());
        listSubstationName = network.getSubstationStream().map(Identifiable::getId).collect(Collectors.toList());
        // listVoltageLevelId and listVoltageLevelName are generated
        listVoltageLevelName = network.getVoltageLevelStream().map(Identifiable::getName).collect(Collectors.toList());
        listVoltageLevelId = network.getVoltageLevelStream().map(Identifiable::getId).collect(Collectors.toList());

        // Prints
        System.out.println(network.getSubstationStream());
        System.out.println(listSubstationName);
        System.out.println(listSubstationId);
        System.out.println(listVoltageLevelName);
        System.out.println(listVoltageLevelId);
    }


    public static void runLoadFlow() {
        // A button "Run loadflow" should be added, as often the power flow values in the input file are missing.
        // Requires a maven clean install of powsybl-open-loadflow
        LoadFlow.run(network);
//        System.out.println(svgWriter);
        new NetworkAreaDiagram(network).draw(svgWriter,
                                             new SvgParameters(),
                                             new LayoutParameters(),
                                             ControllerParameters.styleProvider);
//        System.out.println(svgWriter);
    }


    public static void main(String[] args) {
        // Only for testing purposes
        String file = "C:\\Users\\lhott\\Documents\\VraiTravail\\Free-lance\\JCS\\RTE\\" +
                      "TestConfigurations_packageCASv2.0\\MicroGrid\\" +
                      "Type4_T4/CGMES_v2.4.15_MicroGridTestConfiguration_T4_Assembled_NB_Complete_v2.zip";
        loadNetwork(Paths.get(file));
        listVoltageLevel();
//        runLoadFlow();
//        loadSubgraph("_4ba71b59-ee2f-450b-9f7d-cc2f1cc5e386", 2);
    }
}
