package com.powsybl.nad.viewer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Container;
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

    // To be able to access the Java Logger from JavaScript
    public void log(String message) {
        LOG.info(message);
    }

    // FIXME(Luma) The handler is created in the default constructor of the diagram controller,
    // when we do not have information about the model information
    public void linkWithController(Model model, StringProperty modelSvgContent, Container<?> container) {
        this.model = model;
        this.modelSvgContent = modelSvgContent;
        this.container = container;
    }

    /**
     * Called when the JS side decides that the diagram should be updated
     * @param jsonPositions a JSON string with new positions for (eventually all) elements in the diagram
     */
    public void updateDiagramWithPositions(String jsonPositions) {
        LOG.info("updateDiagramWithEquipmentPositions ({})", jsonPositions);
        DiagramController.updateDiagram(model, modelSvgContent, container, deserializePositions(jsonPositions));
    }

    private Map<String, Point> deserializePositions(String jsonPositions) {
        TypeReference<HashMap<String, Point>> typeRef = new TypeReference<>() { };
        try {
            return new ObjectMapper().readValue(jsonPositions, typeRef);
        } catch (JsonProcessingException x) {
            throw new PowsyblException("deserializing positions from " + jsonPositions, x);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(JsHandler.class);
}
