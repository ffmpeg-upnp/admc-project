var settingTable_settingField;
var currentState_settingField;
var toggling_settingField;

var btn_refresh;
var btn_settings;
var btn_about;

function init_SettingField (){
	settingTable_settingField = $('#div_global_setting_table');
	currentState_settingField = "hide";
	toggling_settingField = false;
	settingTable_settingField.hide();
	
	btn_refresh = $('#img_btn_refresh');
	btn_refresh.bind('tap', function(){
		onTap_Refresh ($(this));
	});
	
	btn_settings = $('#img_btn_settings');
	btn_settings.bind('tap', function(){
		onTap_Settings($(this));
	});
	
	btn_about = $('#img_btn_about');
	btn_about.bind('tap', function(){
		onTap_About($(this));
	});
}

function toggle_settingTable_settingField (){
	if (toggling_settingField)
		return;
	toggling_settingField = true;
	switch (currentState_settingField) {
	case "hide":
		settingTable_settingField.show();
		settingTable_settingField.animate({height: "24%", width: "35%"}, "fast",
				function (){
			currentState_settingField = "show";
			toggling_settingField = false;
		});
		break;

	case "show":
		settingTable_settingField.animate({height: "0%", width: "0%"}, "fast",
				function (){
			currentState_settingField = "hide";
			toggling_settingField = false;
			settingTable_settingField.hide();
		});
		break;
	}
}

//-------------------------EVENT FUCTION--------------------
function onTap_Refresh (sender){
	
}

function onTap_Settings (sender){

}

function onTap_About (sender){
	
}