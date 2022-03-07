package com.powsybl.ad.viewer.model;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.SvgParameters;
import javafx.util.Pair;

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class NadCalls {

    private static Network network;
    private static LayoutParameters layoutParameters;
    public static StringWriter svgWriter;
    public static List listVoltageLevelId;
    public static List listVoltageLevelName;


//    public static void setLayoutParameters(String layoutParameterSelected) {
//        LayoutParameters layoutParameters = layoutParameterSelected;
//    }

    public static void loadNetwork(Path file) {
        Properties properties = new Properties();
        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");

        network = Importers.loadNetwork(file, LocalComputationManager.getDefault(),
                                        new ImportConfig(), properties);

        svgWriter = new StringWriter();
        new NetworkAreaDiagram(network).draw(svgWriter, new SvgParameters());
//        System.out.println(svgWriter);
        listVoltageLevel();
        for (Object s:listVoltageLevelId) {
            System.out.println(s.toString());
        }
        for (Object s:listVoltageLevelName) {
            System.out.println(s.toString());
        }
    }

    public static void loadSubgraph(String voltageLevelId, int depth) {
        svgWriter = new StringWriter();
        new NetworkAreaDiagram(network, voltageLevelId, depth).draw(svgWriter, new SvgParameters());
//        System.out.println(svgWriter);
    }

    public static void listVoltageLevel() {
        // listVoltageLevelId and listVoltageLevelName are generated
        listVoltageLevelName = network.getVoltageLevelStream().map(Identifiable::getName).collect(Collectors.toList());
        listVoltageLevelId = network.getVoltageLevelStream().map(Identifiable::getId).collect(Collectors.toList());
    }

    public static void runLoadFlow() {
        // A button "Run loadflow" should be added, as often the power flow values in the input file are missing.
        // Requires a maven clean install of powsybl-open-loadflow
        LoadFlow.run(network);
//        System.out.println(svgWriter);
        new NetworkAreaDiagram(network).draw(svgWriter);
//        System.out.println(svgWriter);
    }

    public static void main(String[] args) {
        // Only for testing purposes
        String file = "C:\\Users\\lhott\\Documents\\VraiTravail\\Free-lance\\JCS\\RTE\\" +
                      "TestConfigurations_packageCASv2.0\\MicroGrid\\" +
                      "Type4_T4/CGMES_v2.4.15_MicroGridTestConfiguration_T4_Assembled_NB_Complete_v2.zip";
        loadNetwork(Paths.get(file));
        runLoadFlow();
        loadSubgraph("_4ba71b59-ee2f-450b-9f7d-cc2f1cc5e386", 2);
    }
}
