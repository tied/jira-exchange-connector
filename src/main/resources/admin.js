(function($) { // this closure helps us keep our variables to ourselves.
// This pattern is known as an "iife" - immediately invoked function expression

	// form the URL
	var url = window.location.href + "?config=true";
	// alert("URL:"+url);

	String.prototype.escapeSpecialChars = function() {
		return this.replace(/\\n/g, "\\n").replace(/\\'/g, "\\'").replace(
				/\\"/g, '\\"').replace(/\\&/g, "\\&").replace(/\\r/g, "\\r")
				.replace(/\\t/g, "\\t").replace(/\\b/g, "\\b").replace(/\\f/g,
						"\\f");
	};

	// wait for the DOM (i.e., document "skeleton") to load. This likely isn't
	// necessary for the current case,
	// but may be helpful for AJAX that provides secondary content.
	$(document).ready(
			function() {
				// request the config information from the server
				$.ajax({
					url : url,
					dataType : "json"
				}).done(
						function(config) { // when the configuration is
							// returned...
							// ...populate the form.
							$("#active").prop('checked', config.active);
							$("#imapServer").val(config.imapServer);
							$("#imapUserName").val(config.imapUserName);
							$("#imapPassword").val(config.imapPassword);
							$("#imapInboxName").val(config.imapInboxName);
							$("#projectName").val(config.projectName);
							$("#issueOwner").val(config.issueOwner);
							$("#issueType").val(config.issueType);
							$("#issueStatus").val(config.issueStatus);
							$("#imapDeleteMessage").prop('checked',
									config.imapDeleteMessage);

							function updateConfig() {
								var data = '{ "active": '
										+ AJS.$("#active").prop("checked")
										+ ', "imapDeleteMessage": '
										+ AJS.$("#imapDeleteMessage").prop(
												"checked")
										+ ', "imapServer": "'
										+ AJS.$("#imapServer").attr("value")
												.escapeSpecialChars()
										+ '", "imapUserName": "'
										+ AJS.$("#imapUserName").attr("value")
												.escapeSpecialChars()
										+ '", "imapPassword": "'
										+ AJS.$("#imapPassword").attr("value")
												.escapeSpecialChars()
										+ '", "imapInboxName": "'
										+ AJS.$("#imapInboxName").attr("value")
												.escapeSpecialChars()
										+ '", "projectName": "'
										+ AJS.$("#projectName").attr("value")
												.escapeSpecialChars()
										+ '", "issueOwner": "'
										+ AJS.$("#issueOwner").attr("value")
												.escapeSpecialChars()
										+ '", "issueType": "'
										+ AJS.$("#issueType").attr("value")
												.escapeSpecialChars()
										+ '", "issueStatus": "'
										+ AJS.$("#issueStatus").attr("value")
												.escapeSpecialChars() + '" }';

								// console.log('Sending: ' + data);
								// alert("v:" + data);
								AJS.$.ajax({
									url : url,
									type : "POST",
									contentType : "application/json",
									data : data,
									processData : false
								});

							}

							AJS.$("#admin").submit(function(e) {
								e.preventDefault();
								updateConfig();
							});

						});
			});

})(AJS.$ || jQuery);