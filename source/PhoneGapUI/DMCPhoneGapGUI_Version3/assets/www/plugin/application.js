var ApplicationPlugin = function() {
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("ApplicationPlugin", new ApplicationPlugin());
});

var homenetwork_browsestate = 0; 
// 0: browse all dms;
// 1: browse content of dms;