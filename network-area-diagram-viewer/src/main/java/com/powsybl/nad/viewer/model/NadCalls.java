/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.nad.viewer.model;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.NominalVoltageStyleProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Louis Lhotte <louis.lhotte@student-cs.fr>
 */
public final class NadCalls {

    public static ObjectProperty<Network> networkProperty = new SimpleObjectProperty<>();
    public static final ObjectProperty<LayoutParameters> layoutParametersProperty = new SimpleObjectProperty<>(new LayoutParameters());
    public static final ObjectProperty<SvgParameters> svgParametersProperty = new SimpleObjectProperty<>(new SvgParameters().setSvgWidthAndHeightAdded(true));
    public static final ObjectProperty<StyleProvider> styleProviderProperty = new SimpleObjectProperty<>();
    public static Service<Network> networkService;

    private static StringWriter svgWriter = new StringWriter();

    private NadCalls() {
    }

    public static void loadNetwork(Path file) {
        networkService = new Service<>() {
            @Override
            protected Task<Network> createTask() {
                return new Task<>() {
                    @Override
                    protected Network call() {
                        return Importers.loadNetwork(file);
                    }
                };
            }
        };
    }

    public static void drawNetwork(SvgParameters svgParameters) {
        drawNetwork(networkProperty.get(), svgParameters);
    }

    public static void drawNetwork() {
        drawNetwork(networkProperty.get(), svgParametersProperty.get());
    }

    public static void drawNetwork(Network network) {
        drawNetwork(network, svgParametersProperty.get());
    }

    public static void drawNetwork(Network network, SvgParameters svgParameters) {
        networkProperty.set(network);
        draw(new NetworkAreaDiagram(network), svgParameters);
    }

    public static void drawUniqueSubstation(List<String> voltageLevelIds, int depth) {
        draw(new NetworkAreaDiagram(networkProperty.get(), voltageLevelIds, depth));
    }

    public static void drawSubgraph(String voltageLevelId, int depth) {
        draw(new NetworkAreaDiagram(networkProperty.get(), voltageLevelId, depth));
    }

    private static void draw(NetworkAreaDiagram networkAreaDiagram) {
        draw(networkAreaDiagram, svgParametersProperty.get());
    }

    private static void draw(NetworkAreaDiagram networkAreaDiagram, SvgParameters svgParameters) {
        cleanSvgWriter();
        svgParametersProperty.set(new SvgParameters(svgParameters));
        if (styleProviderProperty.get() == null) {
            styleProviderProperty.set(new NominalVoltageStyleProvider(networkProperty.get()));
        }
        networkAreaDiagram.draw(svgWriter, svgParametersProperty.get(), layoutParametersProperty.get(), styleProviderProperty.get());
    }

    public static void runLoadFlow() {
        if (networkProperty.get() != null) {
            LoadFlow.run(networkProperty.get());
        }
    }

    public static StringWriter getSvgWriter() {
        return svgWriter;
    }

    public static void cleanSvgWriter() {
        svgWriter = new StringWriter();
    }

    public static void cleanNetwork() {
        networkProperty = new SimpleObjectProperty<>();
    }
}
