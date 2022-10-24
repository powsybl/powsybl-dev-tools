package com.powsybl.nad.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsHandler {

    JsHandler() {
    }

    /**
     * Called when the JS side detects an event that we should handle
     * @param svgId the svg identity of svg target element
     */
    public void handleEvent(String eventName, String svgId) {
        LOG.info("handleEvent [{}] on svgId = {}", eventName, svgId);
    }

    public void log(String message) {
        LOG.info(message);
    }

    private static final Logger LOG = LoggerFactory.getLogger(JsHandler.class);
}
