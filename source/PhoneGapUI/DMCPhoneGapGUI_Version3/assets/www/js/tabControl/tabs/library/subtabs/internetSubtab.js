var internetSubTab;

function init_InternetSubtab (){
	internetSubTab = $('#div_content_internet');
	internetSubTab.hide();
}

function repadding_InternetSubtab (){
	var paddingValue = getFooterHeight(dmrControllerVisible_GlobalFooter);
	internetSubTab.css('paddingBottom', paddingValue + 'px');
}