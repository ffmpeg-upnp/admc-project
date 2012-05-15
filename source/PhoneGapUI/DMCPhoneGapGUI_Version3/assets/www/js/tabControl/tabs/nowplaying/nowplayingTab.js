var nowplaying_content_perform_minheight;
var nowplaying_content_perform_maxheight;

function init_NowplayingTab() {
	console.log('init Nowplaying tab');
	$('#div_content_internet_searchbar').hide(); // hide global search bar in
	// internet-tab
	$('#div_content_nowplaying_tab').hide();// hide all nowplaying tab
	console.log("div_content_nowplaying_tab height = "
			+ $('#div_content_nowplaying_tab').height());
	var nowplaying_content_min_height = $('#div_content_nowplaying_tab')
			.height();
	var nowplaying_content_max_height = nowplaying_content_min_height / 70 * 85;
	console.log("nowplaying_content_min_height = "
			+ nowplaying_content_min_height
			+ "; nowplaying_content_max_height = "
			+ nowplaying_content_max_height);

	var nowplaying_content_info_height = nowplaying_content_min_height * 12 / 100;
	var nowplaying_content_controler_height = nowplaying_content_min_height * 38 / 100;

	nowplaying_content_perform_minheight = nowplaying_content_min_height
			- nowplaying_content_info_height
			- nowplaying_content_controler_height;
	nowplaying_content_perform_minheight += 'px';

	nowplaying_content_perform_maxheight = nowplaying_content_max_height
			- nowplaying_content_info_height
			- nowplaying_content_controler_height;
	nowplaying_content_perform_maxheight += 'px';

	init_contentManagement();

	$('#div_content_info_nowplaying').css('height',
			(nowplaying_content_info_height - 1) + 'px');
	$('#div_content_perform_nowplaying').css('top',
			nowplaying_content_info_height + 'px');

	$('#div_content_controler_nowplaying').css('height',
			nowplaying_content_controler_height + 'px');

	$('#div_content_perform_nowplaying').css('height',
			nowplaying_content_perform_minheight);

	$('#div_content_internet_searchbar_right img').bind('tap', function() {

	});
}

function show_NowplayingTab() {
	$('.nowplaying_tab').fadeIn("fast");
}

function hide_NowplayingTab() {
	$('.nowplaying_tab').hide();
	$('#div_navbar_1 div').css("backgroundColor", "#33B5E5");
	$('#div_navbar_2 div').css("backgroundColor", "black");
}