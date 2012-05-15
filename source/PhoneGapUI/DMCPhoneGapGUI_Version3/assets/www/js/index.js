var preventTouchMove;
preventTouchMove = function(event) {
	event.preventDefault();
};

var settingButton;

$(document).ready(function() {
	initPage();
	initGlobalFooter ();
	initTabs();
	initImagesManagement();
});

function initPage (){
	settingButton = $('#div_global_setting img');
	settingButton.bind('tap', function(){
		ontap_settingButton($(this));
	});
};

function ontap_settingButton (sender){
	//toggleNetworkContentControler();
	//addNewDMRitem("img/ic_device_unknow_player.png", "localhost")
	//addNewContentItem("video", "hahaha", null, "");
	toggle_settingTable_settingField();
}