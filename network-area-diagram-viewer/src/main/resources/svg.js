
// TODO(Luma) consider three winding transformers
// TODO(Luma) separate components of edges, having one that can be "scaled" (length line from exit of node to middle point)
//              for reference, see Android NinePatch drawables
//              https://developer.android.com/develop/ui/views/graphics/drawables#nine-patch
// TODO(Luma) update on client: find related hidden nodes and update initial position in metadata?

const DIAGRAM_UPDATE_WHILE_DRAG = true;

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

    function hasId(element){
        return typeof element.id != 'undefined' && element.id != '';
    }

    function update(svgElem, position, translation) {
        var updateOnServer = false;
        if (updateOnServer) {
            updateOnServer();
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
            var otherNodeId = movedNodeId === node1 ? node2 : node1;
            var otherNodeSvg = svg.getElementById(otherNodeId);
            if (!otherNodeSvg) {
                svgTools.translate(edgeSvg, translation);
            } else {
                // If the other node of the edge is visible in the diagram,
                // the transform to apply on edge drawings is complex: 
                // we have to rotate them to point to the new location of the moved node
                // and expand (the stretchable parts) so the drawing fills the new distance between nodes
                transformEdge(edgeId, edgeSvg, movedNodeId, otherNodeId, otherNodeSvg, position);
            }
        }
    }

    var cachedEdgeDistances0 = {};
    function transformEdge(edgeId, edgeSvg, movedNodeId, otherNodeId, otherNodeSvg, p1) {
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

        updateEdgeTransform(edgeSvg, edgeId, p0, p1, a0, a1, s);

        //svgTools.debugRotation(p1.x, p1.y, a0, "debug-rotation0");
        //svgTools.debugRotation(p1.x, p1.y, a1, "debug-rotation1");
        //svgTools.debugConnection(edgeSvg, edgeId, p1, q1);
    }

    var edgeTransforms = {}
    function updateEdgeTransform(svgElem, id, p0, p1, a0, a1, s) {
        var transform = null;
        if (!(id in edgeTransforms)) {
            transform = svg.createSVGTransform();
            // We append the rotation at the list of transforms,
            // Order from left to right is order of nested transform,
            // The last transform in the list is the first applied,
            // We have to rotate centered on the initial position
            // We want to keep the first position for the (generic) translations
            svgElem.transform.baseVal.appendItem(transform);
            edgeTransforms[id] = transform;
        }
        edgeTransforms[id].setMatrix(svg.createSVGMatrix()
            .translate(p1.x, p1.y)
            .rotate(a1)
            .scale(s, 0)
            .rotate(-a0)
            .translate(-p0.x, -p0.y)
            );
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
    this.debugRotation = debugRotation;
    this.debugConnection = debugConnection;
    this.translate = translate;
    this.getTransformsEnsuringFirstIsTranslation = getTransformsEnsuringFirstIsTranslation;

    const SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    const DEBUG_COLOR = {
        "debug-rotation0": "#CCCCCC",
        "debug-rotation1": "#888888",
        "debug-connection": "#CCCCCC"};

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
