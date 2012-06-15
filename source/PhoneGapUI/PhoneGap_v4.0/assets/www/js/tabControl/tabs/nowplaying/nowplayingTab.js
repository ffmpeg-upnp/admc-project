function init_NowplayingTab() {
	$('.nowplaying_tab').hide();

	init_contentManagement();

	$('#img_btn_media_previous').bind('tap', function() {
		onTap_Media_Previous($(this));
	});

	$('#img_btn_media_pause_play').bind('tap', function() {
		onTap_Media_Pause_Play($(this));
	});

	$('#img_btn_media_next').bind('tap', function() {
		onTap_Media_Next($(this));
	});
	
	/*$('#slider_seek_bar').siblings('.ui-slider').bind ('vmousedown', function (){
		onSeeking_DurationBar ($('#slider_seek_bar'));
	});
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmouseup', function (){
		onChange_DurationBar($('#slider_seek_bar'));
	});*/
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmousedown', function (){
		onChange_DurationBar($('#slider_seek_bar'));
	});
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmousemove', function(){
		return false;
	});
	
	
	/*$('#slider_volume_bar').siblings('.ui-slider').bind ('vmouseup', function (){
		onChange_VolumeBar($('#slider_volume_bar'));
	});*/
	
	$('#slider_volume_bar').siblings('.ui-slider').bind ('vmousedown', function (){
		onChange_VolumeBar($('#slider_volume_bar'));
	});
	
	$('#slider_volume_bar').siblings('.ui-slider').bind ('vmousemove', function(){
		return false;
	});
}

function show_NowplayingTab() {
	//$('.nowplaying_tab').fadeIn("fast");
	$('.nowplaying_tab').show();
}

function hide_NowplayingTab() {
	$('.nowplaying_tab').hide();
	$('#div_navbar_1 div').css("backgroundColor", "#33B5E5");
	$('#div_navbar_2 div').css("backgroundColor", "black");
}

function animateDown_NowplayingTab() {
	$('#div_content_controler_nowplaying').animate({
		top : '67%'
	}, "fast");
	repadding_content_nowplaying();
}

function animateUp_NowplayingTab() {
	$('#div_content_controler_nowplaying').animate({
		top : '52%'
	}, "fast");
	repadding_content_nowplaying();
}

// ----------------------------EVENT FUNCTION-----------------------------
function onTap_Media_Previous(sender) {
	window.plugins.PlaylistPlugin.prev();
}

function onTap_Media_Pause_Play(sender) {
	if (playlist_currentState == "PLAY")
		window.plugins.PlaylistPlugin.pause();
	else
		window.plugins.PlaylistPlugin.play();
}

function onTap_Media_Next(sender) {
	window.plugins.PlaylistPlugin.next();
}

/*function onSeeking_DurationBar(sender) {
	sender.attr('data-seeking', 'true');
}*/

function onChange_DurationBar(sender) {
	sender.attr('data-seeking', 'false');
	// Code onChange here..
	window.plugins.PlaylistPlugin.seek($(sender).attr('value'));
	//console.log("Current value: " + sender.attr('value'));
}

function onChange_VolumeBar(sender) {
	console.log('VOLUME: Onchange');
}

// --------------------------------------SLIDER
// FUNCTION----------------------------------
// Parameter note: sender is seekbar (slider) that you want to set it values
// This method for init-time(if needed) or to update current value of slider bar
// (seekBar or volumeBar)
// By default (in html file):
// duration-seekbar: currentValue = 0, maxValue = 300
// volume-seekbar: currentValue = 25, maxValue = 50
function setValueForSeekBar(sender, currentValue, maxValue) {
	if (sender.attr('data-seeking') == 'false') {
		$(sender).attr('max', maxValue);
		$(sender).attr('value', currentValue);
		$(sender).slider("refresh");
	}
}
