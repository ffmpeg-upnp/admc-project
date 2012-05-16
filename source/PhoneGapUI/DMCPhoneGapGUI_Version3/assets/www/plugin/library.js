var LibraryPlugin = function() {
};
LibraryPlugin.prototype.browse = function(objectID) {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'browse', [ objectID ]);
};

LibraryPlugin.prototype.addToPlaylist = function(objectID) {
	PhoneGap.exec(null, null, 'LibraryPlugin', 'addToPlaylist', [ objectID ]);
};

LibraryPlugin.prototype.back = function() {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'back', [ "" ]);
};

LibraryPlugin.prototype.nextPage = function() {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'nextPage', [ "" ]);
};

LibraryPlugin.prototype.previousPage = function() {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'previousPage', [ "" ]);
};

LibraryPlugin.prototype.selectAll = function() {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'selectAll', [ "" ]);
};

LibraryPlugin.prototype.deselectAll = function() {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'deselectAll', [ "" ]);
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("LibraryPlugin", new LibraryPlugin());
});

var active_span = '<span class="ui-icon ui-icon-added-to-playlist ui-icon-shadow"></span>';

function loadBrowseResult(e) {
	if (homenetwork_browsestate != 1)
		return;
	var result = eval(e);
	for ( var i = 0; i < result.length; i++) {
		var obj = result[i];
		addItemToListView(obj);
	}
	listview_homenetwork.listview('refresh');
	showContentController_HomeNetworkSubtab();
}

function clearLibraryList() {
	listview_homenetwork.html('');
	listview_homenetwork.listview('refresh');
}

function addItemToListView(item) {
	var html = "<li itemId='" + item.id + "'";

	if (item.selected == "true") {
		html += "data-icon='added-to-playlist' ";
	} else {
		html += "data-icon='false' ";
	}
	if (item.url != null) {
		html += "url='" + item.url + "' ";
	}

	if (item.childCount != null) {
		html += "onclick='onContainerClick(\"" + item.id + "\");'>";
	} else
		html += "onclick='onItemClick(\"" + item.id + "\");'>";
	html += "<a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='" + item.icon
			+ "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>" + item.name + "</h3><p>"
			+ (item.childCount != null ? (item.childCount.toString() + " childs") : " ") + "</p></a></li>";
	listview_homenetwork.append(html);
}

function onContainerClick(id) {
	window.plugins.LibraryPlugin.browse(id);
}

function onItemClick(id) {
	window.plugins.LibraryPlugin.addToPlaylist(id);
}

function onBackClick() {
	var backButton = $("#btn_back");
	if (backButton.attr("data-enable") == "true")
		window.plugins.LibraryPlugin.back();
}

function notifyBrowseComplete() {
	console.log("Browse complete");
	window.plugins.LibraryPlugin.getCurrentPage();
}

function addItemToPlaylist(url) {
	$('#div_wrapper_libs li').each(function(index) {
		if ($(this).attr('url') != null && $(this).attr('url').toString() == url) {
			if ($(this).find('span:first').length <= 0)
				$(this).find('div:first').append(active_span);
		}
	});
	listview_homenetwork.listview('refresh');
}

function removeItemFromPlaylist(url) {
	$('#div_wrapper_libs li').each(function(index) {
		if ($(this).attr('url') != null && $(this).attr('url').toString() == url) {
			$(this).find('span:first').remove();
		}
	});
	listview_homenetwork.listview('refresh');
}

function upToDMSList() {
	hideContentController_HomeNetworkSubtab();
	homenetwork_browsestate = 0;
	window.plugins.DevicesPlugin.refresh();
}