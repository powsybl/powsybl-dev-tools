
// TODO(Luma) change text orientation or hide node infos if rotation makes them appear inverted
// TODO(Luma) how to handle multi-lines without having to split stretchable and glued to end? (avoid gaps and/or overlaps)
// TODO(Luma) redraw the annuli
// TODO(Luma) consider three winding transformers
// TODO(Luma) update on client: find related hidden nodes and update initial position in metadata?

const DIAGRAM_UPDATE_ON_SERVER = false;
const DIAGRAM_UPDATE_WHILE_DRAG = true;
const DIAGRAM_SCALE_ALL_EDGE_PARTS = false;
// TODO(Luma) this distance must be computed or glue point calculation avoided
const DIAGRAM_DISTANCE_FROM_NODE_TO_GLUED_PART = 70;
const DIAGRAM_DEBUG_EDGE_ROTATION = false;
const DIAGRAM_DEBUG_GLUE_POINT = false;

window.addEventListener('load', function() {
    var svgDiagram = document.getElementsByTagName("svg")[0];
    if (svgDiagram != null) {
        makeDraggableSvg(svgDiagram);
    }
});

// Draggable SVG Elements from
// https://www.petercollingridge.co.uk/tutorials/svg/interactive/dragging/
function makeDraggableSvg(svg) {
    var selectedElement = false, offset, transform, translation0;
    var svgTools = new SvgTools(svg);
    var diagram = new Diagram(svg, svgTools, DIAGRAM_UPDATE_WHILE_DRAG);

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
        var draggableElem = diagram.getDraggableFrom(event.target);
        if (!draggableElem) {
            return;
        }
        selectedElement = draggableElem;
        selectedElement.style.cursor = 'grabbing';

        offset = getMousePosition(event);
        var transforms = svgTools.getTransformsEnsuringFirstIsTranslation(selectedElement);
        // Get initial translation amount
        transform = transforms.getItem(0);
        // Save inital translation of selected element
        translation0 = {x: transform.matrix.e, y: transform.matrix.f};
        offset.x -= transform.matrix.e;
        offset.y -= transform.matrix.f;
    }

    function drag(event) {
        if (selectedElement) {
            event.preventDefault();
            updateAfter(event, true);
        }
    }

    function endDrag(event) {
        if (selectedElement) {
            updateAfter(event, false);
            selectedElement.style.cursor = 'grab';
            selectedElement = null;
        }
    }

    function updateAfter(event, dragInProgress) {
        var position = getMousePosition(event);
        position = {x: position.x - offset.x, y: position.y - offset.y};
        transform.setTranslate(position.x, position.y);
        if (!dragInProgress || diagram.updateWhileDrag()) {
            var translation = {x: position.x - translation0.x, y: position.y - translation0.y};
            diagram.update(selectedElement, position, translation);
            translation0 = {x: transform.matrix.e, y: transform.matrix.f};
        }
    }
}

// PowSyBl (Network Area) Diagram

function Diagram(svg, svgTools, updateWhileDrag) {
    this.getDraggableFrom = getDraggableFrom;
    this.update = update;
    this.updateWhileDrag = () => updateWhileDrag;

    function getDraggableFrom(element) {
        if (isDraggable(element)) {
            return element;
        } else if (element.parentElement) {
            return getDraggableFrom(element.parentElement);
        }
    }

    function isDraggable(element) {
        // TODO(Luma) if we decide to add explicit classes for every drawing we could use
        //return hasId(element) && classIsDraggable(element);
        return hasId(element) && element.parentElement && classIsContainerOfDraggables(element.parentElement);
    }

    function classIsDraggable(element) {
        return element.classList.contains("nad-vl-node") || element.classList.contains("nad-boundary-node");
    }

    function classIsContainerOfDraggables(element) {
        return element.classList.contains("nad-vl-nodes") || element.classList.contains("nad-boundary-nodes");
    }

    function update(svgElem, position, translation) {
        if (DIAGRAM_UPDATE_ON_SERVER) {
            updateOnServer(svg);
        } else {
            // the element that is being moved has to have an id
            var id = svgElem.getAttribute("id");
            if (id) {
            updateOnClient(id, position, translation);
            } else {
                console.log("error trying to update: moved element has no id attribute");
            }
        }
    }

    var cachedEdgesForId;
    var cachedEdges;
    function updateOnClient(id, position, translation) {
        translateText(id, translation);
        var edges;
        if (id != cachedEdgesForId) {
            cachedEdges = svg.querySelectorAll('nad\\\\:edge[node1="' + id + '"], nad\\\\:edge[node2="' + id + '"]')
            cachedEdgesForId = id;
        }
        edges = cachedEdges;
        // TODO(Luma) Update here the position of all inner busNodes
        for (var edge of edges) {
            var node1 = edge.getAttribute("node1");
            var node2 = edge.getAttribute("node2");
            updateEdge(edge, node1, node2, id, position, translation);
        }
    }

    function updateEdge(edgeMetadata, node1, node2, movedNodeId, position, translation) {
        // This edge is adjacent to the moved node
        var edgeId = edgeMetadata.getAttribute("svgid");
        var edgeSvg = svg.getElementById(edgeId);
        if (!edgeSvg) {
            return;
        }

        // If it is a loop, just translate
        if (node1 === node2) {
            svgTools.translate(edgeSvg, translation);
        } else {
            var movedNodeSide = movedNodeId === node1 ? 1 : 2;
            var otherNodeId = movedNodeId === node1 ? node2 : node1;
            var otherNodeSvg = svg.getElementById(otherNodeId);
            if (!otherNodeSvg) {
                svgTools.translate(edgeSvg, translation);
            } else {
                // If the other node of the edge is visible in the diagram,
                // the transform to apply on edge drawings is complex: 
                // we have to rotate them to point to the new location of the moved node
                // and expand (the stretchable parts) so the drawing fills the new distance between nodes
                transformEdge(edgeId, edgeSvg, movedNodeId, movedNodeSide, otherNodeId, otherNodeSvg, position);
            }
        }
    }

    var cachedEdgeDistances0 = {};
    function transformEdge(edgeId, edgeSvg, movedNodeId, movedNodeSide, otherNodeId, otherNodeSvg, p1) {
        var q1 = center(otherNodeSvg);
        var a1 = calcRotation(p1, q1);

        var movedNodeMetadata = svg.querySelector('nad\\\\:node[svgid="' + movedNodeId + '"]');
        var otherNodeMetadata = svg.querySelector('nad\\\\:node[svgid="' + otherNodeId + '"]');
        var p0 = {x: parseFloat(movedNodeMetadata.getAttribute("x")), y: parseFloat(movedNodeMetadata.getAttribute("y"))};
        var q0 = {x: parseFloat(otherNodeMetadata.getAttribute("x")), y: parseFloat(otherNodeMetadata.getAttribute("y"))};
        var a0 = calcRotation(p0, q0);

        if (!(edgeId in cachedEdgeDistances0)) {
            var dx0 = p0.x - q0.x;
            var dy0 = p0.y - q0.y;
            cachedEdgeDistances0[edgeId] = dx0*dx0 + dy0*dy0;
        }
        var d02 = cachedEdgeDistances0[edgeId];
        var dx1 = p1.x - q1.x;
        var dy1 = p1.y - q1.y;
        var s = Math.sqrt((dx1*dx1 + dy1*dy1)/d02);

        updateEdgeTransform(edgeSvg, edgeId, movedNodeSide, p0, q0, p1, q1, a0, a1, s);

        if (DIAGRAM_DEBUG_EDGE_ROTATION) {
            svgTools.debugRotation(p1.x, p1.y, a0, "debug-rotation0");
            svgTools.debugRotation(p1.x, p1.y, a1, "debug-rotation1");
            svgTools.debugConnection(edgeSvg, edgeId, p1, q1);
        }
    }

    var edgeTransforms = {};
    function updateEdgeTransform(svgElem, edgeId, movedNodeSide, p0, q0, p1, q1, a0, a1, s) {
        for (const part of svgElem.getElementsByTagName("*")) {
            if (part.transform && isEdgePartUpdatable(part)) {
                createCachedTransform(edgeId, part);
                updateEdgePartTransform(part, edgeTransforms[edgeId][part.id], movedNodeSide, p0, q0, p1, q1, a0, a1, s);
            }
        }
    }

    function createCachedTransform(edgeId, part) {
        if (!part.id) {
            part.id = uniqueIdentifier();
        }
        if (!(edgeId in edgeTransforms)) {
            edgeTransforms[edgeId] = {};
        }
        if (!edgeTransforms[edgeId][part.id]) {
            // We can not reuse the same transform for multiple elements,
            // when we add a transform to a list, it is removed from the previous one
            // if it was inserted in one.
            // From https://developer.mozilla.org/en-US/docs/Web/API/SVGTransformList:
            // "If newItem is already in a list, it is removed from its previous list
            // before it is inserted into this list"
            var transform = svg.createSVGTransform();
            part.transform.baseVal.insertItemBefore(transform, 0);
            edgeTransforms[edgeId][part.id] = transform;
        }
    }

    function updateEdgePartTransform(svgEdgePart, transform, movedNodeSide, p0, q0, p1, q1, a0, a1, s) {
        // The moved node was located at point p0, now it is at p1
        // The other node of edge was initially at q0, but its current position is q1

        // We first undo the rotation of the segment (p0, q0) respect the x axis
        // by moving to p0, rotating -a0
        // then we re-scale the part if it is stretchable (scale only over the x axis)
        // and then rotate to get the current orientation of the edge following the segment (p1, q1).
        // After that we translate everything to the new location of the moved node.

        // Order from left to right is order of nested transform,
        // The last transform in the list is the first applied,
        // We have to rotate centered on the initial position.

        // For parts glued to one side of the edge,
        // we need to know to which side (end) they are glued,
        // and which side is the moved node "p".
        // Glued parts receive the same set of transformations with a different reference point.
        var gluedToSide = getGluedToSide(svgEdgePart);
        var referencePoint0 = p0;
        var referencePoint1 = p1;
        if (gluedToSide) {
            if (movedNodeSide != gluedToSide) {
                referencePoint0 = q0;
                referencePoint1 = q1;
            }
        }
        transform.setMatrix(svg.createSVGMatrix()
            .translate(referencePoint1.x, referencePoint1.y)
            .rotate(a1)
            .scaleNonUniform((isStretchable(svgEdgePart) ? s : 1), 1)
            .rotate(-a0)
            .translate(-referencePoint0.x, -referencePoint0.y));
        if (!DIAGRAM_SCALE_ALL_EDGE_PARTS) {
            var gluePoint = getGluedToPoint(svgEdgePart, p1, q1);
            if (gluePoint) {
                var c = center(svgEdgePart);
                var additionalTranslationToGluePoint = {x: gluePoint.x - c.x, y: gluePoint.y - c.y};
                transform.setMatrix(svg.createSVGMatrix()
                    .translate(additionalTranslationToGluePoint.x, additionalTranslationToGluePoint.y)
                    .multiply(transform.matrix));
            }
        }
    }

    function getGluedToSide(svgElem) {
        if (svgElem.classList.contains("nad-glued-1")) {
            return 1;
        } else if (svgElem.classList.contains("nad-glued-2")) {
            return 2;
        }
    }

    function getGluedToPoint(svgElem, p1, q1) {
        if (svgElem.classList.contains("nad-glued-center")) {
            return {x: (p1.x + q1.x)/2, y: (p1.y + q1.y)/2}
        }
    }

    function isEdgePartUpdatable(element) {
        return element.classList.contains("nad-glued-center")
            || element.classList.contains("nad-glued-1")
            || element.classList.contains("nad-glued-2")
            || element.classList.contains("nad-stretchable");
    }

    function isStretchable(element) {
        if (DIAGRAM_SCALE_ALL_EDGE_PARTS) {
            return true;
        }
        return element.classList.contains("nad-stretchable");
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
        var textNodeId = id + "-textnode";
        var g = svg.getElementById(textNodeId);
        if (g) {
            svgTools.translate(g, translation);
        }
        var textEdgeId = id + "-textedge";
        g = svg.getElementById(textEdgeId);
        if (g) {
            svgTools.translate(g, translation);
        }
    }
}

// SVG helper tools
function SvgTools(svg) {
    this.debugPoint = debugPoint;
    this.debugRotation = debugRotation;
    this.debugConnection = debugConnection;
    this.translate = translate;
    this.getTransformsEnsuringFirstIsTranslation = getTransformsEnsuringFirstIsTranslation;

    const SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    const DEBUG_COLOR = {
        "debug-center": "#888888",
        "debug-glue-point": "#888888",
        "debug-moved-node": "#880000",
        "debug-other-node": "#000088",
        "debug-rotation0": "#CCCCCC",
        "debug-rotation1": "#888888",
        "debug-connection": "#CCCCCC"};

    function debugPoint(p, debugPointId) {
        var debugSvg = svg.getElementById(debugPointId);
        if (!debugSvg) {
            var polyline = document.createElementNS(SVG_NAMESPACE, "polyline");
            polyline.setAttribute("id", debugPointId);
            polyline.setAttribute("points", "0,0 30,30 -30,-30 0,0 -30,30 30,-30");
            polyline.setAttribute("style", "fill:none;stroke:" + DEBUG_COLOR[debugPointId] + ";stroke-width:2");
            svg.appendChild(polyline);
            debugSvg = polyline
        }
        var transforms = debugSvg.transform.baseVal;
            if (transforms.length == 0) {
                transforms.appendItem(svg.createSVGTransform());
        }
        console.log("debugPoint " + debugPointId + " " + p.x + ", " + p.y);
        transforms.getItem(0).setTranslate(p.x, p.y);
        return debugSvg;
    }
    function debugRotation(cx, cy, angle, debugRotationId) {
        debugSvg = getCreateDebugRotation(debugRotationId);
        if (debugSvg) {
            var transforms = debugSvg.transform.baseVal;
                if (transforms.length == 0) {
                    transforms.appendItem(svg.createSVGTransform());
            }
            transforms.getItem(0).setMatrix(svg.createSVGMatrix()
                .translate(cx, cy)
                .rotate(angle)
                );
        }
    }

    function debugConnection(edgeSvg, edgeId, p0, p1) {
        var debugConnectionId = edgeId + "-debug";
        var debugSvg = svg.getElementById(debugConnectionId);
        if (!debugSvg) {
            var polyline = document.createElementNS(SVG_NAMESPACE, "polyline");
            polyline.setAttribute("id", debugConnectionId);
            polyline.setAttribute("points", p0.x + ", " + p0.y + " " + p1.x + " " + p1.y);
            polyline.setAttribute("style", "fill:none;stroke:" + DEBUG_COLOR["debug-connection"] + ";stroke-width:2");
            svg.appendChild(polyline);
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

    function getCreateDebugRotation(debugRotationId) {
        var debugSvg = svg.getElementById(debugRotationId);
        if (!debugSvg) {
            // https://www.motiontricks.com/creating-dynamic-svg-elements-with-javascript/
            var polyline = document.createElementNS(SVG_NAMESPACE, "polyline");
            polyline.setAttribute("id", debugRotationId);
            polyline.setAttribute("points", "0, 0 50, 0 40, 10 50, 0, 40, -10");
            polyline.setAttribute("style", "fill:none;stroke:" + DEBUG_COLOR[debugRotationId] + ";stroke-width:2");
            svg.appendChild(polyline);
            debugSvg = polyline
        }
        return debugSvg;
    }

    function translate(svgElem, translation) {
        // Add the given translation to the current one
        var transform = getTransformsEnsuringFirstIsTranslation(svgElem).getItem(0);
        var totalTranslation = {x: transform.matrix.e + translation.x, y: transform.matrix.f + translation.y};
        transform.setTranslate(totalTranslation.x, totalTranslation.y);
    }

    function getTransformsEnsuringFirstIsTranslation(svgElem) {
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
        id = node.getAttribute("svgid");
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

// Identifiers

function hasId(element){
    return typeof element.id != 'undefined' && element.id != '';
}

var uniqueIdentifier = function (id) {
    return function () { 
        return 'uid-' + (++id); 
    };
} (0);

// Geometry

function distance(p, q) {
    var dx = p.x - q.x;
    var dy = p.y - q.y;
    return Math.sqrt(dx*dx + dy*dy);
}

function findPointInSegment(d, p, q) {
    // Find a point at distance d from p that belongs to the segment (p, q)
    var p1 = {x: p.x, y: p.y};
    var x = 0;
    var y = 0;
    var dx = q.x - p.x;
    //console.log("dx = " + dx);
    if (dx != 0) {
        var m = (q.y - p.y)/dx;
        //console.log("m  = " + m);
        x = d / Math.sqrt(1 + m*m);
        y = m*x;
        //console.log("x  = " + x);
        //console.log("y  = " + y);
        if (Math.sign(x) != Math.sign(dx)) {
            x = -x;
            y = -y;
        }
    } else {
        if (p.y < q.y) {
            y = d;
        } else {
            y = - d;
        }
    }
    p1.x += x;
    p1.y += y;
    return p1;
}

function calcRotation(p, q) {
    var rotation = Math.atan2(q.y - p.y, q.x - p.x);
    return rotation * 180 / Math.PI;
}


