var LibraryPlugin = function() {
};
LibraryPlugin.prototype.browse = function(objectID) {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'browse', [ objectID ]);
};

LibraryPlugin.prototype.addToPlaylist = function(idx) {
	PhoneGap.exec(null, null, 'LibraryPlugin', 'addToPlaylist', [ idx ]);
};

LibraryPlugin.prototype.back = function() {
	// showLoadingIcon();
	PhoneGap.exec(null, null, 'LibraryPlugin', 'back', [ "" ]);
};

LibraryPlugin.prototype.loadMore = function() {
	PhoneGap.exec(null, null, 'LibraryPlugin', 'loadMore', [ "" ]);
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("LibraryPlugin", new LibraryPlugin());
});
var counter = -1;
function loadBrowseResult(e) {
	counter++;
	window.plugins.ApplicationPlugin.showLoadStart();
	if (homenetwork_browsestate != 1)
		return;
	var result = eval(e);
	for ( var i = 0; i < result.length; i++) {
		var obj = result[i];
		addItemToListView(obj, counter * 15 + i);
	}
	listview_homenetwork.listview('refresh');
	window.plugins.ApplicationPlugin.showLoadComplete();
}

function clearLibraryList() {
	listview_homenetwork.html('');
	listview_homenetwork.listview('refresh');
}

function addItemToListView(item, idx) {
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
		html += "onclick='onItemClick(\"" + idx + "\");'>";
	html += "<a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='" + item.icon
			+ "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>" + item.name + "</h3><p>"
			+ (item.childCount != null ? (item.childCount.toString() + " childs") : " ") + "</p></a></li>";
	listview_homenetwork.append(html);
}

function onContainerClick(id) {
	counter = -1;
	window.plugins.LibraryPlugin.browse(id);
}

function onItemClick(idx) {
	window.plugins.LibraryPlugin.addToPlaylist(idx);
}

function notifyBrowseComplete() {
	console.log("Browse complete");
	window.plugins.LibraryPlugin.getCurrentPage();
}

function upToDMSList() {
	hideContentController_HomeNetworkSubtab();
	homenetwork_browsestate = 0;
	window.plugins.DevicesPlugin.refresh();
}