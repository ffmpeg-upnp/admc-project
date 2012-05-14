var dmrRemoteControler;
var btn_footerControler;

var dmrController;
var dmrControllerVisible_GlobalFooter;
var btn_dmrPlayController;

var dmrList;

function initGlobalFooter (){
	dmrControllerVisible_GlobalFooter = true;
	
	dmrRemoteControler = $('#div_remote_footer_controler');
	btn_footerControler = $('#div_remote_footer_controler img');
	dmrController = $('#div_dmr_controler');
	btn_dmrPlayController = $('#div_dmr_play img');
	
	btn_footerControler.bind('tap', function(){
		onTap_btn_footerControler ($(this));
	});
	btn_dmrPlayController.bind('tap', function(){
		onTap_playButton ($(this));
	});
	
	init_DMRManagement();
}

function getFooterHeight (dmrContentVisible){
	var result =  dmrRemoteControler.height();
	if (dmrContentVisible)
		result += dmrController.height();
	return result;
}

function close_GlobalFooter (){
	dmrControllerVisible_GlobalFooter = false;
	
	repadding_HomeNetworkSubtab ();
	repadding_PlaylistSubtab ();
	repadding_InternetSubtab();
	
	dmrController.animate({height: '0%'}, "fast", function (){
		dmrController.hide();
	});
	dmrRemoteControler.animate({bottom: '-1%'}, "fast");
	animateDown_HomeNetworkSubtab();
	animateDown_PlaylistSubtab();
	$('#div_content_nowplaying_tab').animate({height: '85%'}, "fast");
	$('#div_content_perform_nowplaying').animate(
			{height: nowplaying_content_perform_maxheight}, "fast");
}

function open_GlobalFooter (){
	dmrControllerVisible_GlobalFooter = true;
	
	dmrController.show();
	dmrController.animate({height: '15%'}, "fast");
	animateUp_HomeNetworkSubtab();
	animateUp_PlaylistSubtab();
	$('#div_content_nowplaying_tab').animate({height: '70%'}, "fast");
	$('#div_content_perform_nowplaying').animate(
			{height: nowplaying_content_perform_minheight}, "fast");
	dmrRemoteControler.animate({bottom: '14%'}, "fast", function(){
		
		repadding_HomeNetworkSubtab();
		repadding_PlaylistSubtab();
		repadding_InternetSubtab();
		
	});
}

//----------------------Event Function--------------------------
function onTap_btn_footerControler (sender){
	if (sender.attr('data-my-state') == "true"){
		close_GlobalFooter();
	}else{
		open_GlobalFooter();
	}
	changeStateImage(sender);
}

function onTap_playButton (sender){
	
}
