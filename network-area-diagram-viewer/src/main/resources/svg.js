
window.addEventListener('load', function() {
    var svg = document.getElementsByTagName("svg")[0];
    console.log("window.load: svg = " + svg);
    makeDraggableSvg(svg);
});

// Draggable SVG Elements from
// https://www.petercollingridge.co.uk/tutorials/svg/interactive/dragging/
function makeDraggableSvg(svgElem) {
    var svg = svgElem;
    console.log("makeDraggableSvg, svg = " + svg);
    var selectedElement = false, offset, transform;

    svg.addEventListener('mousedown', startDrag);
    svg.addEventListener('mousemove', drag);
    svg.addEventListener('mouseup', endDrag);
    svg.addEventListener('mouseleave', endDrag);

    function getMousePosition(evt) {
        var CTM = svg.getScreenCTM();
        return {
            x: (evt.clientX - CTM.e) / CTM.a,
            y: (evt.clientY - CTM.f) / CTM.d
        };
    }

    function startDrag(evt) {
        // TODO(Luma) only start dragging if the element has class "draggable"
        // Add this class to selected items in the diagram through styles (bus nodes)
        //if (evt.target.classList.contains('draggable')) {
        //    selectedElement = evt.target;
        //}
        selectedElement = evt.target;
        console.log("startDrag " + selectedElement.id);

        offset = getMousePosition(evt);
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

    function drag(evt) {
        if (selectedElement) {
            evt.preventDefault();
            var coord = getMousePosition(evt);
            transform.setTranslate(coord.x - offset.x, coord.y - offset.y);
        }
    }

    function endDrag(evt) {
        if (selectedElement != null) {
            console.log("endDrag " + selectedElement.id);
            var coord = getMousePosition(evt);
            var dx = coord.x - offset.x;
            var dy = coord.y - offset.y;
            console.log("endDrag for element " + selectedElement.id);
            console.log("    offset                " + offset.x + ", " + offset.y);
            console.log("    final translation to  " + dx + ", " + dy);
            console.log("    from transform matrix " + transform.matrix.e + ", " + transform.matrix.f);
            // TODO(Luma) obtain the equipmentId from the metadata and pass it to the jsHandler with the new position for it
            // Or wait until many drag operations have been performed and the user confirms the re-layout with all the updates
            jsHandler.handleEvent("endDragUpdateDiagram", selectedElement.id);
        }
        selectedElement = null;
    }
}
