
function init_NowplayingTab() {
	$('.nowplaying_tab').hide();
	
	init_contentManagement();
	
	
	$('#img_btn_media_previous').bind('tap', function (){
		onTap_Media_Previous ($(this));
	});
	
	$('#img_btn_media_pause_play').bind('tap', function (){
		onTap_Media_Pause_Play($(this));
	});
	
	/*$('#img_btn_media_stop').bind('tap', function (){
		onTap_Media_Stop($(this));
	});*/
	
	$('#img_btn_media_next').bind('tap', function (){
		onTap_Media_Next($(this));
	});
	
	/*$('#img_btn_media_changeitem').bind('tap', function (){
		onTap_Media_ChangeItem($(this));
	});
	
	$('#img_btn_media_changeplaylist').bind('tap', function (){
		onTap_Media_ChangePlaylist($(this));
	});*/
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmousedown', function (){
		onSeeking_DurationBar ($('#slider_seek_bar'));
	});
	
	$('#slider_seek_bar').siblings('.ui-slider').bind ('vmouseup', function (){
		onChange_DurationBar($('#slider_seek_bar'));
	});
	
	$('#slider_volume_bar').siblings('.ui-slider').bind ('vmouseup', function (){
		onChange_VolumeBar($('#slider_volume_bar'));
	});
}

function show_NowplayingTab() {
	$('.nowplaying_tab').fadeIn("fast");
}

function hide_NowplayingTab() {
	$('.nowplaying_tab').hide();
	$('#div_navbar_1 div').css("backgroundColor", "#33B5E5");
	$('#div_navbar_2 div').css("backgroundColor", "black");
}

function animateDown_NowplayingTab(){
	//$('#div_content_perform_nowplaying').animate({height: '47%'}, "fast");
	$('#div_content_controler_nowplaying').animate({top: '67%'}, "fast");
	//animateDown_contentManagement();
	repadding_content_nowplaying ();
}

function animateUp_NowplayingTab(){
	//$('#div_content_perform_nowplaying').animate({height: '32%'}, "fast");
	$('#div_content_controler_nowplaying').animate({top: '52%'}, "fast");
	//animateUp_contentManagement();
	repadding_content_nowplaying ();
}

//----------------------------EVENT FUNCTION-----------------------------
function onTap_Media_Previous (sender){
	alert('previous');
}

function onTap_Media_Pause_Play (sender){
	alert('play');
}

/*function onTap_Media_Stop (sender){
	alert('stop');
}*/

function onTap_Media_Next (sender){
	alert('next');
}

/*function onTap_Media_ChangeItem (sender){
	alert('change Item');
}

function onTap_Media_ChangePlaylist (sender){
	alert('change playlist');
}*/

function onSeeking_DurationBar (sender){
	sender.attr('data-seeking', 'true');
}

function onChange_DurationBar (sender){
	sender.attr('data-seeking', 'false');
	//Code onChange here..
	console.log('DURATION: Onchange');
}

function onChange_VolumeBar (sender){
	console.log('VOLUME: Onchange');
}

//--------------------------------------SLIDER FUNCTION----------------------------------
//Parameter note: sender is seekbar (slider) that you want to set it values
//This method for init-time(if needed) or to update current value of slider bar (seekBar or volumeBar)
//By default (in html file):
//duration-seekbar: currentValue = 0, maxValue = 300
//volume-seekbar: currentValue = 25, maxValue = 50
function setValueForSeekBar(sender, currentValue, maxValue) {
	if (sender.attr('data-seeking') == 'false') {
		$(sender).attr('max', maxValue);
		$(sender).attr('value', currentValue);
		$(sender).slider("refresh");
	}
}


