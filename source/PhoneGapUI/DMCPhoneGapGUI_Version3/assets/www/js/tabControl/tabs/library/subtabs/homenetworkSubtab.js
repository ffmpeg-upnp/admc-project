var homeNetworkSubTab;
var homenetworkContentControler;
var homenetworkContentControler_visible = false;

function initHomenetworkSubtab (){
	homeNetworkSubTab = $('#div_content_homenetwork');
	
	homenetworkContentControler = $('#div_content_network_controler');
	homenetworkContentControler.hide();
}

function getPadding_HomeNetworkSubtab (dmrContentVisible, networkContentControlerVisible){
	var paddingValue = getFooterHeight(dmrContentVisible);
	if (networkContentControlerVisible)
		paddingValue += homenetworkContentControler.height();
	return paddingValue;
}

function showContentController_HomeNetworkSubtab (){
	if (currentSubTab == "homenetwork" && currentTab_TabsControl == "library"){
		homenetworkContentControler_visible = true;
		if (dmrControllerVisible_GlobalFooter){
			var paddingValue = getPadding_HomeNetworkSubtab(true, true);
			homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
		}else{
			var paddingValue = getPadding_HomeNetworkSubtab(false, true);
			homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
		}
		homenetworkContentControler.fadeIn("fast");
	}
}

function hideContentController_HomeNetworkSubtab (){
	homenetworkContentControler_visible = false;
	if (dmrControllerVisible_GlobalFooter){
		var paddingValue = getFooterHeight(true);
		homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
	}else{
		var paddingValue = getFooterHeight(false);
		homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
	}
	homenetworkContentControler.hide();
}

function toggleNetworkContentControler (){
	if (homenetworkContentControler_visible){
		hideContentController_HomeNetworkSubtab();
	}else{
		showContentController_HomeNetworkSubtab();
	}
}

function repadding_HomeNetworkSubtab (){
	var paddingValue = getPadding_HomeNetworkSubtab(dmrControllerVisible_GlobalFooter, homenetworkContentControler_visible);
	homeNetworkSubTab.css('paddingBottom', paddingValue + 'px');
}

function animateDown_HomeNetworkSubtab (){
	homenetworkContentControler.animate({bottom: '6%'}, "fast");
}

function animateUp_HomeNetworkSubtab (){
	homenetworkContentControler.animate({bottom: '21%'}, "fast");
}
