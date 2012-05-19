var time_to_swap_image = 200;

function initImagesManagement (){
	
	$('.img_normal').live(
			'vmousedown',
			function() {
				if ($(this).attr("data-enable") == "true") {
					changeImagePath($(this), $(this).attr('data-highlight-image'));
			}
	});

	$('.img_normal').live(
			'vmouseup',
			function() {
				if ($(this).attr("data-enable") == "true") {
					changeImagePathWithTimeOut($(this), $(this).attr(
							'data-normal-image'), time_to_swap_image);
				}
			});

	$('.img_double_state').live(
			'vmousedown', 
			function() {
				if ($(this).attr("data-enable") == "true") {
					var currentPath = $(this).attr('data-current-path');
					var stateAImgPath = $(this).attr('data-state-a');
					var highlightImagePath = "";
					if (currentPath == stateAImgPath) {
						highlightImagePath = $(this).attr('data-highlight-a');
					} else {
						highlightImagePath = $(this).attr('data-highlight-b');
					}
					changeImagePath($(this), highlightImagePath);
				}
			});

	$('.img_double_state').live(
			'vmouseup',
			function() {
				if ($(this).attr("data-enable") == "true") {
					changeImagePathWithTimeOut($(this), $(this).attr(
							'data-current-path'), time_to_swap_image);
				}
			});
}

function changeImagePath(sender, imgPath) {
	$(sender).attr('src', imgPath);
}

function changeImagePathWithTimeOut(sender, imgPath, timeOut) {
	setTimeout(function() {
		$(sender).attr('src', imgPath);
	}, timeOut);
}

function changeStateImage(sender) {
	var currentPath = $(sender).attr('data-current-path');
	var stateAImgPath = $(sender).attr('data-state-a');
	var stateBImgPath = $(sender).attr('data-state-b');
	if (currentPath == stateAImgPath) {
		$(sender).attr('data-current-path', stateBImgPath);
	} else {
		$(sender).attr('data-current-path', stateAImgPath);
	}
	changeImagePathWithTimeOut(sender, $(sender).attr('data-current-path'),
			time_to_swap_image);
	if ($(sender).attr('data-my-state') == 'true') {
		$(sender).attr('data-my-state', 'false');
	} else {
		$(sender).attr('data-my-state', 'true');
	}
}

function disableButton(sender) {
	sender.attr("data-enable", "false");
	var disableImagePath = sender.attr("data-disable-image");
	sender.removeAttr("src").attr("src", disableImagePath);
}

function enableButton(sender) {
	sender.attr("data-enable", "true");
	var enableImagePath = sender.attr("data-normal-image");
	sender.removeAttr("src").attr("src", enableImagePath);
}