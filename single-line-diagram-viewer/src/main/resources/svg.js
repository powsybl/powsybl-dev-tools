document.addEventListener('click', function (event) {
	if (event.target.closest('.sld-top-feeder')    ||
	    event.target.closest('.sld-bottom-feeder')) {
	    jsHandler.select(event.target.id);
	}
	if (event.target.closest('.sld-breaker')       ||
        event.target.closest('.sld-disconnector')) {
            jsHandler.handleSwitchPositionchange(event.target.parentElement.id);
    }
}, false);
