
window.addEventListener('load', function() {
    var svg = document.getElementsByTagName("svg")[0];
    if (svg != null) {
        makeDraggableSvg(svg);
    }
});

// Draggable SVG Elements from
// https://www.petercollingridge.co.uk/tutorials/svg/interactive/dragging/
function makeDraggableSvg(svg) {
    var selectedElement = false, offset, transform;

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
        if (isDraggable(event.target)) {
            selectedElement = event.target;
        } else if (isDraggable(event.target.parentElement)) {
            selectedElement = event.target.parentElement;
        } else {
            return;
        }
        selectedElement.style.cursor = 'grabbing';

        offset = getMousePosition(event);
        // Get all the transforms currently on this element
        var transforms = selectedElement.transform.baseVal;
        // Ensure the first transform is a translate transform
        if (transforms.length === 0 ||
            transforms.getItem(0).type !== SVGTransform.SVG_TRANSFORM_TRANSLATE) {
            // Create an transform that translates by (0, 0)
            var translate = svg.createSVGTransform();
            translate.setTranslate(0, 0);
            // Add the translation to the front of the transforms list
            selectedElement.transform.baseVal.insertItemBefore(translate, 0);
        }
        // Get initial translation amount
        transform = transforms.getItem(0);
        offset.x -= transform.matrix.e;
        offset.y -= transform.matrix.f;
    }

    function drag(event) {
        if (selectedElement) {
            event.preventDefault();
            var coords = getMousePosition(event);
            transform.setTranslate(coords.x - offset.x, coords.y - offset.y);
        }
    }

    function endDrag(event) {
        if (selectedElement) {
            console.log("endDrag for SVG element " + selectedElement.id);
            selectedElement.style.cursor = 'grab';
            // Desired position for the selected element is current mouse position in SVG coordinate system
            updateDiagram(svg, selectedElement.id, getMousePosition(event));
        }
        selectedElement = null;
    }
}

function updateDiagram(svg, diagramId, coords) {
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

function isDraggable(element) {
    return hasId(element);
}

function hasId(element){
    return typeof element.id != 'undefined' && element.id != '';
}
