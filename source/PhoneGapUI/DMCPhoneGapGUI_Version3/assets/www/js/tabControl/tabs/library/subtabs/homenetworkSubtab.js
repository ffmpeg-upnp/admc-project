var homeNetworkSubTab;
var homenetworkContentControler;
var homenetworkContentControler_visible = false;

var currentPadding_homenetwork;
var preScrollPosition_homenetwork;

var btn_back_homenetwork;
var btn_selectAll;
var btn_deselectAll;

function initHomenetworkSubtab() {
	homeNetworkSubTab = $('#div_content_homenetwork');

	homenetworkContentControler = $('#div_content_network_controler');
	homenetworkContentControler.hide();
	
	currentPadding_homenetwork = getPadding_HomeNetworkSubtab(dmrControllerVisible_GlobalFooter,
			homenetworkContentControler_visible);

	btn_back_homenetwork = $('#img_btn_back_homenetwork');
	btn_back_homenetwork.bind('tap', function() {
		onTap_Back_Homenetwork($(this));
	});

	btn_selectAll = $('#img_btn_selectall');
	btn_selectAll.bind('tap', function() {
		onTap_SelectAll($(this));
	});

	btn_deselectAll = $('#img_btn_deselectall');
	btn_deselectAll.bind('tap', function() {
		onTap_DeselectAll($(this));
	});
	
	preScrollPosition_homenetwork = 0;
	homeNetworkSubTab.bind('scrollstop', function (){
		
		var currentScrollPosition = $(document).scrollTop();
		if (currentScrollPosition <= preScrollPosition_homenetwork){
			preScrollPosition_homenetwork = currentScrollPosition;
			return;
		}
		
		preScrollPosition_homenetwork = currentScrollPosition;
		var documentHeight = homeNetworkSubTab.height() + 
			currentPadding_homenetwork + 
			$(window).height() * 0.18;
		
		if (documentHeight > $(window).height()){
			var currentWindowPosition = currentScrollPosition + 
				$(window).height() + 15;// 15px is a tolerance-value
			
			if (currentWindowPosition >= documentHeight){
				onScrollToEndOfPage_HomenetworkContent ();
			}
		}
	});
}

function getPadding_HomeNetworkSubtab(dmrContentVisible,
		networkContentControlerVisible) {
	var paddingValue = getFooterHeight(dmrContentVisible);
	if (networkContentControlerVisible)
		paddingValue += homenetworkContentControler.height();
	currentPadding_homenetwork = paddingValue; //save for check end of page
	return paddingValue;
}

function showContentController_HomeNetworkSubtab() {
	if (currentSubTab == "homenetwork" && currentTab_TabsControl == "library") {
		homenetworkContentControler_visible = true;
		if (dmrControllerVisible_GlobalFooter) {
			var paddingValue = getPadding_HomeNetworkSubtab(true, true);
			homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
		} else {
			var paddingValue = getPadding_HomeNetworkSubtab(false, true);
			homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
		}
		homenetworkContentControler.fadeIn("fast");
	}
}

function hideContentController_HomeNetworkSubtab() {
	homenetworkContentControler_visible = false;
	if (dmrControllerVisible_GlobalFooter) {
		var paddingValue = getFooterHeight(true);
		homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
	} else {
		var paddingValue = getFooterHeight(false);
		homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
	}
	homenetworkContentControler.hide();
}

function toggleNetworkContentControler() {
	if (homenetworkContentControler_visible) {
		hideContentController_HomeNetworkSubtab();
	} else {
		showContentController_HomeNetworkSubtab();
	}
}

function repadding_HomeNetworkSubtab() {
	var paddingValue = getPadding_HomeNetworkSubtab(
			dmrControllerVisible_GlobalFooter,
			homenetworkContentControler_visible);
	homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
}

function animateDown_HomeNetworkSubtab() {
	homenetworkContentControler.animate({
		bottom : '5%'
	}, "fast");
}

function animateUp_HomeNetworkSubtab() {
	homenetworkContentControler.animate({
		bottom : '20%'
	}, "fast");
}

// ----------------------------------EVENT
// FUNCTION------------------------------
function onTap_Back_Homenetwork(sender) {
	window.plugins.LibraryPlugin.back();
}

function onTap_SelectAll(sender) {

}

function onTap_DeselectAll(sender) {

}

function onScrollToEndOfPage_HomenetworkContent (){
	console.log('SCROLLED TO END OF PAGE');
}
