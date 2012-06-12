var dmr_handler;
var dmr_slider;
var dmr_lenght;
var dmr_currentLeft;
var dmr_itemWidth;
var dmr_numberToPresent;

var dmr_selectedItem;
var dmr_taphold;

var btn_dmrPlayController;
var btn_dmrGoNext;
var btn_dmrGoPrevious;

function init_DMRManagement() {
	dmr_taphold = false;
	dmr_lenght = 0;
	dmr_currentLeft = 0;
	dmr_numberToPresent = 3;
	dmr_selectedItem = null;
	dmr_slider =  $('#div_dmr_list_container');
	dmr_slider = $('#div_dmr_list_container');
	dmr_handler = $('#div_dmr_handler');

	dmr_itemWidth = dmr_handler.width() / dmr_numberToPresent;

	btn_dmrGoNext = $('#div_dmr_move_right > img');
	btn_dmrGoNext.hide();

	btn_dmrGoPrevious = $('#div_dmr_move_left > img');
	btn_dmrGoPrevious.hide();

	btn_dmrGoPrevious.bind('tap', function() {
		if ($(this).attr('data-enable') == "true") {
			onTap_DMRmoveLeftButton($(this));
		}
	});

	btn_dmrGoNext.bind('tap', function() {
		if ($(this).attr('data-enable') == "true") {
			onTap_DMRmoveRightButton($(this));
		}
	});

	$('.img_dmr_item').live('tap', function() {
		if (!dmr_taphold)
			onTap_dmrItem($(this));
		else
			dmr_taphold = false;
	});

	$('.img_dmr_item').live('taphold', function() {
		dmr_taphold = true;
		onTapHold_dmrItem($(this));
	});
}

function resetCSS_dmrItems() {
	var listOfDMRitem = $('.div_dmr_item');
	listOfDMRitem.css("width", dmr_itemWidth + 'px');
}

function selectDMRitem(item) {
	if (dmr_selectedItem != null) {
		dmr_selectedItem.attr("data-selected", "false");
		dmr_selectedItem.find("img").css("-webkit-border-radius", "0px");
		dmr_selectedItem.find("img").css("border", "0px");
	}
	dmr_selectedItem = item;
	dmr_selectedItem.attr("data-selected", "true");
	dmr_selectedItem.find("img").css("-webkit-border-radius", "5px");
	dmr_selectedItem.find("img").css("border", "solid 2px #33B5E5");
}

// -----------------------------PUBLIC FUNCTION--------------
function addNewDMRitem(imageUrl, deviceUdn, deviceName) {
	if (imageUrl == null) {
		imageUrl = "img/ic_device_unknow_player.png";
		alert('null ne');
	}
	var containerWidth = dmr_slider.width();
	containerWidth += dmr_itemWidth;
	dmr_slider.css('width', containerWidth + 'px');
	dmr_slider
			.append('<div class="div_dmr_item" data-selected="false" align="center" data-url="'
					+ deviceUdn
					+ '">'
					+ '<img class ="img_dmr_item" src="'
					+ imageUrl
					+ '">'
					+ '<p class ="p_dmr_info">'
					+ deviceName
					+ '</p>'
					+ '</div>');
	resetCSS_dmrItems();
	dmr_lenght++;
	if (dmr_lenght - 1 >= dmr_currentLeft + dmr_numberToPresent) {
		btn_dmrGoNext.show();
	}
}

function removeDMRitem(deviceUdn) {
	console.log("remove : " + deviceUdn);
	var removedItem = $("#div_dmr_list_container div.div_dmr_item[data-url='"
			+ deviceUdn + "']");
	if (removedItem.html() == null)
		return;

	if (removedItem.attr('data-selected') == "true") {
		dmr_selectedItem = null;
		alert('set null');
	}

	var index = removedItem.index();
	removedItem.remove();

	dmr_lenght--;
	var containerWidth = dmr_lenght * dmr_itemWidth;
	dmr_slider.css('width', containerWidth + 'px');

	if (index < dmr_currentLeft) {
		dmr_currentLeft--;
		var offSet = dmr_currentLeft * dmr_itemWidth * -1;
		dmr_slider.animate({
			left : offSet
		}, "slow", function() {
			if (dmr_currentLeft == 0) {
				btn_dmrGoPrevious.hide();
			}
		});
	} else if (index == dmr_currentLeft) {
		if (dmr_currentLeft == 0) {
			if (dmr_lenght <= dmr_numberToPresent) {
				btn_dmrGoNext.hide();
			}
		} else if (dmr_currentLeft > 0) {
			dmr_currentLeft--;
			var offSet = dmr_currentLeft * dmr_itemWidth * -1;
			dmr_slider.animate({
				left : offSet
			}, "slow", function() {
				if (dmr_currentLeft == 0) {
					btn_dmrGoPrevious.hide();
				}
			});
		}
	} else {
		if (dmr_currentLeft == 0) {
			if (dmr_currentLeft + dmr_numberToPresent >= dmr_lenght)
				btn_dmrGoNext.hide();
		} else if (dmr_currentLeft > 0) {
			dmr_currentLeft--;
			var offSet = dmr_currentLeft * dmr_itemWidth * -1;
			dmr_slider.animate({
				left : offSet
			}, "slow", function() {
				if (dmr_currentLeft == 0) {
					btn_dmrGoPrevious.hide();
				}
			});
		}
	}

}

// ------------------------------EVENT FUNCTION---------------------------
function onTap_dmrItem(sender) {
	selectDMRitem(sender.parent());
	choseDMR(sender.attr("data-url"));
}

function onTapHold_dmrItem(sender) {

}

function onTap_playButton(sender) {

}

function onTap_DMRmoveLeftButton(sender) {
	if (dmr_currentLeft > 0) {
		dmr_currentLeft--;
		var offSet = dmr_currentLeft * dmr_itemWidth * -1;
		offSet += 'px';
		dmr_slider.animate({
			left : offSet
		}, "slow", function() {
			if (dmr_currentLeft == 0) {
				btn_dmrGoPrevious.hide();
			}
		});
		if (dmr_lenght - 1 >= dmr_currentLeft + dmr_numberToPresent) {
			btn_dmrGoNext.show();
		}
	}
}

function onTap_DMRmoveRightButton(sender) {
	if (dmr_lenght - 1 >= dmr_currentLeft + dmr_numberToPresent) {
		dmr_currentLeft++;
		var offSet = dmr_currentLeft * dmr_itemWidth * -1;
		offSet += 'px';
		dmr_slider.animate({
			left : offSet
		}, "slow", function() {
			if (dmr_currentLeft + dmr_numberToPresent == dmr_lenght) {
				btn_dmrGoNext.hide();
			}
		});

		if (dmr_currentLeft == 1) {
			btn_dmrGoPrevious.show();
		}
	}
}
