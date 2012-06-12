var preventTouchMove;
preventTouchMove = function(event) {
	event.preventDefault();
};

var listview_homenetwork;
var listview_mediacontent;

/*var settingButton*/;

$(document).ready(function() {
	initPage();
	initGlobalFooter();
	initTabs();
	initImagesManagement();
	listview_homenetwork = $('#listview_homenetwork');
	listview_mediacontent = $('#listview_mediacontent');
	
	console.log('Window width: ' + $(window).width() + 'px');
	console.log('Window height: ' + $(window).height() + 'px');
});

function initPage() {
	/*settingButton = $('#div_global_setting img');
	settingButton.bind('tap', function() {
		ontap_settingButton($(this));
	});*/
};

/*function ontap_settingButton(sender) {
	//toggleNetworkContentControler();
	//addNewDMRitem("img/ic_device_unknow_player.png", "a", "Vice car lone device");
	//addNewContentItem("video", "hahaha", null, "");
	//toggle_settingTable_settingField();
	//removeDMRitem("LocalPlayer");
	addNewContentItem("video", "My Video.mp4", null, "abcd");
}*/