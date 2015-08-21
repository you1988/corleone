function validateLanguage(id) {
	var arr = id.split('_');
	var isValid=true;
if(!$("#language_value_" +arr[1]).val()){
	isValid=false;
}
	if(!$("#message_value_" +arr[1]).val()){
	isValid=false;
	}
	return isValid;
}	
function addLanguage(id) {
	if(!validateLanguage(id)) return;
	
		var arr = id.split('_');
		 $("#language_value_" + arr[1]).attr("name", "translationslanguage");
		 $("#message_value_" + arr[1]).attr("name", "translationsmessage");
		 $("#message_value_" + arr[1]).attr("required", "required");
		var index = parseInt(arr[1]) +1;
		var result =  " <div class=\"row creation-component-zero\">"+
			"<div class=\"col-md-2 creation-language-zero\">"+
				"<select id=\"select_"+index+"\">";
		var selectId='select_'+arr[1];
				    $("#" + selectId+ " option").each(function()
				    		{
				    	result+="<option value=\""+$(this).val()+"\">" +$(this).text() +"</option>";
				    		
				    		});


			result+="</select>"+"</div>"+
				"<div class=\"col-md-9 creation-input-div-zero\">"+
					"<input id=\"message_value_"+index+"\" class=\"form-control creation-input-zero\" type=\"text\" placeholder=\"Add your translation\">"+ "</input>"+
					"<input id=\"language_value_"+index+"\"  type=\"hidden\">"+ "</input>"+

				"</div>"+
							"<div id =\"language_"+index+ "\" class=\"col-md-1 translation-trash-div\" onclick=\"addLanguage(this.id)\">"+
						"<button id=\"translation_remove_button\" type=\"button\""+
							"class=\"btn btn-default trash-button\">"+"<span id =\"span_language_"+index+ "\"class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\">"+"</span>"+"</button>"+
			"</div>"+
			"</div>";
		var m = "#" + id;
		var el  = $(m).parent();
		el.after(result);
		var k = "#span_" + id;
		$(k).removeClass("glyphicon-trash").addClass("glyphicon-trash");
		$(m).attr("onclick","deletLanguage(this.id)");
		$("#select_"+index).each(function () {

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
		    $styledSelect.text($this.val());

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
		        $("#language_value_"+index).val($(this).attr('rel'));
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
	
	}
	;
function deletLanguage(id) {
$("#" + id).parent().remove();
			
	}
	;

