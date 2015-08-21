function validateCreationForm() {
	var result = true;
	var errors = "<div class=\"alert alert-danger\" role=\"alert\">"
	if (!$("input[name='tags']") || $("input[name='tags']").length == 0) {
		errors += buildError("You should specify at least one tag.")
		result = false;
	}
	var t = $("input[name='translationsmessage']");
	var m = $("input[name='translationslanguage']");
	if (!t || t.length == 0) {
		errors += buildError("You should specify at least one translation message.")
		result = false;
	}
	if (!m || m.length == 0) {
		result = false;
		errors += buildError("You should specify at least one translation code.")
	}

	var unique_values = {};

	t.each(function(index) {
		var translationlanguage=$("input[name='translationslanguage']:eq(" + index + ")").val();
		var translationmessage=$("input[name='translationsmessage']:eq(" + index + ")").val();
		if (!translationlanguage) {
			errors += buildError("Some translation message are empty.")
			result = false;
			return false
		}
		if (!translationmessage) {
			errors += buildError("Some translation code are empty.")
			result = false;
			return false;
		}

		if (!unique_values[translationlanguage]) {
			unique_values[translationlanguage] = true;
		} else {
			errors += buildError("There are multiple translations for language "
					+ translationlanguage + ".")
			result = false;
			return false;
		}
	});
	if (!$("input[name='key']").val()) {
		errors += buildError("You should specify a key.")

		result = false;
	}
	errors += "</div>"
	if (!result) {
		$("#notification").empty();
		$("#notification").append(errors);
		return false;
	}
}
function buildError(errorMsg) {
	return "<div>"+
	"<span class=\"glyphicon glyphicon-exclamation-sign\" aria-hidden=\"true\"></span>"+
	"<span class=\"sr-only\">Error:</span>" + errorMsg+
	"</div>";
	// </div>

}
$('select').each(function() {

	var arr = $(this).attr('id').split('_');
	var index = arr[1];
	// Cache the number of options
	var $this = $(this), numberOfOptions = $(this).children('option').length;

	// Hides the select element
	$this.addClass('s-hidden');

	// Wrap the select element in a div
	$this.wrap('<div class="select"></div>');

	// Insert a styled div to sit over the top of the hidden select element
	$this.after('<div class="styledSelect"></div>');

	// Cache the styled div
	var $styledSelect = $this.next('div.styledSelect');

	// Show the first select option in the styled div
	$styledSelect.text($this.val());

	// Insert an unordered list after the styled div and also cache the list
	var $list = $('<ul />', {
		'class' : 'options'
	}).insertAfter($styledSelect);

	// Insert a list item into the unordered list for each select option
	for (var i = 0; i < numberOfOptions; i++) {
		$('<li />', {
			text : $this.children('option').eq(i).text(),
			rel : $this.children('option').eq(i).val()
		}).appendTo($list);
	}

	// Cache the list items
	var $listItems = $list.children('li');

	// Show the unordered list when the styled div is clicked (also hides it if
	// the div is clicked again)
	$styledSelect.click(function(e) {
		e.stopPropagation();
		$('div.styledSelect.active').each(function() {
			$(this).removeClass('active').next('ul.options').hide();
		});
		$(this).toggleClass('active').next('ul.options').toggle();
	});

	// Hides the unordered list when a list item is clicked and updates the
	// styled div to show the selected list item
	// Updates the select element to have the value of the equivalent option
	$listItems.click(function(e) {
		e.stopPropagation();
		$styledSelect.text($(this).text()).removeClass('active');
		$this.val($(this).attr('rel'));
		$("#language_value_" + index).val($(this).attr('rel'));
		$list.hide();
	});

	// Hides the unordered list when clicking outside of it
	$(document).click(function() {
		$styledSelect.removeClass('active');
		$list.hide();
	});

});