var nowplaying_content_perform_minheight;
var nowplaying_content_perform_maxheight;

function init_NowplayingTab() {
	//console.log('init Nowplaying tab');
	$('#div_content_internet_searchbar').hide(); // hide global search bar in internet-tab
	$('#div_content_nowplaying_tab').hide();// hide all nowplaying tab
	
	/*console.log("div_content_nowplaying_tab height = "
			+ $('#div_content_nowplaying_tab').height());
	
	var nowplaying_content_min_height = $('#div_content_nowplaying_tab')
			.height();
	var nowplaying_content_max_height = nowplaying_content_min_height / 70 * 85;
	console.log("nowplaying_content_min_height = "
			+ nowplaying_content_min_height
			+ "; nowplaying_content_max_height = "
			+ nowplaying_content_max_height);

	var nowplaying_content_info_height = nowplaying_content_min_height * 12 / 100;
	var nowplaying_content_controler_height = nowplaying_content_min_height * 38 / 100;

	nowplaying_content_perform_minheight = nowplaying_content_min_height
			- nowplaying_content_info_height
			- nowplaying_content_controler_height;
	nowplaying_content_perform_minheight += 'px';

	nowplaying_content_perform_maxheight = nowplaying_content_max_height
			- nowplaying_content_info_height
			- nowplaying_content_controler_height;
	nowplaying_content_perform_maxheight += 'px';

	init_contentManagement();
	$('#div_content_info_nowplaying').css('height',
			(nowplaying_content_info_height - 1) + 'px');
	$('#div_content_perform_nowplaying').css('top',
			nowplaying_content_info_height + 'px');

	$('#div_content_controler_nowplaying').css('height',
			nowplaying_content_controler_height + 'px');

	$('#div_content_perform_nowplaying').css('height',
			nowplaying_content_perform_minheight);
	
	
	$('#img_btn_media_previous').bind('tap', function (){
		onTap_Media_Previous ($(this));
	});
	
	$('#img_btn_media_pause_play').bind('tap', function (){
		onTap_Media_Pause_Play($(this));
	});
	
	$('#img_btn_media_stop').bind('tap', function (){
		onTap_Media_Stop($(this));
	});
	
	$('#img_btn_media_next').bind('tap', function (){
		onTap_Media_Next($(this));
	});
	
	$('#img_btn_media_changeitem').bind('tap', function (){
		onTap_Media_ChangeItem($(this));
	});
	
	$('#img_btn_media_changeplaylist').bind('tap', function (){
		onTap_Media_ChangePlaylist($(this));
	});
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmousedown', function (){
		onSeeking_DurationBar ($('#slider_seek_bar'));
	});
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmouseup', function (){
		onChange_DurationBar($('#slider_seek_bar'));
	});
	
	$('#slider_volume_bar').siblings('.ui-slider').bind ('vmouseup', function (){
		onChange_VolumeBar($('#slider_volume_bar'));
	});
	*/
}

function show_NowplayingTab() {
	$('.nowplaying_tab').fadeIn("fast");
}

function hide_NowplayingTab() {
	$('.nowplaying_tab').hide();
	$('#div_navbar_1 div').css("backgroundColor", "#33B5E5");
	$('#div_navbar_2 div').css("backgroundColor", "black");
}

//----------------------------EVENT FUNCTION-----------------------------
function onTap_Media_Previous (sender){
	
}

function onTap_Media_Pause_Play (sender){
	
}

function onTap_Media_Stop (sender){
	
}

function onTap_Media_Next (sender){
	
}

function onTap_Media_ChangeItem (sender){
	
}

function onTap_Media_ChangePlaylist (sender){
	
}

function onSeeking_DurationBar (sender){
	sender.attr('data-seeking', 'true');
}

function onChange_DurationBar (sender){
	sender.attr('data-seeking', 'false');
	//Code onChange here..
}

function onChange_VolumeBar (sender){
	
}
