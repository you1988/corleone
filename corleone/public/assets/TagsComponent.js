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
			});

   });
function delet(id) {
	var el3 = $('#parent_' + id);
	el3.remove();
}
$('#tags-span-container').on("click", function search() {
	$('#tag-input').focus();
	return true;
});
$('#tag-input').focus(function(event) {
	$('#tags-span-container').addClass("focus-controoll-tags");
});
$('#tag-input').blur(function(event) {
	$('#tags-span-container').removeClass("focus-controoll-tags");
});
$('#tags-span-container')
		.keydown(
				function(e) {
				//case delete clicked
					if (e.keyCode == 8) {

						var test = $('#tag-input').val();
						if (!test) {
							$('#tags-collector .tag-span:last-child').remove();
						}

					}
					//case space clicked
					if (e.keyCode == 32) {
						var str = $('#tag-input').val().trim();
						var id = ID()
						var result = "<span id=\"parent_"+id+"\" class='tag-span'>&nbsp;<span  contenteditable=\"false\""
								+ "class=\"label label-success\""
								+ ">"
								+ "<input readonly=\"readonly\" class=\"input-transparent\" type=\"hidden\" name='tags' value=\""
								+ str
								+ "\">"
								+ str
								+ "&nbsp;"
								+ "<span id=\""
								+ id
								+ "\" contenteditable=\"false\" class=\"hover-remove glyphicon glyphicon-remove\" onclick=\"delet(this.id)\"> </span> "
								+ "</span></span>";
						var el = $('#tags-collector');
						var elementType = el.prop('tagName');
						el.append(result);
						$('#tag-input').val('');
						return false;
					}
				});
var ID = function() {
	return '_' + Math.random().toString(36).substr(2, 9);
};