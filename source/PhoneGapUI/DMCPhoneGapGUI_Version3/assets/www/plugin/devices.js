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
	var device = eval(element);
	var html = "<li data-icon='false' type='"
			+ device.type
			+ "' udn='"
			+ device.udn
			+ "'  onclick='onDeviceClick(this);'><a href='#' style='padding-top: 0px;padding-bottom: 0px' data-icon='delete'><img src='"
			+ device.icon
			+ "' style='height: 100%; width: height; padding-left: 4%; float: left;'/><h3>"
			+ device.name + "</h3><p>" + device.address + "</p></a></li>";
	if (type == 'dms') {
		listview_homenetwork.append(html);
		listview_homenetwork.listview('refresh');
		// myScroll_devices_dmS.refresh();
	}
	// } else {
	// dmr_listview.append(html);
	// dmr_listview.listview('refresh');
	// // myScroll_devices_dmR.refresh();
	// }
};

var clearDevicesList = function() {
	listview_homenetwork.html('');
	listview_homenetwork.listview('refresh');
	showContentController_HomeNetworkSubtab();
};

var remove_device = function(udn) {
	$("#content_devices li[udn='" + udn + "']").remove();
};

var onDeviceClick = function(e) {
	var type = e.getAttribute('type');
	var udn = e.getAttribute('udn');
	if (type == 'dms') {
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

function setCurrentDMS(udn) {
	console.log('Set current dms ' + udn.toString());
	if (currentDMS_udn != udn) {
		currentDMS_udn = udn;
		setSelectedDevice();
		rescanDMS();
	}
}

function rescanDMS() {
	window.plugins.LibraryPlugin.browse("0");
}

function setCurrentDMR(udn) {
	console.log('Set current dmr ' + udn.toString());
	currentDMR_udn = udn;
	setSelectedDevice();
}

function setSelectedDevice() {
	$('#content_devices li')
			.each(
					function(index) {
						if ((currentDMS_udn != null
								&& $(this).attr('udn') != null && $(this).attr(
								'udn').toString() == currentDMS_udn.toString())
								|| (currentDMR_udn != null
										&& $(this).attr('udn') && $(this).attr(
										'udn').toString() == currentDMR_udn
										.toString())) {
							$(this).addClass("ui-btn-active");
						} else {
							$(this).removeClass("ui-btn-active");
						}
					});
	dmr_listview.listview('refresh');
}
