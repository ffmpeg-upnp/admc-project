var dmr_handler;
var dmr_slider;
var dmr_mouseDown;
var dmr_mouseX;
var dmr_lenght;
var dmr_currentIndex;
var dmr_xOffset;
var dmr_handlerWidth;
var dmr_tolerance;
var dmr_delayTime;
var dmr_infotextHeight;

var dmr_selectedItem;
var btn_dmrPlayController;

var dmr_taphold;

function init_DMRManagement (){
	dmr_taphold = false;
	dmr_mouseDown = false;
	dmr_mouseX = 0;
	dmr_lenght = 1;
	dmr_currentIndex = 0;
	dmr_xOffset = 0;
	dmr_handlerWidth = 0;
	dmr_tolerance = 0.25;
	dmr_delayTime = 300;
	
	dmr_slider = $('#div_dmr_list_container');
	dmr_handler = $('#div_dmr_handler');
	dmr_handlerWidth = dmr_handler.width();
	dmr_infotextHeight = dmr_handler.height() * 0.15;
	
	resetCSS_dmrItems();
	console.log(dmr_handlerWidth + 'px');
	
	selectDMRitem($('#div_dmr_list_container div.div_dmr_item:eq(0)'));
	
	dmr_handler.bind('vmousedown', function(event){
		onMouseDown_dmrHandler(event);
	});
	
	dmr_handler.bind('vmousemove', function(event){
		onMouseMove_dmrHandler(event);
	});
	
	dmr_handler.bind('vmouseup', function(event){
		onMouseUp_dmrHandler(event);
	});
	
	$('.img_dmr_item').live('tap', function(){
		if (!dmr_taphold)
			onTap_dmrItem($(this));
		else
			dmr_taphold = false;
	});
	
	$('.img_dmr_item').live('taphold', function(){
		dmr_taphold = true;
		onTapHold_dmrItem($(this));
	});
	
	btn_dmrPlayController = $('#div_dmr_play img');
	btn_dmrPlayController.bind('tap', function(){
		onTap_playButton ($(this));
	});
}

function resetCSS_dmrItems (){
	var listOfDMRitem = $('.div_dmr_item');
	listOfDMRitem.css("width", dmr_handlerWidth + 'px');
	$('.div_dmr_info').css('fontSize', dmr_infotextHeight + 'px');
}

function selectDMRitem (item){
	if (dmr_selectedItem != null){
		dmr_selectedItem.attr("data-selected", "false");
		dmr_selectedItem.find("img").css("-webkit-border-radius", "0px");
		dmr_selectedItem.find("img").css("border", "0px");
		/*dmr_selectedItem.find("img").css("background", "none");*/
	}
	dmr_selectedItem = item;
	dmr_selectedItem.attr("data-selected", "true");
	dmr_selectedItem.find("img").css("-webkit-border-radius", "5px");
	dmr_selectedItem.find("img").css("border", "solid 2px #33B5E5");
	/*dmr_selectedItem.find("img").css("background", "rgba(51, 181, 229, 0.5)");*/
}

function onMouseDown_dmrHandler (event){
	if (!dmr_mouseDown){
		dmr_mouseDown = true;
		dmr_mouseX = event.pageX;
	}
}

function onMouseMove_dmrHandler (event){
	if (dmr_mouseDown){
		dmr_xOffset = event.pageX - dmr_mouseX;
		if (dmr_xOffset < 0){
			/*if (dmr_currentIndex < dmr_lenght - 1)*/
				dmr_slider.css('left', - dmr_currentIndex * dmr_handlerWidth + dmr_xOffset);
		}else{
			/*if (dmr_currentIndex > 0)*/
				dmr_slider.css('left', - dmr_currentIndex * dmr_handlerWidth + dmr_xOffset);
		}
	}
}

function onMouseUp_dmrHandler (event){
	dmr_mouseDown = false;
	if (dmr_xOffset == 0){
		return false;
	}
	
	var fullWidth = dmr_handlerWidth;
	var haflWidth = fullWidth / 2;
	
	if (-dmr_xOffset > haflWidth - fullWidth * dmr_tolerance){
		dmr_currentIndex++;
		if (dmr_currentIndex >= dmr_lenght)
			dmr_currentIndex = dmr_lenght - 1;
		dmr_slider.animate({left: -dmr_currentIndex * dmr_handlerWidth}, dmr_delayTime);
	}else if (dmr_xOffset > haflWidth - fullWidth * dmr_tolerance){
		dmr_currentIndex--;
		if (dmr_currentIndex < 0)
			dmr_currentIndex = 0;
		dmr_slider.animate({left: -dmr_currentIndex * dmr_handlerWidth}, dmr_delayTime);
	}else{
		dmr_slider.animate({left: -dmr_currentIndex * dmr_handlerWidth}, dmr_delayTime);
	}
	dmr_xOffset = 0;
	return true;
}
//-----------------------------PUBLIC FUNCTION--------------
function addNewDMRitem (imageUrl, deviceUrl, deviceName){
	if (imageUrl == null){
		imageUrl = "img/ic_device_unknow_player.png";
	}
	var containerWidth = dmr_slider.width();
	containerWidth += dmr_handlerWidth;
	dmr_slider.css('width', containerWidth + 'px');
	dmr_slider.append('<div class="div_dmr_item" data-selected="false" align="center" data-url="' + deviceUrl + '">' + 
			'<img class ="img_dmr_item" src="' + imageUrl + '">' +
			'<div class="div_dmr_info">' +
				deviceName + 
			'</div>' +
			'</div>');
	resetCSS_dmrItems();
	dmr_lenght++;
}

function removeDMRitem (deviceUrl){
	dmr_lenght--;
}

//------------------------------EVENT FUNCTION---------------------------
function onTap_dmrItem (sender){
	console.log('DMR: tap');
	selectDMRitem($('#div_dmr_list_container div.div_dmr_item:eq(' + dmr_currentIndex + ')'));
}

function onTapHold_dmrItem (sender){
	console.log('DMR: tap hold');
}

function onTap_playButton (sender){
	
}
