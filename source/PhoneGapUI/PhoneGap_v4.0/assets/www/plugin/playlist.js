//private static final String ACTION_NEXT = "next";
//	private static final String ACTION_PREV = "prev";
//	private static final String ACTION_PLAY = "play";
//	private static final String ACTION_PAUSE = "pause";
//	private static final String ACTION_STOP = "stop";
//	private static final String ACTION_SET_VOLUME = "setVolume";
//	private static final String ACTION_SEEK = "seek";

var playlist_currentState = "STOP";

var PlaylistPlugin = function() {
};

PlaylistPlugin.prototype.loadPlaylist = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'loadPlaylist', [ "" ]);
};

PlaylistPlugin.prototype.itemClick = function(idx) {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'itemClick', [ idx ]);
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("PlaylistPlugin", new PlaylistPlugin());
});

function loadPlaylistItems(e) {
	var result = eval(e);
	for ( var i = 0; i < result.length; i++) {
		var obj = result[i];
		addPlaylistItem(obj);
	}
	listview_mediacontent.listview('refresh');
}

function addPlaylistItem(item) {
	var html = "<li idx='" + item.idx + "'";

	if (item.selected == "true") {
		html += "data-icon='added-to-playlist' ";
	} else {
		html += "data-icon='false' ";
	}

	html += "onclick='onPlaylistItemClick(\"" + item.idx + "\");'>";

	html += "<a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='" + item.icon
			+ "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>" + item.name + "</h3><p>"
			+ (item.childCount != null ? (item.childCount.toString() + " childs") : " ") + "</p></a></li>";
	listview_mediacontent.append(html);
}

function onPlaylistItemClick(e) {
	window.plugins.PlaylistPlugin.itemClick(e);
}

function clearPlaylist() {
	listview_mediacontent.html("");
	listview_mediacontent.listview("refresh");
}

function playlist_onStop() {
	if (playlist_currentState != "STOP") {
		playlist_currentState = "STOP";
		playlist_updateMediaButton();
	}
}

function playlist_onPlaying() {
	if (playlist_currentState != "PLAY") {
		playlist_currentState = "PLAY";
		playlist_updateMediaButton();
	}
}

function playlist_onPause() {
	if (playlist_currentState != "PAUSE") {
		playlist_currentState = "PAUSE";
		playlist_updateMediaButton();
	}

}
