var ApplicationPlugin = function() {
};

PhoneGap.addConstructor(function() {
	PhoneGap.addPlugin("ApplicationPlugin", new ApplicationPlugin());
});