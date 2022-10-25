package com.powsybl.nad.viewer;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Container;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.nad.model.Point;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JsHandler {

    private Model model;
    private StringProperty modelSvgContent;
    private Container<?> container;

    private final Map<String, Point> postLayoutPositions = new HashMap<>();

    // FIXME(Luma) The handler is created in the default constructor of the diagram controller,
    // when we do not have information about the model information
    public void linkWithController(Model model, StringProperty modelSvgContent, Container<?> container) {
        this.model = model;
        this.modelSvgContent = modelSvgContent;
        this.container = container;
    }

    /**
     * Called when the JS side decides that the diagram should be updated
     * @param equipmentId the id of the equipment that has been moved
     * @param x the x coord of the new location for the equipment
     * @param y the y coord of the new location for the equipment
     */
    public void updateDiagramWithEquipmentLocation(String equipmentId, double x, double y) {
        LOG.info("updateDiagramWithEquipmentLocation ({}, {}, {})", equipmentId, x, y);
        // Check if we have a bus id or a voltage level id
        VoltageLevel vl = model.getNetwork().getVoltageLevel(equipmentId);
        if (vl == null) {
            Bus bus = model.getNetwork().getBusView().getBus(equipmentId);
            if (bus != null) {
                vl = bus.getVoltageLevel();
            }
        }
        if (vl != null) {
            postLayoutPositions.put(vl.getId(), new Point(x, y));
            DiagramController.updateDiagram(model, modelSvgContent, container, postLayoutPositions);
        }
    }

    public void log(String message) {
        LOG.info(message);
    }

    private static final Logger LOG = LoggerFactory.getLogger(JsHandler.class);
}
