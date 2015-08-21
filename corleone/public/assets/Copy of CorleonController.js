/**
 * 
 */

$(function(err, window) {
       
	function is_array(input) {
		$ret = typeof (input) == 'object' && (input instanceof Array);
		return $ret;
	}

	function split(val) {
		return val.split("(");
	}
	;

	$(".autocomplete").autocomplete(
			{
				source : function(request, response) {
				var test = $(window);
					var availableTags = model.tags;
					var regex = new RegExp('^' + request.term);
					result = $.grep(availableTags, function(s) {
						return s.match(regex)
					});
					var res = !result;
					var res1 = result.length;
					var res2 = $(".autocomplete").val();
					var res3 = $(".autocomplete").val()
							&& $(".autocomplete").val() != "";
					if ((!result || result.length == 0)
							&& $(".autocomplete").val()
							&& $(".autocomplete").val() != "") {

						result = [ $(".autocomplete").val() + '( new value)' ];
						// result.push($(".autocomplete").val() + '( new
						// value)').reverse();
					}
					response(result);
				},
				select : function(event, ui) {
					var itemTitleDesc = split(ui.item.value);
					if (is_array(itemTitleDesc)) {
						var itemTitle = itemTitleDesc[0];
						if (itemTitleDesc.length > 1) {
							var itemDesc = itemTitleDesc[1];
							itemDesc = itemDesc.replace(")", "");
						}
						ui.item.value = itemTitle;
					}
				}
			})
	// .data("autocomplete")._renderItem = function(ul, item) {
	// itemTitleDesc = split(item.value);
	// var desc = "";
	// if (is_array(itemTitleDesc)) {
	// itemTitle = itemTitleDesc[0];
	// if (itemTitleDesc.length > 1) {
	// itemDesc = itemTitleDesc[1];
	// itemDesc = itemDesc.replace(")", "");
	// desc = '<br/><span><small>' + itemDesc + '</small></span>';
	// }
	// var itemContent = '<span><strong>' + itemTitle + '</strong></span>'
	// + desc;
	// return $("<li></li>").data("item.autocomplete", item).append(
	// $("<a></a>").html(itemContent)).appendTo(ul);
	// } else {
	// return $("<li></li>").data("item.autocomplete", item).append(
	// $("<a></a>").html(item.label)).appendTo(ul);
	// }
	// }
	;

});

//$('#translation_search_button').on("click", function search() {
//	get('');
//});

//function update(data) {
//
//	$('#translation_key').text(data.key);
//	$('table tbody').append(constructTranslationsTable(data.Translations));
//}



//function constructTranslationsTable(listTanslation) {
//	var result = "";
//
//	for (i = 0; i < listTanslation.length; i++) {
//		result = result + "<tr>" + "<td data-qid='searchTable_" + i + "'>"
//				+ "<span>" + listTanslation[i].translation1 + "</span>"
//				+ "</td><td>" + "<span>" + listTanslation[i].translation1
//				+ "</span>" + "</td><td>" + "<span></span>"
//				+ "</td><td class='deleterow'>"
//				+ "<div class='glyphicon glyphicon-remove'></div>"
//				+ "</td></tr>";
//	}
//	return result;
//}
function delet(id) {
	var el2 = $(this);
	var el3 = $('#' + id).parents('.tm');
	el3.remove();
}
$('#span-container').on("click", function search() {
	$('#texttest').focus();
	return true;
});
$('#texttest').focus(function(event) {
	$('#span-container').addClass("focus-controoll-tags");
});
$('#texttest').blur(function(event) {
	$('#span-container').removeClass("focus-controoll-tags");
});
$('#span-container')
		.keydown(
				function(e) {
					if (e.keyCode == 8) {

						var test = $('#texttest').val();
						if (!test) {
							$('#tags-collector .tm:last-child').remove();
						}

					}
					if (e.keyCode == 32) {

						var str = $('#texttest').val();
						var result = "<span class='tm'>&nbsp;<span  contenteditable=\"false\""
								+ "class=\"label label-success\""
								+ ">"
								+ "<input readonly=\"readonly\" class=\"input-transparent\" type=\"hidden\" name='PACK_1' value=\""
								+ str
								+ "\">"
								+ str
								+ "&nbsp;"
								+ "<span id=\""
								+ ID()
								+ "\" contenteditable=\"false\" class=\"hover-remove glyphicon glyphicon-remove\" onclick=\"delet(this.id)\"> </span> "
								+ "</span></span>";
						var el = $('#tags-collector');
						el.append(result);
						$('#texttest').val('');
						return false;
					}
				});
var ID = function() {
	return '_' + Math.random().toString(36).substr(2, 9);
};

//
// select : function(event, ui) {
// itemTitleDesc = split(ui.item.value);
// if (is_array(itemTitleDesc)) {
// itemTitle = itemTitleDesc[0];
// if (itemTitleDesc.length > 1) {
// itemDesc = itemTitleDesc[1];
// itemDesc = itemDesc.replace(")", "");
// }
// ui.item.value = itemTitle;
// }
// }
// }).data("autocomplete")._renderItem = function(ul, item) {
// itemTitleDesc = split(item.value);
// var desc = "";
// if (is_array(itemTitleDesc)) {
// itemTitle = itemTitleDesc[0];
// if (itemTitleDesc.length > 1) {
// itemDesc = itemTitleDesc[1];
// itemDesc = itemDesc.replace(")", "");
// desc = '<br/><span><small>' + itemDesc + '</small></span>';
// }
// var itemContent = '<span><strong>' + itemTitle
// + '</strong></span>' + desc;
// return $("<li></li>")
// .data("item.autocomplete", item)
// .append($("<a></a>").html(itemContent))
// .appendTo(ul);
// } else {
// return $("<li></li>")
// .data("item.autocomplete", item)
// .append($("<a></a>").html(item.label))
// .appendTo(ul);
// }
// };







//Iterate over each select element
$('select').each(function () {

    // Cache the number of options
    var $this = $(this),
        numberOfOptions = $(this).children('option').length;

    // Hides the select element
    $this.addClass('s-hidden');

    // Wrap the select element in a div
    $this.wrap('<div class="select"></div>');

    // Insert a styled div to sit over the top of the hidden select element
    $this.after('<div class="styledSelect"></div>');

    // Cache the styled div
    var $styledSelect = $this.next('div.styledSelect');

    // Show the first select option in the styled div
    $styledSelect.text($this.children('option').eq(0).text());

    // Insert an unordered list after the styled div and also cache the list
    var $list = $('<ul />', {
        'class': 'options'
    }).insertAfter($styledSelect);

    // Insert a list item into the unordered list for each select option
    for (var i = 0; i < numberOfOptions; i++) {
        $('<li />', {
            text: $this.children('option').eq(i).text(),
            rel: $this.children('option').eq(i).val()
        }).appendTo($list);
    }

    // Cache the list items
    var $listItems = $list.children('li');

    // Show the unordered list when the styled div is clicked (also hides it if the div is clicked again)
    $styledSelect.click(function (e) {
        e.stopPropagation();
        $('div.styledSelect.active').each(function () {
            $(this).removeClass('active').next('ul.options').hide();
        });
        $(this).toggleClass('active').next('ul.options').toggle();
    });

    // Hides the unordered list when a list item is clicked and updates the styled div to show the selected list item
    // Updates the select element to have the value of the equivalent option
    $listItems.click(function (e) {
        e.stopPropagation();
        $styledSelect.text($(this).text()).removeClass('active');
        $this.val($(this).attr('rel'));
        $list.hide();
        /* alert($this.val()); Uncomment this for demonstration! */
    });

    // Hides the unordered list when clicking outside of it
    $(document).click(function () {
        $styledSelect.removeClass('active');
        $list.hide();
    });

});
