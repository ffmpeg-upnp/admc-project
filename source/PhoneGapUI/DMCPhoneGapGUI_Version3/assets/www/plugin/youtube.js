var YoutubePlugin = function() {
};

YoutubePlugin.prototype.addToPlaylist = function(idx) {
	showLoadingIcon();
	PhoneGap.exec(null, null, 'YoutubePlugin', 'addToPlaylist', [ idx, proxy ]);
};

YoutubePlugin.prototype.query = function(queryString) {
	showLoadingIcon();
	PhoneGap.exec(null, null, 'YoutubePlugin', 'query', [ queryString ]);
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("YoutubePlugin", new YoutubePlugin());
});

var proxy = false;

function showYoutubeResult(e) {
	clearYoutubeList();
	var result = eval(e);
	for ( var i = 0; i < result.length; i++) {
		console.log('add to list');
		var obj = result[i];
		yt_addItemToListView(obj);
	}
	hideLoadingIcon();
	youtube_listview.listview('refresh');
	myScroll_youtube.refresh();
	myScroll_youtube.scrollTo(0, 0, 0);
}

function yt_addItemToListView(item) {
	var html = "<li index='" + item.idx + "' ";

	html += "data-icon='false' ";
	if (item.url != null) {
		html += "url='" + item.url + "' ";
	}
	html += "onclick='yt_onYoutubeItemClick(" + item.idx + ");'>";
	html += "<a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='"
			+ item.thumb
			+ "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>"
			+ item.title + "</h3><p>" + item.duration + "</p></a></li>";
	youtube_listview.append(html);
}

function clearYoutubeList() {
	youtube_listview.html('');
}

function yt_onYoutubeItemClick(idx) {
	console.log(idx.toString());
	// TODO: fix constant here
	window.plugins.YoutubePlugin.addToPlaylist(idx);
}
