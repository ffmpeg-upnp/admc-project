var internetSubTab;

var btn_search_youtube;

function init_InternetSubtab (){
	internetSubTab = $('#div_content_internet');
	internetSubTab.hide();
	
	btn_search_youtube = $('#img_btn_search_youtube');
	btn_search_youtube.bind('tap', function(){
		onTap_Search_Youtube($(this));
	});
}

function repadding_InternetSubtab (){
	var paddingValue = getFooterHeight(dmrControllerVisible_GlobalFooter);
	internetSubTab.css('paddingBottom', paddingValue + 'px');
}

//-----------------------------EVENT FUNCTION----------------------
function onTap_Search_Youtube (sender){
	
}