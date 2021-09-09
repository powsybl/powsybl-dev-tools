/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.viewer;

import com.powsybl.sld.library.ComponentSize;
import com.powsybl.sld.model.BaseNode;
import com.powsybl.sld.model.Point;
import com.powsybl.sld.svg.GraphMetadata;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class VoltageLevelHandler implements BaseNode {

    private final Node node;   // node for voltageLevel label

    private final List<NodeHandler> nodeHandlers = new ArrayList<>();

    private final String vId;

    private double mouseX;
    private double mouseY;

    private final GraphMetadata metadata;

    public VoltageLevelHandler(Node node, GraphMetadata metadata, String vId) {
        this.node = Objects.requireNonNull(node);
        this.metadata = Objects.requireNonNull(metadata);
        this.vId = Objects.requireNonNull(vId);

        setDragAndDrop();
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String getId() {
        return node.getId();
    }

    public String getVId() {
        return vId;
    }

    @Override
    public String getComponentType() {
        return null;
    }

    @Override
    public Double getRotationAngle() {
        return null;
    }

    @Override
    public boolean isRotated() {
        return false;
    }

    @Override
    public Point getDiagramCoordinates() {
        ComponentSize size = new ComponentSize(0, 0);
        Point2D parent = node.localToParent(node.getLayoutX() + size.getWidth() / 2,
            node.getLayoutY() + size.getHeight() / 2);
        return new Point(parent.getX(), parent.getY());
    }

    public double getX() {
        return getDiagramCoordinates().getX();
    }

    public double getY() {
        return getDiagramCoordinates().getY();
    }

    public void addNodeHandlers(List<NodeHandler> nodeHandlers) {
        this.nodeHandlers.addAll(nodeHandlers);
    }

    public void setDragAndDrop() {
        node.setOnMousePressed(event -> {
            mouseX = event.getSceneX() - node.getTranslateX();
            mouseY = event.getSceneY() - node.getTranslateY();
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            // apply transformation for label node
            node.setTranslateX(event.getSceneX() - mouseX);
            node.setTranslateY(event.getSceneY() - mouseY);

            // apply transformation to all nodes of the voltageLevel in nodeHandlers list
            nodeHandlers.stream().filter(n -> n.getVId().equals(vId)).forEach(v -> v.translate(event.getSceneX() - mouseX,
                                                           event.getSceneY() - mouseY));

            redrawSnakeLines();

            event.consume();
        });
    }

    private void redrawSnakeLines() {
        // TODO: redraw the snakeLines between the voltage levels using ForceSubstationLayout
    }

}
