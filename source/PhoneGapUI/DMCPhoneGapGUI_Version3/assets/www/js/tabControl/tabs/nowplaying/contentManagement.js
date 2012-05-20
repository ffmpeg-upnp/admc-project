/*var content_renderer;

var content_handler;
var content_slider;
var content_mouseDown;
var content_mouseX;
var content_lenght;
var content_currentIndex;
var content_xOffset;
var content_handlerWidth;
var content_tolerance;
var content_delayTime;

var content_selectedItem;

function init_contentManagement (){
	content_mouseDown = false;
	content_mouseX = 0;
	content_lenght = 0;
	content_currentIndex = 0;
	content_xOffset = 0;
	content_handlerWidth = 0;
	content_tolerance = 0.25;
	content_delayTime = 300;
	
	content_slider = $('#div_content_perform_container');
	content_handler = $('#div_content_perform_handler');
	content_handlerWidth = $(window).width();
	
	resetWidth_contentItems();
	selectContentItem(null);
	
	content_handler.bind('vmousedown', function(event){
		onMouseDown_contentHandler(event);
	});
	
	content_handler.bind('vmousemove', function(event){
		onMouseMove_contentHandler(event);
	});
	
	content_handler.bind('vmouseup', function(event){
		onMouseUp_contentHandler(event);
	});
	
	content_renderer = $("#div_content_info_nowplaying > div");
	var fontHeight_contentManagement = $(window).height() * 0.055;
	fontHeight_contentManagement = Math.round(fontHeight_contentManagement);
	content_renderer.css("fontSize", fontHeight_contentManagement + "px");
}

function resetWidth_contentItems (){
	if (content_lenght > 0){
		var listOfContentItem = $('.div_content_item');
		listOfContentItem.css("width", content_handlerWidth + 'px');
	}else{ //== 0
		content_slider.css("width", "0%");
	}
}

function selectContentItem (item){
	content_selectedItem = item;
	if (content_selectedItem != null){
		var title = content_selectedItem.attr("data-title");
		content_renderer.html(title);
		content_renderer.hide();
		content_renderer.fadeIn("slow");
	}
}

function onMouseDown_contentHandler (event){
	if (!content_mouseDown && content_lenght > 0){
		content_mouseDown = true;
		content_mouseX = event.pageX;
	}
}

function onMouseMove_contentHandler (event){
	if (content_mouseDown){
		content_xOffset = event.pageX - content_mouseX;
		if (content_xOffset < 0){
			//if (content_currentIndex < content_lenght - 1)
				content_slider.css('left', - content_currentIndex * content_handlerWidth + content_xOffset);
		}else{
			//if (content_currentIndex > 0)
				content_slider.css('left', - content_currentIndex * content_handlerWidth + content_xOffset);
		}
	}
}

function onMouseUp_contentHandler (event){
	content_mouseDown = false;
	if (content_xOffset == 0){
		return false;
	}
	
	var fullWidth = content_handlerWidth;
	var haflWidth = fullWidth / 2;
	var isAtLastItem = false;
	var isAtFirstItem = false;
	
	if (-content_xOffset > haflWidth - fullWidth * content_tolerance){
		content_currentIndex++;
		if (content_currentIndex >= content_lenght){
			isAtLastItem = true;
			content_currentIndex = content_lenght - 1;
		}
		content_slider.animate({left: -content_currentIndex * content_handlerWidth}, content_delayTime, 
				function(){
			if (!isAtLastItem)
				selectContentItem($('#div_content_perform_container div:eq(' + content_currentIndex +  ')'));
		});
	}else if (content_xOffset > haflWidth - fullWidth * content_tolerance){
		content_currentIndex--;
		if (content_currentIndex < 0){
			isAtFirstItem = true;
			content_currentIndex = 0;
		}
		content_slider.animate({left: -content_currentIndex * content_handlerWidth}, content_delayTime, 
				function (){
			if (!isAtFirstItem)
				selectContentItem($('#div_content_perform_container div:eq(' + content_currentIndex +  ')'));
		});
	}else{
		content_slider.animate({left: -content_currentIndex * content_handlerWidth}, content_delayTime);
	}
	content_xOffset = 0;
	return true;
}

//-----------------------------PUBLIC FUNCTION--------------
function addNewContentItem (contentType, contentTitle, contentPerformanceUrl, contentUrl){
	if (contentPerformanceUrl == null){
		switch (contentType) {
		case "audio":
			contentPerformanceUrl = "img/ic_didlobject_audio_large.png";
			break;

		case "image":
			contentPerformanceUrl = "img/ic_didlobject_image_large.png";
			break;
			
		case "video":
			contentPerformanceUrl = "img/ic_didlobject_video_large.png";
			break;
		}
	}
	var containerWidth = content_slider.width();
	containerWidth += content_handlerWidth;
	content_slider.css('width', containerWidth + 'px');
	content_slider.append('<div class="div_content_item" data-title="' + contentTitle +  '" data-url="' + contentUrl + '">' + 
			'<img class ="img_content_item" src="' + contentPerformanceUrl + '">' +
			'</div>');
	content_lenght++;
	resetWidth_contentItems();
	
	if (content_lenght == 1){
		selectContentItem($('#div_content_perform_container div:eq(0)'));
	}
}

*/

/*---------------------------------------------NEW CODE------------------------------------*/
var content_handler;
var content_slider;
var content_lenght;
var content_currentLeft;
var content_itemWidth;
var content_numberToPresent;
var content_selectedItem;

var content_renderer;

var btn_contentGoNext;
var contentGoNext_visible;
var btn_contentGoPrevious;
var contentGoPrevious_visible;

function init_contentManagement() {
	content_lenght = 0;
	content_currentLeft = 0;
	content_numberToPresent = 1;
	content_selectedItem = null;
	content_slider =  $('#div_content_perform_container');
	content_handler = $('#div_content_perform_handler');
	content_itemWidth = ($(window).width() * 0.8) / content_numberToPresent;
	//content_itemWidth = content_handler.width() / content_numberToPresent;
	
	var marginValue = $(window).width() * 0.05 * -1;
	marginValue = Math.round(marginValue);
	$('.div_content_perform_controller > img').css('marginTop', marginValue + 'px');
	
	btn_contentGoNext = $('#div_content_move_right > img');
	btn_contentGoNext.hide();
	contentGoNext_visible = false;
	btn_contentGoPrevious = $('#div_content_move_left > img');
	btn_contentGoPrevious.hide();
	contentGoPrevious_visible = false;
	
	btn_contentGoPrevious.bind ('tap', function (){
		if ($(this).attr('data-enable') == "true"){
			onTap_ContenTmoveLeftButton ($(this));
		}
	});
	
	btn_contentGoNext.bind ('tap', function (){
		if ($(this).attr('data-enable') == "true"){
			onTap_ContenTmoveRightButton ($(this));
		}
	});
	
	content_renderer = $("#div_content_info_nowplaying > div");
	var fontHeight_contentManagement = $(window).height() * 0.055;
	fontHeight_contentManagement = Math.round(fontHeight_contentManagement);
	content_renderer.css("fontSize", fontHeight_contentManagement + "px");
}

function resetCSS_ContentItems() {
	var listOfContentItem = $('.div_content_item');
	listOfContentItem.css("width", content_itemWidth + 'px');
}

function showGoNextButton_CM (){
	btn_contentGoNext.show();
	contentGoNext_visible = true;
}

function hideGoNextButton_CM (){
	btn_contentGoNext.hide();
	contentGoNext_visible = false;
}

function showGoPreviousButton_CM(){
	btn_contentGoPrevious.show();
	contentGoPrevious_visible = true;
}

function hideGoPreviousButton_CM (){
	btn_contentGoPrevious.hide();
	contentGoPrevious_visible = false;
}

function selectContentItem (item){
	content_selectedItem = item;
	if (content_selectedItem != null){
		var title = content_selectedItem.attr("data-title");
		content_renderer.html(title);
		content_renderer.hide();
		content_renderer.fadeIn("slow");
	}
}
// -----------------------------PUBLIC FUNCTION--------------
/*function addNewContentItem(imageUrl, deviceUdn, deviceName) {
	if (imageUrl == null) {
		imageUrl = "img/ic_device_unknow_player.png";
	}
	var containerWidth = content_slider.width();
	containerWidth += content_itemWidth;
	content_slider.css('width', containerWidth + 'px');
	content_slider
			.append('<div class="div_dmr_item" data-selected="false" align="center" data-url="'
					+ deviceUdn
					+ '">'
					+ '<img class ="img_dmr_item" src="'
					+ imageUrl
					+ '">'
					+ '</div>');
	resetCSS_ContentItems();
	content_lenght++;
	if (content_lenght - 1 >= content_currentLeft + content_numberToPresent ){
		btn_contentGoNext.show();
	}
}*/

function addNewContentItem (contentType, contentTitle, contentPerformanceUrl, contentUrl){
	if (contentPerformanceUrl == null){
		switch (contentType) {
		case "audio":
			contentPerformanceUrl = "img/ic_didlobject_audio_large.png";
			break;

		case "image":
			contentPerformanceUrl = "img/ic_didlobject_image_large.png";
			break;
			
		case "video":
			contentPerformanceUrl = "img/ic_didlobject_video_large.png";
			break;
		}
	}
	var containerWidth = content_slider.width();
	containerWidth += content_itemWidth;
	content_slider.css('width', containerWidth + 'px');
	content_slider.append('<div class="div_content_item" data-title="' + contentTitle +  '" data-url="' + contentUrl + '">' + 
			'<img class ="img_content_item" src="' + contentPerformanceUrl + '">' +
			'</div>');
	content_lenght++;
	resetCSS_ContentItems();
	if (content_lenght == 1){
		selectContentItem($('.div_content_item:eq(0)'));
	}
	if (content_lenght - 1 >= content_currentLeft + content_numberToPresent ){
		showGoNextButton_CM();
	}
}

function animateDown_contentManagement (){
	refreshImage_contentManagement();
}

function animateUp_contentManagement(){
	refreshImage_contentManagement();
}

function refreshImage_contentManagement (){
	if (contentGoNext_visible){
		btn_contentGoNext.hide(function(){
			btn_contentGoNext.fadeIn("fast");
		});
		
	}
	if (contentGoPrevious_visible){
		btn_contentGoPrevious.hide(function(){
			btn_contentGoPrevious.fadeIn("fast");
		});
	}
}

// ------------------------------EVENT FUNCTION---------------------------
function onTap_ContenTmoveLeftButton (sender){
	if (content_currentLeft > 0){
		content_currentLeft--;
		var offSet = content_currentLeft * content_itemWidth * -1;
		offSet += 'px';
		content_slider.animate({left: offSet}, "slow", function(){
			selectContentItem($('.div_content_item:eq(' + content_currentLeft + ')'));
			if (content_currentLeft == 0){
				hideGoPreviousButton_CM();
			}
			
		});
		if (content_lenght - 1 >= content_currentLeft + content_numberToPresent){
			showGoNextButton_CM();
		}
	}
}

function onTap_ContenTmoveRightButton (sender){
	if (content_lenght - 1 >= content_currentLeft + content_numberToPresent ){
		content_currentLeft++;
		var offSet = content_currentLeft * content_itemWidth * -1;
		offSet += 'px';
		content_slider.animate({left: offSet}, "slow", function(){
			selectContentItem($('.div_content_item:eq(' + content_currentLeft + ')'));
			if (content_currentLeft + content_numberToPresent == content_lenght){
				hideGoNextButton_CM();
			}
		});
		
		if (content_currentLeft == 1){
			showGoPreviousButton_CM();
		}
	}
}

