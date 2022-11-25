
// TODO(Luma) separate components of edges, having one that can be "scaled" (length line from exit of node to middle point)
//              for reference, see Android NinePatch drawables
//              https://developer.android.com/develop/ui/views/graphics/drawables#nine-patch
// TODO(Luma) JavaFX seems to have problems drawing a translated foreign object, try to enclose it in a <g> element?
// TODO(Luma) update on client: find related hidden nodes and update initial position in metadata?

window.addEventListener('load', function() {
    var svgDiagram = document.getElementsByTagName("svg")[0];
    if (svgDiagram != null) {
        makeDraggableSvg(svgDiagram);
    }
});

// Draggable SVG Elements from
// https://www.petercollingridge.co.uk/tutorials/svg/interactive/dragging/
function makeDraggableSvg(svg) {
    var selectedElement = false, offset, transform;
    var diagram = new Diagram(svg);

    svg.addEventListener('mousedown', startDrag);
    svg.addEventListener('mousemove', drag);
    svg.addEventListener('mouseup', endDrag);
    svg.addEventListener('mouseleave', endDrag);

    function getMousePosition(event) {
        // From screen coordinates to SVG coordinate system
        var CTM = svg.getScreenCTM();
        return {
            x: (event.clientX - CTM.e) / CTM.a,
            y: (event.clientY - CTM.f) / CTM.d
        };
    }

    function startDrag(event) {
        var draggableElem = diagram.getDraggableFrom(event);
        if (!draggableElem) {
            return;
        }
        selectedElement = draggableElem;
        selectedElement.style.cursor = 'grabbing';

        offset = getMousePosition(event);
        var transforms = getTransformsEnsuringFirstIsTranslation(svg, selectedElement);
        // Get initial translation amount
        transform = transforms.getItem(0);
        offset.x -= transform.matrix.e;
        offset.y -= transform.matrix.f;
    }

    const CONTINUOUS_UPDATE = true;
    function drag(event) {
        if (selectedElement) {
            event.preventDefault();
            var position = getMousePosition(event);
            var translation = {x: position.x - offset.x, y: position.y - offset.y};
            transform.setTranslate(translation.x, translation.y);
            if (CONTINUOUS_UPDATE) {
                diagram.update(selectedElement, position, translation);
            }
        }
    }

    function endDrag(event) {
        if (selectedElement) {
            console.log("endDrag for element " + selectedElement.id);
            selectedElement.style.cursor = 'grab';
            // Desired position for the selected element is current mouse position in SVG coordinate system
            var position = getMousePosition(event);
            var translation = {x: position.x - offset.x, y: position.y - offset.y};
            diagram.update(selectedElement, position, translation);
        }
        selectedElement = null;
    }
}

// PowSyBl Diagram

function Diagram(svg) {
    this.getDraggableFrom = getDraggableFrom;
    this.update = update;

    function getDraggableFrom(event) {
        if (isDraggable(event.target)) {
            return event.target;
        } else if (isDraggable(event.target.parentElement)) {
            return event.target.parentElement;
        }
    }

    function isDraggable(element) {
        return hasId(element);
    }

    function hasId(element){
        return typeof element.id != 'undefined' && element.id != '';
    }

    function update(svgElem, position, translation) {
        var updateOnServer = false;
        if (updateOnServer) {
            updateOnServer();
        } else {
            // Adjust the id of the element that has been moved
            // It we were moving a bus node (its circle),
            // find the id of the corresponding voltage level
            var parent = svgElem.parentElement;
            idParent = parent.getAttribute("id");
            if (idParent) {
                id = idParent;
            }
            updateOnClient(id, position, translation);
        }
    }

    function updateOnClient(id, position, translation) {
        console.log("updateOnClient, moved node id = " + id);
        translateText(id, translation);
        for (var edge of svg.getElementsByTagName("nad:edge")) {
            var node1 = edge.getAttribute("node1");
            var node2 = edge.getAttribute("node2");
            console.log("  consider edge (" + node1 + ", " + node2 + ") after moved node " + id);
            if (node1 === id || node2 === id) {
                updateEdge(edge, node1, node2, id, position, translation);
            }
        }
    }

    function updateEdge(edgeMetadata, node1, node2, movedNodeId, position, translation) {
        const DEBUG_EXPERIMENTS = false;
        if (DEBUG_EXPERIMENTS) {
            updateEdgeExperiments(edgeMetadata, node1, node2, id, position, translation);
        } else {
            updateEdgeUsingRotation(edgeMetadata, node1, node2, id, position, translation);
        }
    }

    function updateEdgeUsingRotation(edgeMetadata, node1, node2, movedNodeId, position, translation) {
        // This edge is adjacent to the moved node
        console.log("  adjacent edge (" + node1 + ", " + node2 + ")");
        var edgeId = edgeMetadata.getAttribute("diagramid");
        var edgeSvg = svg.getElementById(edgeId);
        if (!edgeSvg) {
            return;
        }

        // Translate all edge-related graphical elements to the new position
        translate(svg, edgeSvg, translation);

        // If the other node of the edge is visible in the diagram,
        // rotate the edge towards it.
        // rotation centered on moved node
        // (now we may not "reach" the other node, but we point in the right direction)
        var otherNodeId = movedNodeId === node1 ? node2 : node1;
        var otherNodeSvg = svg.getElementById(otherNodeId);
        console.log("  other node is " + otherNodeId + ", svg = " + otherNodeSvg);
        if (otherNodeSvg) {
            rotateEdge(edgeId, edgeSvg, movedNodeId, otherNodeId, otherNodeSvg, position, translation);
        }
    }

    function rotateEdge(edgeId, edgeSvg, movedNodeId, otherNodeId, otherNodeSvg, position, translation) {
        var otherNodeCenter = center(otherNodeSvg);
        var movedNodeMetadata = svg.querySelectorAll('nad\\\\:node[diagramid="' + movedNodeId + '"]')[0];
        var otherNodeMetadata = svg.querySelectorAll('nad\\\\:node[diagramid="' + otherNodeId + '"]')[0];
        var position0 = {x: movedNodeMetadata.getAttribute("x"), y: movedNodeMetadata.getAttribute("y")};
        var otherNodePosition0 = {x: otherNodeMetadata.getAttribute("x"), y: otherNodeMetadata.getAttribute("y")};
        var rotation0 = calcRotation(position0, otherNodePosition0);
        var rotation1 = calcRotation(position, otherNodeCenter);
        updateRotation(svg, edgeSvg, edgeId, position0, position, rotation0, rotation1);
        // Add a debug line connecting the two ends of the edge
        // It will be useful as an indication for the "continuation" of the edge,
        // that we have not scaled/elongated to cover the whole current segment between them
        debugConnection(svg, edgeSvg, edgeId, position, otherNodeCenter);
    }

    function calcRotation(p, q) {
        var rotation = Math.atan2(q.y - p.y, q.x - p.x);
        return rotation * 180 / Math.PI;
    }

    function center(svgElem) {
        var rect = svgElem.getBoundingClientRect();
        var center = {x: rect.x + .5 * rect.width, y: rect.y + .5 * rect.height};
        var CTM = svg.getScreenCTM();
        if (CTM) {
            center.x = (center.x - CTM.e) / CTM.a;
            center.y = (center.y - CTM.f) / CTM.d;
        }
        return center;
    }

    function translateText(id, translation) {
        // Find the graphical element corresponding to the right end
        var textNodeId = id + "-textnode";
        var g = svg.getElementById(textNodeId);
        console.log("  translate textNode " + id + ", textNodeId = " + textNodeId + ", g = " + g);
        if (g) {
            translate(svg, g, translation);
        }
        var textEdgeId = id + "-textedge";
        g = svg.getElementById(textEdgeId);
        console.log("  translate textEdge " + id + ", textEdgeId = " + textEdgeId + ", g = " + g);
        if (g) {
            translate(svg, g, translation);
        }
    }

    // Experiments

    // The main problem with these experimental updates is that we have too much information about the graphical elements of the edge
    // If the way of drawing the edge changes we will fail
    function updateEdgeExperiments(edgeMetadata, node1, node2, movedNodeId, position, translation) {
        // This edge is adjacent to the moved node
        console.log("  adjacent edge (" + node1 + ", " + node2 + ")");
        var edgeId = edgeMetadata.getAttribute("diagramid");
        var edgeSvg = svg.getElementById(edgeId);
        if (!edgeSvg) {
            return;
        }
        var otherNodeId = movedNodeId === node1 ? node2 : node1;
        var otherNodeSvg = svg.getElementById(otherNodeId);
        console.log("  other node is " + otherNodeId + ", svg = " + otherNodeSvg);
        if (otherNodeSvg) {
            var end = movedNodeId === node1 ? 1 : 2;
            updateEdgeEndOfMovedNode(edgeId, end, translation);
            //updateEdgePoints(edgeId, end, translation);
        } else {
            translate(svg, edgeSvg, translation);
        }
    }

    var edge_end_original_point = {};
    function updateEdgeEndOfMovedNode(edgeId, end, translation) {
        // Find the graphical element corresponding to the right end
        var endId = edgeId + "." + end;
        var g = svg.getElementById(endId);
        var polyline = g.querySelector("polyline");
        // We must apply the translation to the original first point of the polyline
        // So we store the original points
        if (!(endId in edge_end_original_point)) {
            edge_end_original_point[endId] = {x: polyline.points[0].x, y: polyline.points[0].y};
        }
        polyline.points[0].x = edge_end_original_point[endId].x + translation.x;
        polyline.points[0].y = edge_end_original_point[endId].y + translation.y;
        // Instead we add a new point
        //var svgPoint = svg.createSVGPoint();
        //svgPoint.x = coords.x;
        //svgPoint.y = coords.y;
        //polyline.points.insertItemBefore(svgPoint, 0)
    }

    var edge_end_original_points = {};
    function updateEdgePoints(edgeId, end, translation) {
        // only for branches with 2 ends
        // Find the graphical element corresponding to the right end
        var endId = edgeId + "." + end;
        var g = svg.getElementById(endId);
        var polyline = g.querySelector("polyline");
        // Store the original points
        if (!(endId in edge_end_original_points)) {
            edge_end_original_points[endId] = [
                {x: polyline.points[0].x, y: polyline.points[0].y}, // close to the bus
                {x: polyline.points[1].x, y: polyline.points[1].y} // close to the middle of the edge
                ];
        }
        polyline.points[0].x = edge_end_original_points[endId][0].x + translation.x;
        polyline.points[0].y = edge_end_original_points[endId][0].y + translation.y;

        var otherEndId = edgeId + "." + (3 - end);
        var g1 = svg.getElementById(otherEndId);
        var polyline1 = g1.querySelector("polyline");
        if (!(otherEndId in edge_end_original_points)) {
            edge_end_original_points[otherEndId] = [
                {x: polyline1.points[0].x, y: polyline1.points[0].y},
                {x: polyline1.points[1].x, y: polyline1.points[1].y}
                ];
        }

        var x0 = polyline.points[0].x;
        var y0 = polyline.points[0].y;
        var x1 = polyline1.points[0].x;
        var y1 = polyline1.points[0].y;

        // If we have a circle is a transformer
        var middlePointFactor = 0.5;
        var circle = g.querySelector("circle");
        if (circle) {
            middlePointFactor = 0.4
            circle.setAttribute("cx", x0 + (x1 - x0) * 0.45);
            circle.setAttribute("cy", y0 + (y1 - y0) * 0.45);
            circle = g1.querySelector("circle");
            circle.setAttribute("cx", x1 + (x0 - x1) * 0.45);
            circle.setAttribute("cy", y1 + (y0 - y1) * 0.45);
        }
        polyline.points[1].x = x0 + (x1 - x0) * middlePointFactor;
        polyline.points[1].y = y0 + (y1 - y0) * middlePointFactor;
        polyline1.points[1].x = x1 + (x0 - x1) * middlePointFactor;
        polyline1.points[1].y = y1 + (y0 - y1) * middlePointFactor;
    }
}

// SVG helper tools

const SVG_NAMESPACE = "http://www.w3.org/2000/svg";
const DEBUG_COLOR = {
    "debug-rotation0": "#CCCCCC",
    "debug-rotation1": "#888888",
    "debug-connection": "#CCCCCC"};

function getCreateDebugRotation(svg, debugRotationId) {
    var debugSvg = svg.getElementById(debugRotationId);
    if (!debugSvg) {
        // https://www.motiontricks.com/creating-dynamic-svg-elements-with-javascript/
        var polyline = document.createElementNS(SVG_NAMESPACE, "polyline");
        polyline.setAttribute("id", debugRotationId);
        polyline.setAttribute("points", "0, 0 50, 0 40, 10 50, 0, 40, -10");
        polyline.setAttribute("style", "fill:none;stroke:" + DEBUG_COLOR[debugRotationId] + ";stroke-width:2");
        svg.appendChild(polyline);
        console.log("created debug element [" + debugRotationId + "]");
        debugSvg = polyline
    }
    return debugSvg;
}

function debugRotation(svg, cx, cy, angle, debugRotationId) {
    debugSvg = getCreateDebugRotation(svg, debugRotationId);
    if (debugSvg) {
        translate(svg, debugSvg, {x: cx, y: cy});
        var transforms = debugSvg.transform.baseVal;
        if (transforms.length === 1) {
            rotate = svg.createSVGTransform();
            debugSvg.transform.baseVal.appendItem(rotate);
        }
        transforms.getItem(1).setRotate(angle, 0, 0);
    }
}

function debugConnection(svg, edgeSvg, edgeId, p0, p1) {
    var debugConnectionId = edgeId + "-debug";
    var debugSvg = svg.getElementById(debugConnectionId);
    if (!debugSvg) {
        var polyline = document.createElementNS(SVG_NAMESPACE, "polyline");
        polyline.setAttribute("id", debugConnectionId);
        polyline.setAttribute("points", p0.x + ", " + p0.y + " " + p1.x + " " + p1.y);
        polyline.setAttribute("style", "fill:none;stroke:" + DEBUG_COLOR["debug-connection"] + ";stroke-width:2");
        svg.appendChild(polyline);
        console.log("created debug element [" + debugConnectionId + "]");
    } else {
        var polyline = debugSvg;
        polyline.points.clear();
        var p = svg.createSVGPoint();
        p.x = p0.x;
        p.y = p0.y;
        polyline.points.appendItem(p);
        p = svg.createSVGPoint();
        p.x = p1.x;
        p.y = p1.y;
        polyline.points.appendItem(p);
    }
}

function debugRotations(svg, cx, cy, angle0, angle1) {
    debugRotation(svg, cx, cy, angle0, "debug-rotation0");
    debugRotation(svg, cx, cy, angle1, "debug-rotation1");
}

function translate(svg, svgElem, translation) {
    getTransformsEnsuringFirstIsTranslation(svg, svgElem).getItem(0).setTranslate(translation.x, translation.y);
}

var rotations = {}
function updateRotation(svg, svgElem, id, position0, position, angle0, angle1) {
    debugRotations(svg, position.x, position.y, angle0, angle1);
    var transforms = svgElem.transform.baseVal;
    var rotate = null;
    if (!(id in rotations)) {
        rotate = svg.createSVGTransform();
        svgElem.transform.baseVal.appendItem(rotate);
        rotations[id] = rotate;
    }
    rotate = rotations[id];
    // We append the rotation at the list of transforms,
    // Order from left to right is order of nested transform,
    // The last transform in the list is the first applied,
    // We have to rotate centered on the initial position
    // We want to keep the first position for the (generic) translations
    rotate.setRotate(angle1 - angle0, position0.x, position0.y);
}

function getTransformsEnsuringFirstIsTranslation(svg, svgElem) {
    // Get all the transforms currently on this element
    var transforms = svgElem.transform.baseVal;
    // Ensure the first transform is a translate transform
    if (transforms.length === 0 ||
        transforms.getItem(0).type !== SVGTransform.SVG_TRANSFORM_TRANSLATE) {
        // Create an transform that translates by (0, 0)
        var translate = svg.createSVGTransform();
        translate.setTranslate(0, 0);
        // Add the translation to the front of the transforms list
        svgElem.transform.baseVal.insertItemBefore(translate, 0);
    }
    return transforms;
}

// Updated positions processed on server

function updateDiagramOnServer(svg) {
    // In this version we simply ignore the updated position of the diagram element,
    // we just gather all positions and fire a global update of the diagram.
    // An alternative could be to request a diagram update passing only the change in the element moved.
    // Or, we could keep a local list of new locations for some equipment
    // and wait until the user confirms the re-layout with all the updated positions.
    positions = gatherAllEquipmentPositions(svg);
    if (typeof jsHandler !== 'undefined') {
        jsHandler.updateDiagramWithPositions(JSON.stringify(positions));
    }
}

function gatherAllEquipmentPositions(svg) {
    var positions = {};
    // Store all node id mappings in a dictionary
    // With the initial positions
    var diagramId2EquipmentId = {};
    for (node of svg.getElementsByTagName("nad:node")) {
        id = node.getAttribute("diagramid");
        equipmentId = node.getAttribute("equipmentid");
        diagramId2EquipmentId[id] = equipmentId;
        positions[equipmentId] = {x: node.getAttribute("x"), y: node.getAttribute("y")};
    }
    // For all elements in svg that have the id property, update the position of equipment id
    var CTM = svg.getScreenCTM();
    for (svgElem of svg.querySelectorAll("[id]")) {
        if (svgElem.id in diagramId2EquipmentId) {
            equipmentId = diagramId2EquipmentId[svgElem.id];
            // getBBox returns coordinates without considering transforms to the element itself or its parents
            // getBoundingClientRect returns actual coordinates but in screen space
            var rect = svgElem.getBoundingClientRect();
            var point = {x: rect.x + .5 * rect.width, y: rect.y + .5 * rect.height};
            point.x = (point.x - CTM.e) / CTM.a;
            point.y = (point.y - CTM.f) / CTM.d;
            positions[equipmentId] = point;
        }
    }
    return positions;
}
