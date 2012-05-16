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

PlaylistPlugin.prototype.play = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'play', [ "" ]);
};

PlaylistPlugin.prototype.pause = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'pause', [ "" ]);
};

PlaylistPlugin.prototype.next = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'next', [ "" ]);
};

PlaylistPlugin.prototype.prev = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'prev', [ "" ]);
};

PlaylistPlugin.prototype.stop = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'stop', [ "" ]);
};

PlaylistPlugin.prototype.setVolume = function() {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'setVolume', [ "" ]);
};

PlaylistPlugin.prototype.seek = function(seekTo) {
	PhoneGap.exec(null, null, 'PlaylistPlugin', 'seek', [ seekTo ]);
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
	//myScroll_playlist.scrollTo(0, 0, 0);
	hideLoadingIcon();
	playlist_listview.listview('refresh');
	//myScroll_playlist.refresh();
}

function addPlaylistItem(item) {
	var html = "<li idx='" + item.idx + "'";

	if (item.selected == "true") {
		html += "data-icon='added-to-playlist' ";
	} else {
		html += "data-icon='false' ";
	}

	html += "onclick='onPlaylistItemClick(\"" + item.idx + "\");'>";

	html += "<a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='"
			+ item.icon
			+ "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>"
			+ item.name
			+ "</h3><p>"
			+ (item.childCount != null ? (item.childCount.toString() + " childs")
					: " ") + "</p></a></li>";
	playlist_listview.append(html);
}

function onPlaylistItemClick(e) {
	window.plugins.PlaylistPlugin.itemClick(e);
}

function clearPlaylist() {
	playlist_listview.html("");
	playlist_listview.listview("refresh");
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

function playlist_onEndtrack() {

	window.plugins.PlaylistPlugin.next();
}

function changePlayButtonState() {
	var playButton = $('#img_media_control_play');
	var stateAImgPath = playButton.attr('data-state-a');
	var stateBImgPath = playButton.attr('data-state-b');
	if (playlist_currentState == "PLAY") {
		$(sender).attr('data-current-path', stateBImgPath);
	} else {
		$(sender).attr('data-current-path', stateAImgPath);
	}
	changeImagePathWithTimeOut(playButton,
			playButton.attr('data-current-path'), time_to_swap_image);
	if (playlist_currentState == "PLAY") {
		playButton.attr("data-my-state", "true");
	} else {
		playButton.attr("data-my-state", "false");
	}
}

function playlist_updateMediaButton() {
	var playButton = $('#img_media_control_play');

	var stateAImgPath = $(playButton).attr('data-state-a');
	var stateBImgPath = $(playButton).attr('data-state-b');
	if (playlist_currentState == "PLAY") {
		$(playButton).attr('data-current-path', stateBImgPath);
		$(playButton).attr('data-my-state', 'true');
	} else {
		$(playButton).attr('data-current-path', stateAImgPath);
		$(playButton).attr('data-my-state', 'false');
	}
	changeImagePathWithTimeOut(playButton, $(playButton).attr(
			'data-current-path'), time_to_swap_image);
}

function playlist_updateDurationSeekbar(current, max) {
	setValueForSeekBar($('#div_field_seekbar input'), current, max);
}

function playlist_updateDMRName(name) {
	$('#div_play_dmr_info').html(name);
}

function playlist_updateDurationString(timeString) {
	$('#div_play_duration_time').html(timeString);
}
