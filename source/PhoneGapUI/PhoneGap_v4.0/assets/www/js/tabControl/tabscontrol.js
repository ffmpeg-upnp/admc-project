var currentTab_TabsControl;

function initTabs (){
	currentTab_TabsControl = "library";

	init_LibraryTab();
	init_NowplayingTab();
	init_SettingField();
	
	$('#div_navbar_1').bind('tap', function (){
		switchingLibrarytab ();
	});
	$('#div_navbar_2').bind('tap', function (){
		switchingNowplayingTab ();
	});
}

function switchingLibrarytab (){
	//addNewDMRitem("img/ic_device_unknow_player.png", "hihi", "hihi");
	if (currentTab_TabsControl != "library"){
		hide_NowplayingTab();
		show_LibraryTab();
		currentTab_TabsControl = "library";
	}
}

function switchingNowplayingTab (){
	if (currentTab_TabsControl != "nowplaying"){
		hide_LibraryTab();
		show_NowplayingTab();
		currentTab_TabsControl = "nowplaying";
	}
}