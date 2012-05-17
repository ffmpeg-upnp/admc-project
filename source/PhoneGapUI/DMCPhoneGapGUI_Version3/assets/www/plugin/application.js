var ApplicationPlugin = function() {
};

ApplicationPlugin.prototype.showLoadStart = function() {
	PhoneGap.exec(null, null, 'ApplicationPlugin', 'showLoadStart', [ "" ]);
};

ApplicationPlugin.prototype.showLoadComplete = function() {
	PhoneGap.exec(null, null, 'ApplicationPlugin', 'showLoadComplete', [ "" ]);
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("ApplicationPlugin", new ApplicationPlugin());
});

var homenetwork_browsestate = 0;
// 0: browse all dms;
// 1: browse content of dms;
