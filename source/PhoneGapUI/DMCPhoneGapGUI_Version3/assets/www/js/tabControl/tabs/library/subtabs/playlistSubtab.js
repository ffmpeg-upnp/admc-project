var playlistSubTab;

var playlistContentControler;
var playlistContentControler_visible = false;

function initPlaylistSubtab (){
	playlistSubTab = $('#div_content_playlist');
	playlistSubTab.hide();
	
	playlistContentControler = $('#div_content_playlist_controler');
	playlistContentControler.hide();
}

function getPadding_PlaylistSubTab (dmrContentVisible, playlistContentControlerVisible){
	var paddingValue = getFooterHeight(dmrContentVisible);
	if (playlistContentControlerVisible)
		paddingValue += playlistContentControler.height();
	return paddingValue;
}

function showContentController_PlaylistSubtab (){
	if (currentSubTab == "playlist" && currentTab_TabsControl == "library"){
		playlistContentControler_visible = true;
		if (dmrControllerVisible_GlobalFooter){
			var paddingValue = getPadding_PlaylistSubTab(true, true);
			playlistSubTab.css('paddingBottom', paddingValue + 'px');
		}else{
			var paddingValue = getPadding_PlaylistSubTab(false, true);
			playlistSubTab.css('paddingBottom', paddingValue + 'px');
		}
		playlistContentControler.fadeIn("fast");
	}
}

function hideContentController_PlaylistSubtab(){
	playlistContentControler_visible = false;
	if (dmrControllerVisible_GlobalFooter){
		var paddingValue = getFooterHeight(true);
		playlistSubTab.css('paddingBottom', paddingValue + 'px');
	}else{
		var paddingValue = getFooterHeight(false);
		playlistSubTab.css('paddingBottom', paddingValue + 'px');
	}
	playlistContentControler.hide();
}

function togglePlaylistContentControler (){
	if (playlistContentControler_visible){
		hideContentController_PlaylistSubtab();
	}else{
		showContentController_PlaylistSubtab();
	}
}

function repadding_PlaylistSubtab (){
	var paddingValue = getPadding_PlaylistSubTab(dmrControllerVisible_GlobalFooter, playlistContentControler_visible);
	playlistSubTab.css('paddingBottom', paddingValue + 'px');
}

function animateDown_PlaylistSubtab (){
	playlistContentControler.animate({bottom: '6%'}, "fast");
}

function animateUp_PlaylistSubtab (){
	playlistContentControler.animate({bottom: '21%'}, "fast");
}