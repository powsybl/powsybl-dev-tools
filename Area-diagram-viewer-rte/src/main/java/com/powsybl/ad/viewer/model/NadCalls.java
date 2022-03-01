package com.powsybl.ad.viewer.model;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.iidm.import_.Importers;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class NadCalls {

    public static void loadNetwork(Path file) {
        Properties properties = new Properties();
        properties.put("iidm.import.cgmes.post-processors", "cgmesDLImport");

        Network network = Importers.loadNetwork(file, LocalComputationManager.getDefault(),
                new ImportConfig(), properties);

        new NetworkAreaDiagram(network).draw(Path.of("C:\\Users\\lhott\\Documents\\VraiTravail\\Free-lance\\JCS\\RTE\\test\\diagram.svg"));
    }

    public static void main(String[] args) {
        String file = "C:\\Users\\lhott\\Documents\\VraiTravail\\Free-lance\\JCS\\RTE\\" +
                      "TestConfigurations_packageCASv2.0\\MicroGrid\\" +
                      "Type4_T4/CGMES_v2.4.15_MicroGridTestConfiguration_T4_Assembled_NB_Complete_v2.zip";
        loadNetwork(Paths.get(file));
    }
}
