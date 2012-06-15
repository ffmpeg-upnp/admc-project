var currentSubTab;

function init_LibraryTab (){
	currentSubTab = "homenetwork";
	
	initHomenetworkSubtab();
	//initPlaylistSubtab();
	//init_InternetSubtab();
	setPaddingBottomCSS(getFooterHeight(true) + 'px');
	
	/*$('.library_tab_info img').bind('tap', function(){
		switchingSubTabInLibraryTab($(this));
	});*/
}

//------------------PRIVATE FUNCTION---------------------------------
function setPaddingBottomCSS (value){
	$('.content_library').css('paddingBottom', value);
}

/*function switchingSubTabInLibraryTab (sender){
	var dataHref = sender.attr('data-href');
	if (currentSubTab != dataHref){
		switch (dataHref) {
		case "homenetwork":
			showHomeNetworkSubtab(sender);
			break;
			
		//case "playlist":
		//	showPlaylistSubtab(sender);
		//	break;
			
		case "internet":
			showInternetSubtab(sender);
			break;
		}
		currentSubTab = dataHref;
	}
}*/

/*function showHomeNetworkSubtab (sender){
	//playlistSubTab.hide();
	internetSubTab.hide();
	$('#div_content_internet_searchbar').hide();
	//if (playlistContentControler_visible){
	//	playlistContentControler.hide();
	//}
	homeNetworkSubTab.fadeIn("fast", function(){
		if (homenetworkContentControler_visible){
			showContentController_HomeNetworkSubtab();
		}
	});
	
	sender.attr('src', 'img/ic_homenetwork_hl.png');
	//$(".library_tab_info img[data-href='playlist']").attr('src', 'img/ic_playlist.png');
	$(".library_tab_info img[data-href='internet']").attr('src', 'img/ic_internet.png');
}*/

/*function showPlaylistSubtab (sender){
	homeNetworkSubTab.hide();
	internetSubTab.hide();
	$('#div_content_internet_searchbar').hide();
	if (homenetworkContentControler_visible){
		homenetworkContentControler.hide();
	}
	playlistSubTab.fadeIn("fast", function(){
		if (playlistContentControler_visible){
			showContentController_PlaylistSubtab();
		}
	});
	
	sender.attr('src', 'img/ic_playlist_hl.png');
	$(".library_tab_info img[data-href='homenetwork']").attr('src', 'img/ic_homenetwork.png');
	$(".library_tab_info img[data-href='internet']").attr('src', 'img/ic_internet.png');
}*/

/*function showInternetSubtab (sender){
	homeNetworkSubTab.hide();
	//playlistSubTab.hide();
	if (homenetworkContentControler_visible){
		homenetworkContentControler.hide();
	}
	if (playlistContentControler_visible){
		playlistContentControler.hide();
	}
	$('#div_content_internet_searchbar').show();
	internetSubTab.fadeIn("fast", function(){
	});
	
	sender.attr('src', 'img/ic_internet_hl.png');
	$(".library_tab_info img[data-href='homenetwork']").attr('src', 'img/ic_homenetwork.png');
	//$(".library_tab_info img[data-href='playlist']").attr('src', 'img/ic_playlist.png');
}*/

//---------------------------------PUBLIC FUNCTION--------------------------
function show_LibraryTab (){
	/*
	$('#div_global_library_toolbar').fadeIn("fast");
	
	switch (currentSubTab) {
	case "homenetwork":
		showHomeNetworkSubtab($(".library_tab_info img[data-href='homenetwork']"));
		break;
		
	case "playlist":
		showPlaylistSubtab($(".library_tab_info img[data-href='playlist']"));
		break;
		
	case "internet":
		showInternetSubtab($(".library_tab_info img[data-href='internet']"));
		break;
	}
	*/
	homeNetworkSubTab.fadeIn("fast", function(){
		if (homenetworkContentControler_visible){
			showContentController_HomeNetworkSubtab();
		}
	});
}

function hide_LibraryTab (){
	$('.library_tab').hide();
	$('#div_navbar_1 div').css("backgroundColor", "black");
	$('#div_navbar_2 div').css("backgroundColor", "#33B5E5");
}