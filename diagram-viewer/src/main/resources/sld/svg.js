document.addEventListener('click', function (event) {
	if (event.target.closest('.sld-top-feeder')    ||
	    event.target.closest('.sld-bottom-feeder')) {
	    jsHandler.handleSelectionChange(event.target.parentElement.id);
	}
	if (event.target.closest('.sld-breaker')       ||
        event.target.closest('.sld-disconnector')) {
            jsHandler.handleSwitchPositionChange(event.target.parentElement.id);
    }
}, false);
