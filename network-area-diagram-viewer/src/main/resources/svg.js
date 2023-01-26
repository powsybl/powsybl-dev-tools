/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
// TODO(Luma) change text orientation or hide node infos if rotation makes them appear inverted
// TODO(Luma) how to handle multi-lines without having to split stretchable and glued to end? (avoid gaps and/or overlaps)
// TODO(Luma) redraw the annuli
// TODO(Luma) update on client: find related hidden nodes and update initial position in metadata?

// FIXME(Luma) these should not be parameters of the diagram, they should be computed for each edge drawing
const XXX_NON_STRETCHABLE_SIDE_SIZE = 27.5;
const XXX_NON_STRETCHABLE_CENTER_SIZE = 60;

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
    var diagram = new Diagram(svg, svgTools, XXX_NON_STRETCHABLE_SIDE_SIZE, XXX_NON_STRETCHABLE_CENTER_SIZE);

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

        var mouse0 = getMousePosition(event);
        var transforms = svgTools.getTransformsEnsuringFirstIsTranslation(selectedElement);
        // Get initial translation amount
        transform = transforms.getItem(0);
        // Save translation of selected element at the start of dragging operation
        translation0 = {x: transform.matrix.e, y: transform.matrix.f};
        offset = {x: mouse0.x - translation0.x, y: mouse0.y - translation0.y};
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
        var mouse1 = getMousePosition(event);
        var translation = {x: mouse1.x - offset.x, y: mouse1.y - offset.y};
        transform.setTranslate(translation.x, translation.y);
        translationForOthers = {x: translation.x - translation0.x, y: translation.y - translation0.y}
        diagram.update(selectedElement, translationForOthers);
        translation0 = {x: transform.matrix.e, y: transform.matrix.f};
    }
}

// PowSyBl (Network Area) Diagram

function Diagram(svg, svgTools, nonStretchableSideSize, nonStretchableCenterSize) {
    this.getDraggableFrom = getDraggableFrom;
    this.update = update;
    this.nonStretchableSideSize = nonStretchableSideSize;
    this.nonStretchableCenterSize = nonStretchableCenterSize;

    function getDraggableFrom(element) {
        if (isDraggable(element)) {
            return element;
        } else if (element.parentElement) {
            return getDraggableFrom(element.parentElement);
        }
    }

    function isDraggable(element) {
        return hasId(element) && element.parentElement && classIsContainerOfDraggables(element.parentElement);
    }

    function classIsDraggable(element) {
        return element.classList.contains("nad-vl-node") || element.classList.contains("nad-boundary-node");
    }

    function classIsContainerOfDraggables(element) {
        return element.classList.contains("nad-vl-nodes")
            || element.classList.contains("nad-boundary-nodes")
            || element.classList.contains("nad-3wt-nodes");
    }

    function update(svgElem, translation) {
        // the element that is being moved has to have an id
        var id = svgElem.getAttribute("id");
        if (id) {
            updateOnClient(id, svgElem, translation);
        } else {
            console.log("error trying to update: moved element has no id attribute");
        }
    }

    var cachedEdgesForId;
    var cachedEdges;
    function updateOnClient(id, nodeSvg, translation) {
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
            updateEdge(edge, node1, node2, id, nodeSvg, translation);
        }
    }

    function updateEdge(edgeMetadata, node1, node2, movedNodeId, movedNodeSvg, translation) {
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
                transformEdge(edgeId, edgeSvg, movedNodeId, movedNodeSide, movedNodeSvg, otherNodeId, otherNodeSvg);
            }
        }
    }

    var cachedEdgeDistances0 = {};
    function transformEdge(edgeId, edgeSvg, movedNodeId, movedNodeSide, movedNodeSvg, otherNodeId, otherNodeSvg) {
        var movedNodeMetadata = svg.querySelector('nad\\\\:node[svgid="' + movedNodeId + '"]');
        var otherNodeMetadata = svg.querySelector('nad\\\\:node[svgid="' + otherNodeId + '"]');
        var p0 = {x: parseFloat(movedNodeMetadata.getAttribute("x")), y: parseFloat(movedNodeMetadata.getAttribute("y"))};
        var q0 = {x: parseFloat(otherNodeMetadata.getAttribute("x")), y: parseFloat(otherNodeMetadata.getAttribute("y"))};
        var a0 = calcRotation(p0, q0);
        var p1 = center(movedNodeSvg);
        var q1 = center(otherNodeSvg);
        var a1 = calcRotation(p1, q1);

        if (!(edgeId in cachedEdgeDistances0)) {
            var dx0 = p0.x - q0.x;
            var dy0 = p0.y - q0.y;
            cachedEdgeDistances0[edgeId] = Math.sqrt(dx0*dx0 + dy0*dy0);
        }
        var d0 = cachedEdgeDistances0[edgeId];
        var dx1 = p1.x - q1.x;
        var dy1 = p1.y - q1.y;
        var d1 = Math.sqrt(dx1*dx1 + dy1*dy1);
        var nonStretchables = findNonStretchables(edgeSvg);
        var s = (d1 - nonStretchables.d) / (d0 - nonStretchables.d);

        if (isDanglingLine(edgeSvg)) {
            // Rotate the boundary node according to new edge orientation
            updateBoundaryNode((movedNodeSide === 2 ? movedNodeId : otherNodeId), a0, a1);
        }
        updateEdgeTransform(edgeSvg, edgeId, movedNodeSide, p0, q0, p1, q1, a0, a1, s, nonStretchables);
    }

    function findNonStretchables(edgeSvg) {
        // For 3wt:
        // although the side close to the center node does not have any glued part,
        // we have to keep a distance from the center node drawing
        if (edgeSvg.parentElement.classList.contains("nad-3wt-edges")) {
            return {dside: nonStretchableSideSize, d: nonStretchableSideSize + nonStretchableSideSize}
        } else {
            return {dside: nonStretchableSideSize, d: nonStretchableSideSize + nonStretchableSideSize + nonStretchableCenterSize}
        }
    }

    var edgeTransforms = {};
    function updateEdgeTransform(svgElem, edgeId, movedNodeSide, p0, q0, p1, q1, a0, a1, s, nonStretchables) {
        for (const part of svgElem.getElementsByTagName("*")) {
            if (part.transform && isEdgePartUpdatable(part)) {
                createCachedTransform(edgeId, part);
                updateEdgePartTransform(part, edgeTransforms[edgeId][part.id], movedNodeSide, p0, q0, p1, q1, a0, a1, s, nonStretchables);
            }
        }
    }

    var boundaryNodeTransforms = {};
    function updateBoundaryNode(nodeId, a0, a1) {
        var nodeSvg = svg.getElementById(nodeId);
        if (!(nodeId in boundaryNodeTransforms)) {
            var transform = svg.createSVGTransform();
            nodeSvg.transform.baseVal.appendItem(transform);
            boundaryNodeTransforms[nodeId] = transform;
        }
        boundaryNodeTransforms[nodeId].setRotate(a1 - a0, 0, 0);
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

    function updateEdgePartTransform(svgEdgePart, transform, movedNodeSide, p0, q0, p1, q1, a0, a1, s, nonStretchables) {
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
        var referencePoint0 = p0;
        var referencePoint1 = p1;
        var gluedToSide = getGluedToSide(svgEdgePart);
        if (gluedToSide && movedNodeSide != gluedToSide) {
            referencePoint0 = q0;
            referencePoint1 = q1;
            nonStretchables.dside = -nonStretchables.dside;
        } else if (isGluedToCenter(svgEdgePart)) {
            referencePoint1 = {x: (p1.x + q1.x)/2, y: (p1.y + q1.y)/2};
            referencePoint0 = {x: (p0.x + q0.x)/2, y: (p0.y + q0.y)/2};
        }
        transform.setMatrix(svg.createSVGMatrix()
            .translate(referencePoint1.x, referencePoint1.y)
            .rotate(a1)
            .translate(nonStretchables.dside, 0)
            .scaleNonUniform((isStretchable(svgEdgePart) ? s : 1), 1)
            .translate(-nonStretchables.dside, 0)
            .rotate(-a0)
            .translate(-referencePoint0.x, -referencePoint0.y));
    }

    function isDanglingLine(svgElem) {
        return svgElem.classList.contains("nad-dangling-line-edge");
    }

    function getGluedToSide(svgElem) {
        if (svgElem.classList.contains("nad-glued-1")) {
            return 1;
        } else if (svgElem.classList.contains("nad-glued-2")) {
            return 2;
        }
    }

    function isGluedToCenter(svgElem) {
        return svgElem.classList.contains("nad-glued-center");
    }

    function isEdgePartUpdatable(element) {
        return element.classList.contains("nad-glued-center")
            || element.classList.contains("nad-glued-1")
            || element.classList.contains("nad-glued-2")
            || element.classList.contains("nad-stretchable");
    }

    function isStretchable(element) {
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
    this.translate = translate;
    this.getTransformsEnsuringFirstIsTranslation = getTransformsEnsuringFirstIsTranslation;

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
    if (dx != 0) {
        var m = (q.y - p.y)/dx;
        x = d / Math.sqrt(1 + m*m);
        y = m*x;
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


