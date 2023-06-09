/******************************************************************************
 * Copyright © 2020-2021    The BENED Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * BENED software, including this file, may be copied, modified, propagated,  *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {
	NRS.automaticallyCheckRecipient = function() {
        var $recipientFields = $("#send_money_recipient, #transfer_asset_recipient, #transfer_currency_recipient, " +
        "#send_message_recipient, #add_contact_account_id, #update_contact_account_id, #lease_balance_recipient, " +
        "#transfer_alias_recipient, #sell_alias_recipient, #set_account_property_recipient, #delete_account_property_recipient, " +
		"#add_monitored_account_recipient,  #send_money_recipient_public_key");

		$recipientFields.on("blur", function() {
			$(this).trigger("checkRecipient");
		});

		$recipientFields.on("checkRecipient", function() {
			var value = $(this).val();
			var modal = $(this).closest(".modal");
//a+
                        if (value && value.length===64 && (modal.find("input[name=recipient]").val()==="" || modal.find("input[name=recipient]").val()==="bened_________________")) {                         
                            modal.find("input[name=recipient]").val(NRS.getAccountIdFromPublicKey(value, true) );
                        }
///a-
			if (value && value != "bened_________________") {
				NRS.checkRecipient(value, modal);
			} else {
				modal.find(".account_info").hide();
			}
		});

		$recipientFields.on("oldRecipientPaste", function() {
			var modal = $(this).closest(".modal");


			var callout = modal.find(".account_info").first();

			callout.removeClass("callout-info callout-danger callout-warning").addClass("callout-danger").html($.t("error_numeric_ids_not_allowed")).show();
		});
	};

	$("#send_message_modal, #send_money_modal, #transfer_currency_modal, #add_contact_modal, #set_account_property_modal, #delete_account_property_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);
		var account = $invoker.data("account");
		if (!account) {
			account = $invoker.data("contact");
		}
		if (account) {
			var $inputField = $(this).find("input[name=recipient], input[name=account_id]").not("[type=hidden]");
			if (!/bened/i.test(account)) {
				$inputField.addClass("noMask");
			}
			$inputField.val(account).trigger("checkRecipient");
		}
	});

	//todo later: http://twitter.github.io/typeahead.js/
	var modal = $(".modal");
    modal.on("click", "span.recipient_selector button, span.plain_address_selector button", function(e) {   
		if (!Object.keys(NRS.contacts).length) {
			e.preventDefault();
			e.stopPropagation();
			return;
		}

		var $list = $(this).parent().find("ul");

		$list.empty();

		for (var accountId in NRS.contacts) {
			if (!NRS.contacts.hasOwnProperty(accountId)) {
				continue;
			}
			$list.append("<li><a href='#' data-contact-id='" + accountId + "' data-contact='" + String(NRS.contacts[accountId].name).escapeHTML() + "'>" + String(NRS.contacts[accountId].name).escapeHTML() + "</a></li>");
		}
	});

	modal.on("click", "span.recipient_selector ul li a", function(e) {
		e.preventDefault();
		$(this).closest("form").find("input[name=converted_account_id]").val("");
		$(this).closest("form").find("input[name=recipient],input[name=account_id]").not("[type=hidden]").trigger("unmask").val($(this).data("contact")).trigger("blur");
	});

	modal.on("click", "span.plain_address_selector ul li a", function(e) {
		e.preventDefault();
		$(this).closest(".input-group").find("input.plain_address_selector_input").not("[type=hidden]").trigger("unmask").val($(this).data("contact-id")).trigger("blur");
	});

	modal.on("keyup blur show", ".plain_address_selector_input", function() {
		var currentValue = $(this).val();
		var contactInfo;
		if (NRS.contacts[currentValue]) {
			contactInfo = NRS.contacts[currentValue]['name'];
		} else {
			contactInfo = " ";
		}
		$(this).closest(".input-group").find(".pas_contact_info").text(contactInfo);
	});

	NRS.forms.sendMoneyComplete = function(response, data) {
		if (!(data["_extra"] && data["_extra"].convertedAccount) && !(data.recipient in NRS.contacts)) {
			$.growl($.t("success_send_money") + " <a href='#' data-account='" + NRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>" + $.t("add_recipient_to_contacts_q") + "</a>", {
				"type": "success"
			});
		} else {
			$.growl($.t("send_money_submitted"), {
				"type": "success"
			});
		}
	};

	NRS.sendMoneyShowAccountInformation = function(accountId) {
		NRS.getAccountError(accountId, function(response) {
			if (response.type == "success") {
				$("#send_money_account_info").hide();
			} else {
				$("#send_money_account_info").html(response.message).show();

			}
		});
	};

	NRS.getAccountError = function(accountId, callback) {  
                
           
		NRS.sendRequest("getAccount", {
			"account": accountId
		}, function(response) {
			if (response.publicKey) {
				if (response.name){
					callback({
						"type": "info",
						"message": $.t("recipient_info_with_name", {
							"name" : response.name,
							"techcoin": NRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
						}),
						"account": response,
                                                "publicKey":response.publicKey
					});
				}
				else{
					callback({
						"type": "info",
						"message": $.t("recipient_info", {
							"techcoin": NRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
						}),
						"account": response,
                                                "publicKey":response.publicKey
					});
				}
			} else {
				if (response.errorCode) {
					if (response.errorCode == 4) {
						callback({
							"type": "danger",
							"message": $.t("recipient_malformed") + (!/^(bened)/i.test(accountId) ? " " + $.t("recipient_alias_suggestion") : ""),
							"account": null
						});
					} else if (response.errorCode == 5) {
						callback({
							"type": "warning",
							"message": $.t("recipient_unknown_pka"),
							"account": null,
							"noPublicKey": true
						});
					} else {
						callback({
							"type": "danger",
							"message": $.t("recipient_problem") + " " + String(response.errorDescription).escapeHTML(),
							"account": null
						});
					}
				} else {
					callback({
						"type": "warning",
						"message": $.t("recipient_no_public_key_pka", {
							"techcoin": NRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
						}),
						"account": response,
						"noPublicKey": true
					});
				}
			}
		});
	};

	NRS.correctAddressMistake = function(el) {
		$(el).closest(".modal-body").find("input[name=recipient],input[name=account_id]").val($(el).data("address")).trigger("blur");
	};

	NRS.checkRecipient = function(account, modal) {      
		var classes = "callout-info callout-danger callout-warning";

		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");
		var merchantInfoField = modal.find("input[name=merchant_info]");

		accountInputField.val("");
		merchantInfoField.val("");

		account = $.trim(account);
                


 if(modal.find("input[name=recipientPublicKey]").val()!="" && /^(bened)?[a-zA-Z0-9]+/i.test(account))return ;
		//solomon reed. Btw, this regex can be shortened..
		if (/^(bened)?[a-zA-Z0-9]+/i.test(account)) {
			var address = new BenedAddress();
			if (address.set(account)) {
				NRS.getAccountError(account, function(response) {
					if (response.noPublicKey && account!=NRS.accountRS) {
						modal.find(".recipient_public_key").show();                                               
					} else {
                                                if(modal.find("input[name=recipientPublicKey]").val()===response.publicKey)return ;
                                                if(response.publicKey){
                                                    modal.find("input[name=recipientPublicKey]").val(response.publicKey);
                                                }else{
                                                   modal.find("input[name=recipientPublicKey]").val(""); 
                                                }
						
						modal.find(".recipient_public_key").show();                                                 
					}
					if (response.account && response.account.description) {
						checkForMerchant(response.account.description, modal);
					}

					if (account==NRS.accountRS){
						callout.removeClass(classes).addClass("callout-" + response.type).html("This is your account").show();
                                        }else{                                           
						var message = response.message.escapeHTML();
						callout.removeClass(classes).addClass("callout-" + response.type).html(message).show();
					}
				});
			} else {
				if (address.guess.length == 1) {                                  
					callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed_suggestion", {
						"recipient": "<span class='malformed_address' data-address='" + String(address.guess[0]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[0], account) + "</span>"
					})).show();
				} else if (address.guess.length > 1) {                                   
					var html = $.t("recipient_malformed_suggestion", {
						"count": address.guess.length
					}) + "<ul>";
					for (var i = 0; i < address.guess.length; i++) {
						html += "<li><span class='malformed_address' data-address='" + String(address.guess[i]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[i], account) + "</span></li>";
					}

					callout.removeClass(classes).addClass("callout-danger").html(html).show();
				} else {                                   
					callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
                                        
				}
			}
		} else if (!(/^\d+$/.test(account))) {                    
			if (account.charAt(0) != '@') {;                            
				NRS.storageSelect("contacts", [{
					"name": account
				}], function(error, contact) {
					if (!error && contact.length) {                                            
						contact = contact[0];
						NRS.getAccountError(contact.accountRS, function(response) {
							if (response.noPublicKey && account!=NRS.account) {
								modal.find(".recipient_public_key").show();                                                               
							} else {                                                            
								modal.find("input[name=recipientPublicKey]").val("");
								modal.find(".recipient_public_key").show();
							}
							if (response.account && response.account.description) {
								checkForMerchant(response.account.description, modal);
							}

							callout.removeClass(classes).addClass("callout-" + response.type).html($.t("contact_account_link", {
								"account_id": NRS.getAccountFormatted(contact, "account")
							}) + " " + response.message.escapeHTML()).show();

							if (response.type == "info" || response.type == "warning") {
								accountInputField.val(contact.accountRS);
							}
						});
					} else if (/^[a-zA-Z0-9]+$/i.test(account)) {
//						NRS.checkRecipientAlias(account, modal);
					} else {                                           
						callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
					}
				});
			} else if (/^[a-zA-Z0-9@]+$/i.test(account)) {                            
				if (account.charAt(0) == '@') {
					account = account.substring(1);
//					NRS.checkRecipientAlias(account, modal);
				}
			} else {                           
				callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
			}
		} else {
			callout.removeClass(classes).addClass("callout-danger").html($.t("error_numeric_ids_not_allowed")).show();
		}
	};

	function checkForMerchant(accountInfo, modal) {
		var requestType = modal.find("input[name=request_type]").val();

		if (requestType == "sendMoney" || requestType == "transferAsset") {
			if (accountInfo.match(/merchant/i)) {
				modal.find("input[name=merchant_info]").val(accountInfo);
				var checkbox = modal.find("input[name=add_message]");
				if (!checkbox.is(":checked")) {
					checkbox.prop("checked", true).trigger("change");
				}
			}
		}
	}

	return NRS;
}(NRS || {}, jQuery));
