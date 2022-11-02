
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

            // TODO(Luma) allow drag multiple equipment before updating the diagram
            // We could keep a local list of new locations for some equipment
            // and wait until the user confirms the re-layout with all the updates

            // Obtain the equipmentId from the metadata and pass it to the jsHandler with the new position for it
            var equipmentId = findEquipmentId(svg, selectedElement.id);
            // Final desired position for the equipment is current mouse position in SVG coordinate system
            var coords = getMousePosition(event);
            if (typeof jsHandler !== 'undefined') {
                jsHandler.updateDiagramWithEquipmentLocation(equipmentId, coords.x, coords.y);
            }
        }
        selectedElement = null;
    }
}

function isDraggable(element) {
    return hasId(element);
}

function hasId(element){
    return typeof element.id != 'undefined' && element.id != '';
}

function findEquipmentId(svg, svgId) {
    console.log("findEquipmentId (" + svgId + ")");
    for (node of svg.getElementsByTagName("nad:node")) {
        if (node.getAttribute("diagramid") === svgId) {
            console.log("  found node " + node.getAttribute("equipmentid"));
            return node.getAttribute("equipmentid");
        }
    }
    for (node of svg.getElementsByTagName("nad:busnode")) {
        if (node.getAttribute("diagramid") === svgId) {
            console.log("  found busnode " + node.getAttribute("equipmentid"));
            return node.getAttribute("equipmentid");
        }
    }
}

