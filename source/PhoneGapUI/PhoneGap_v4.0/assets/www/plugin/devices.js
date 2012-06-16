var DevicesPlugin = function() {
};

DevicesPlugin.prototype.setDMS = function(udn) {
	PhoneGap.exec(null, null, 'DevicesPlugin', 'setDMS', [ udn ]);
};

DevicesPlugin.prototype.setDMR = function(udn) {
	PhoneGap.exec(null, null, 'DevicesPlugin', 'setDMR', [ udn ]);
};

DevicesPlugin.prototype.refresh = function() {
	PhoneGap.exec(null, null, 'DevicesPlugin', 'refreshDMS', [ "" ]);
};
PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("DevicesPlugin", new DevicesPlugin());
});
var currentDMS_udn = "";
var currentDMR_udn = "";

var add_device = function(element, type) {
	if (homenetwork_browsestate != 0) {
		return;
	}
	var device = eval(element);
	var html = "<li data-icon='false' type='" + device.type + "' udn='" + device.udn
			+ "'  onclick='onDeviceClick(this);'><a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='"
			+ device.icon + "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>" + device.name + "</h3><p>"
			+ device.address + "</p></a></li>";
	if (type == 'dms') {
		listview_homenetwork.append(html);
		listview_homenetwork.listview('refresh');
		// myScroll_devices_dmS.refresh();
	} else if (type == 'dmr') {
		addNewDMRitem(device.icon, device.udn, device.name);
	}
	// } else {
	// dmr_listview.append(html);
	// dmr_listview.listview('refresh');
	// // myScroll_devices_dmR.refresh();
	// }
};

var clearDMSList = function() {
	listview_homenetwork.html('');
	listview_homenetwork.listview('refresh');
};

var clearDMRList = function() {
	dmr_slider.html('');
	window.plugins.ApplicationPlugin.showLoadComplete();
};

var remove_dms = function(udn) {
	$("#listview_homenetwork li[udn='" + udn + "']").remove();
};

var remove_dmr = function(udn) {
	removeDMRitem(udn);
};

var onDeviceClick = function(e) {
	var type = e.getAttribute('type');
	var udn = e.getAttribute('udn');
	if (type == 'dms') {
		showContentController_HomeNetworkSubtab();
		choseDMS(udn);
		homenetwork_browsestate = 1;// browse content of dms
	} else if (type == 'dmr') {
		choseDMR(udn);
	}
};

function choseDMS(udn) {
	window.plugins.DevicesPlugin.setDMS(udn);
}

function choseDMR(udn) {
	window.plugins.DevicesPlugin.setDMR(udn);
}

function rescanDMS() {
	window.plugins.LibraryPlugin.browse("0");
}

function setCurrentDMR(udn) {

}
