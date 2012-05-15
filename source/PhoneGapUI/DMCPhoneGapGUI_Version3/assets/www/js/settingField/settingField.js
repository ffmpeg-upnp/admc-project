
var settingTable_settingField;
var currentState_settingField;
var toggling_settingField;

function init_SettingField (){
	settingTable_settingField = $('#div_global_setting_table');
	currentState_settingField = "hide";
	toggling_settingField = false;
	settingTable_settingField.hide();
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