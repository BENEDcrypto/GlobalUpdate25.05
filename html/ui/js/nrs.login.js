/******************************************************************************
 * Copyright © 2020-2021    The BENED Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * BENED software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
 var NRS = (function(NRS, $, undefined) {
	NRS.newlyCreatedAccount = false;

	NRS.allowLoginViaEnter = function() {
		$("#login_account_other").keypress(function(e) {
			if (e.which == '13') {
				e.preventDefault();
				var account = $("#login_account_other").val();
				NRS.login(false,account);
			}
		});
		$("#login_password").keypress(function(e) {
			if (e.which == '13') {
				e.preventDefault();
				var password = $("#login_password").val();
				NRS.login(true,password);
			}
		});
	};

	NRS.showLoginOrWelcomeScreen = function() {
		if (localStorage.getItem("logged_in")) {
			NRS.showLoginScreen();
		} else {
			NRS.showWelcomeScreen();
		}
	};

	NRS.showLoginScreen = function() {
		$("#account_phrase_custom_panel, #account_phrase_generator_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#account_phrase_custom_panel").find(":input:not(:button):not([type=submit])").val("");
		$("#account_phrase_generator_panel").find(":input:not(:button):not([type=submit])").val("");
        $("#login_account_other").mask("bened*****************");

		$("#login_panel").show();
		setTimeout(function() {
			$("#login_password").focus()
		}, 10);
	};

	NRS.showWelcomeScreen = function() {
		$("#login_panel, #account_phrase_generator_panel, #account_phrase_custom_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#welcome_panel").show();
	};

	NRS.registerUserDefinedAccount = function() {
		$("#account_phrase_generator_panel, #login_panel, #welcome_panel, #custom_passphrase_link").hide();
		$("#account_phrase_generator_panel").find(":input:not(:button):not([type=submit])").val("");
		var accountPhraseCustomPanel = $("#account_phrase_custom_panel");
        accountPhraseCustomPanel.find(":input:not(:button):not([type=submit])").val("");
		accountPhraseCustomPanel.show();
		$("#registration_password").focus();
	};

	NRS.registerAccount = function() {
		$("#login_panel, #welcome_panel").hide();
		var accountPhraseGeneratorPanel = $("#account_phrase_generator_panel");
        accountPhraseGeneratorPanel.show();
		accountPhraseGeneratorPanel.find(".step_3 .callout").hide();

		var $loading = $("#account_phrase_generator_loading");
		var $loaded = $("#account_phrase_generator_loaded");

		//noinspection JSUnresolvedVariable
		if (window.crypto || window.msCrypto) {
			$loading.find("span.loading_text").html($.t("generating_passphrase_wait"));
		}

		$loading.show();
		$loaded.hide();

		if (typeof PassPhraseGenerator == "undefined") {
			$.when(
				$.getScript("js/crypto/passphrasegenerator.js")
			).done(function() {
				$loading.hide();
				$loaded.show();

				PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
			}).fail(function() {
				alert($.t("error_word_list"));
			});
		} else {
			$loading.hide();
			$loaded.show();

			PassPhraseGenerator.generatePassPhrase("#account_phrase_generator_panel");
		}
	};

	NRS.verifyGeneratedPassphrase = function() {
		var accountPhraseGeneratorPanel = $("#account_phrase_generator_panel");
        var password = $.trim(accountPhraseGeneratorPanel.find(".step_3 textarea").val());

		if (password != PassPhraseGenerator.passPhrase) {
			accountPhraseGeneratorPanel.find(".step_3 .callout").show();
		} else {
			NRS.newlyCreatedAccount = true;
			NRS.login(true,password);
			PassPhraseGenerator.reset();
			accountPhraseGeneratorPanel.find("textarea").val("");
			accountPhraseGeneratorPanel.find(".step_3 .callout").hide();
		}
	};

	$("#account_phrase_custom_panel").find("form").submit(function(event) {
		event.preventDefault();

		var password = $("#registration_password").val();
		var repeat = $("#registration_password_repeat").val();

		var error = "";

		if (password.length < 35) {
			error = $.t("error_passphrase_length");
		} else if (password.length < 50 && (!password.match(/[a-zA-Z]/) || !password.match(/[0-9]/))) {
			error = $.t("error_passphrase_strength");
		} else if (password != repeat) {
			error = $.t("error_passphrase_match");
		}

		if (error) {
			$("#account_phrase_custom_panel").find(".callout").first().removeClass("callout-info").addClass("callout-danger").html(error);
		} else {
			$("#registration_password, #registration_password_repeat").val("");
			NRS.login(true,password);
		}
	});

	NRS.listAccounts = function() {
		var loginAccount = $('#login_account');
        loginAccount.empty();
		if (NRS.getStrItem("savedBenedAccounts") && NRS.getStrItem("savedBenedAccounts") != ""){
			$('#login_account_container').show();
			$('#login_account_container_other').hide();
			$('#login_account_container_other').removeClass('show');
			$('#remember_account_container').hide();
			var accounts = NRS.getStrItem("savedBenedAccounts").split(";");
			$.each(accounts, function(index, account) {
				if (account != ''){
					$('#login_account')
					.append($("<li></li>")
						.append($("<a></a>")
							.attr("href","#")
							.attr("style","display: inline-block;width: 360px;")
							.attr("onClick","NRS.login(false,'"+account+"')")
							.text(account))
						.append($('<button aria-hidden="true" data-dismiss="modal" class="close" type="button">×</button>')
							.attr("onClick","NRS.removeAccount('"+account+"')")
							.attr("style","margin-right:5px"))
					);
				}
			});
			var otherHTML = "<li><a href='#' style='display: inline-block;width: 380px;' ";
			otherHTML += "data-i18n='other'>Other</a></li>";
			var $otherHTML = $(otherHTML);
			$otherHTML.click(function() {
				$('#login_account_container').hide();
				$('#login_account_container_other').show();
				$('#remember_account_container').show();
			});
			$otherHTML.appendTo(loginAccount);
		}
		else{
			$('#login_account_container').hide();
			$('#login_account_container_other').show();
			$('#login_account_container_other').addClass('show');
			$('#remember_account_container').show();
		}
	};

	NRS.switchAccount = function(account) {
		// Reset security related state
		NRS.resetEncryptionState();
		NRS.setServerPassword(null);
		NRS.rememberPassword = false;
		$("#remember_password").prop("checked", false);

		// Reset other functional state
		$("#account_balance, #account_balance_sidebar, #account_nr_assets, #account_assets_balance, #account_currencies_balance, #account_nr_currencies, #account_purchase_count, #account_pending_sale_count, #account_completed_sale_count, #account_message_count, #account_alias_count").html("0");
		$("#id_search").find("input[name=q]").val("");
		NRS.resetAssetExchangeState();
		NRS.resetPollsState();
		NRS.resetMessagesState();
		NRS.forgingStatus = NRS.constants.UNKNOWN;
		NRS.isAccountForging = false;
		NRS.selectedContext = null;

		// Reset plugins state
		NRS.activePlugins = false;
		NRS.numRunningPlugins = 0;
		$.each(NRS.plugins, function(pluginId) {
			NRS.determinePluginLaunchStatus(pluginId);
		});

		// Return to the dashboard and notify the user
		NRS.goToPage("dashboard");
        NRS.login(false, account, function() {
            $.growl($.t("switched_to_account", { account: account }))
        }, true);
	};

	$(".loginButtons.password").on('click',function() {
		NRS.listAccounts();
		// $('#login_password').parent().hide();
		$('#login_account_container').hide();
		$('#login_password').closest('.passphrase').hide();
		$('#remember_password_container').hide();
		$('#remember_account_container').show();
		$(this).html('<input type="hidden" name="loginType" id="accountLogin" value="account" autocomplete="off" /><i class="fa fa-male"></i>');
		$(this).data( "login-type","account");
	});
	$(".loginButtons.account").on('click',function() {
		$('#login_account_container').hide();
		$('#login_account_container_other').hide();
		$('#login_password').closest('.passphrase').show();
		$('#remember_account_container').show();
		$('#remember_password_container').show();
		$(this).html('<input type="hidden" name="loginType" id="accountLogin" value="passwordLogin" autocomplete="off" /><i class="fa fa-key"></i>');
		$(this).data( "login-type","password");
	});
	$("#passphrase-sign-up").click(function(){
		$('#login_account_container').hide();
		$('#login_account_container_other').hide();
		$('#login_account_container_other').removeClass('show');
		$('#login_password').closest('.passphrase').show();
		$('#remember_account_container').show();
		$('#remember_password_container').show();
	});

	NRS.removeAccount = function(account) {
		var accounts = NRS.getStrItem("savedBenedAccounts").replace(account+';','');
		if (accounts == '') {
			NRS.removeItem('savedBenedAccounts');
		} else {
			NRS.setStrItem("savedBenedAccounts", accounts);
		}
		NRS.listAccounts();
	};

	// id can be either account id or passphrase
	NRS.login = function(isPassphraseLogin, id, callback, isAccountSwitch) {
		if (isPassphraseLogin){
			var loginCheckPasswordLength = $("#login_check_password_length");
			if (!id.length) {
				$.growl($.t("error_passphrase_required_login"), {
					"type": "danger",
					"offset": 10
				});
				return;
			} else if (!NRS.isTestNet && id.length < 12 && loginCheckPasswordLength.val() == 1) {
				loginCheckPasswordLength.val(0);
				var loginError = $("#login_error");
				loginError.find(".callout").html($.t("error_passphrase_login_length"));
				loginError.show();
				return;
			}

			$("#login_password, #registration_password, #registration_password_repeat").val("");
			loginCheckPasswordLength.val(1);
		}

		NRS.sendRequest("getBlockchainStatus", {}, function(response) {
			if (response.errorCode) {
				$.growl($.t("error_server_connect"), {
					"type": "danger",
					"offset": 10
				});

				return;
			}

			NRS.state = response;
			var accountRequest;
			var requestVariable;
			if (isPassphraseLogin) {
				accountRequest = "getAccountId";
				requestVariable = {secretPhrase: id};
			} else {
				accountRequest = "getAccount";
				requestVariable = {account: id};
			}

			//this is done locally..
			NRS.sendRequest(accountRequest, requestVariable, function(response, data) {
				if (!response.errorCode) {
					NRS.account = String(response.account).escapeHTML();
					NRS.accountRS = String(response.accountRS).escapeHTML();
					if (isPassphraseLogin) {
						NRS.publicKey = NRS.getPublicKey(converters.stringToHexString(id));
					} else {
						NRS.publicKey = String(response.publicKey).escapeHTML();
					}
				}
				if (!isPassphraseLogin && response.errorCode == 5) {
					NRS.account = String(response.account).escapeHTML();
					NRS.accountRS = String(response.accountRS).escapeHTML();
				}
				if (!NRS.account) {
					$.growl($.t("error_find_account_id", { accountRS: (data && data.account ? String(data.account).escapeHTML() : "") }), {
						"type": "danger",
						"offset": 10
					});
					return;
				} else if (!NRS.accountRS) {
					$.growl($.t("error_generate_account_id"), {
						"type": "danger",
						"offset": 10
					});
					return;
				}

				NRS.sendRequest("getAccountPublicKey", {
					"account": NRS.account
				}, function(response) {
					if (response && response.publicKey && response.publicKey != NRS.generatePublicKey(id) && isPassphraseLogin) {
						$.growl($.t("error_account_taken"), {
							"type": "danger",
							"offset": 10
						});
						return;
					}

					var rememberPassword = $("#remember_password");
					if (rememberPassword.is(":checked") && isPassphraseLogin) {
						NRS.rememberPassword = true;
						rememberPassword.prop("checked", false);
						NRS.setPassword(id);
						$(".secret_phrase, .show_secret_phrase").hide();
						$(".hide_secret_phrase").show();
					}
					NRS.disablePluginsDuringSession = $("#disable_all_plugins").is(":checked");
					$("#sidebar_account_id").html("bened<br/>"+String(NRS.accountRS).escapeHTML().substring(5));

					/* Left Panel Copy Account Info Buttons */
					$('.bened-main .left-panel .card .address a').html(String(NRS.accountRS).escapeHTML());
					$('#copy-address-block').attr('address',String(NRS.accountRS).escapeHTML());
					$('#copy-id-block').attr('data-id', String(NRS.account).escapeHTML());
					$('#copy-qr-block').attr('data-qr', escape(String(NRS.account) + ":" + String(NRS.publicKey)));
			
					
					NRS.sendRequest("getAccountBlocks+", {
						"account": NRS.account
					}, function(response) {
						if (response.blocks && response.blocks.length) {
							NRS.blocksPageLoaded(response.blocks);
						}
					});
					
					$('#dashboard-blocks-lastdate').html(NRS.formatTimestamp("<a href='#' class='block show_block_modal_action' data-blockid='"+ NRS.state.lastBlock +"'>" + NRS.blocks[0].timestamp + "</a>"));
        			$('#dashboard-blocks-lastheight, .progress-bar .height .value').html("<a href='#' class='block show_block_modal_action' data-blockid='"+ NRS.state.lastBlock +"'>" + NRS.lastBlockHeight + "</a>");

					//TODO remove THIS


					var balanceHtml = $("#sideBalance").html();
					var acclink = NRS.getAccountLink(NRS, "account", NRS.accountRS, "", false, "btn btn-default");
					$("#sideBalance").html(acclink);
					$("#account_link_in_sidebar").html(balanceHtml);

					if (NRS.lastBlockHeight == 0 && NRS.state.numberOfBlocks) {
						NRS.lastBlockHeight = NRS.state.numberOfBlocks - 1;
					}
					$("#sidebar_block_link").html(NRS.getBlockLink(NRS.lastBlockHeight));

					var passwordNotice = "";

					if (id.length < 35 && isPassphraseLogin) {
						passwordNotice = $.t("error_passphrase_length_secure");
					} else if (isPassphraseLogin && id.length < 50 && (!id.match(/[a-zA-Z]/) || !id.match(/[0-9]/))) {
						passwordNotice = $.t("error_passphrase_strength_secure");
					}

					if (passwordNotice) {
						$.growl("<strong>" + $.t("warning") + "</strong>: " + passwordNotice, {
							"type": "danger"
						});
					}
					NRS.getAccountInfo(true, function() {
						if (NRS.accountInfo.currentLeasingHeightFrom) {
							NRS.isLeased = (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo);
						} else {
							NRS.isLeased = false;
						}
						NRS.updateForgingTooltip($.t("forging_unknown_tooltip"));
						NRS.updateForgingStatus(isPassphraseLogin ? id : null);
						if (NRS.isLocalHost && isPassphraseLogin && false) {
							var forgingIndicator = $("#forging_indicator");
							NRS.sendRequest("non_startForging", {
								"non_secretPhrase": id
							}, function (response) {
								if ("deadline" in response) {
									forgingIndicator.addClass("forging");
									forgingIndicator.find("span").html($.t("forging")).attr("data-i18n", "forging");
									NRS.forgingStatus = NRS.constants.FORGING;
									NRS.updateForgingTooltip(NRS.getForgingTooltip);
								} else {
									forgingIndicator.removeClass("forging");
									forgingIndicator.find("span").html($.t("not_forging")).attr("data-i18n", "not_forging");
									NRS.forgingStatus = NRS.constants.NOT_FORGING;
									NRS.updateForgingTooltip(response.errorDescription);
								}
								forgingIndicator.show();
							});
						}
					}, isAccountSwitch);
					NRS.initSidebarMenu();
					NRS.unlock();

					if (NRS.isOutdated) {
						$.growl($.t("nrs_update_available"), {
							"type": "danger"
						});
					}

					if (!NRS.downloadingBlockchain) {
            try {
              NRS.checkIfOnAFork();
            } catch (err) {
              console.log("Error during fork check: "+err.message);
            }
					}
					NRS.logConsole("User Agent: " + String(navigator.userAgent));
					if (navigator.userAgent.indexOf('Safari') != -1 &&
						navigator.userAgent.indexOf('Chrome') == -1 &&
						navigator.userAgent.indexOf('JavaFX') == -1) {
						// Don't use account based DB in Safari due to a buggy indexedDB implementation (2015-02-24)
						NRS.createDatabase("NRS_USER_DB");
						$.growl($.t("nrs_safari_no_account_based_db"), {
							"type": "danger"
						});
					} else {
						NRS.createDatabase("NRS_USER_DB_" + String(NRS.account));
					}
					if (callback) {
						callback();
					}

					$.each(NRS.pages, function(key) {
						if(key in NRS.setup) {
							NRS.setup[key]();
						}
					});

					$(".sidebar .treeview").tree();
					$('#dashboard_link').find('a').addClass("ignore").click();



					var accounts;
					if ($("#remember_account").is(":checked") || NRS.newlyCreatedAccount) {
						var accountExists = 0;
						if (NRS.getStrItem("savedBenedAccounts")) {
							accounts = NRS.getStrItem("savedBenedAccounts").split(";");
							$.each(accounts, function(index, account) {
								if (account == NRS.accountRS) {
									accountExists = 1;
								}
							});
						}
						if (!accountExists){
							if (NRS.getStrItem("savedBenedAccounts") && NRS.getStrItem("savedBenedAccounts") != ""){
								accounts = NRS.getStrItem("savedBenedAccounts") + NRS.accountRS + ";";
								NRS.setStrItem("savedBenedAccounts", accounts);
							} else {
								NRS.setStrItem("savedBenedAccounts", NRS.accountRS + ";");
							}
						}
					}

					$("[data-i18n]").i18n();
				});
			});
		});
	};

	$("#logout_button_container").on("show.bs.dropdown", function() {
		if (NRS.forgingStatus != NRS.constants.FORGING) {
			$(this).find("[data-i18n='logout_stop_forging']").hide();
		}
	});

	NRS.initPluginWarning = function() {
		if (NRS.activePlugins) {
			var html = "";
			html += "<div style='font-size:13px;'>";
			html += "<div style='background-color:#e6e6e6;padding:12px;'>";
			html += "<span data-i18n='following_plugins_detected'>";
			html += "The following active plugins have been detected:</span>";
			html += "</div>";
			html += "<ul class='list-unstyled' style='padding:11px;border:1px solid #e0e0e0;margin-top:8px;'>";
			$.each(NRS.plugins, function(pluginId, pluginDict) {
				if (pluginDict["launch_status"] == NRS.constants.PL_PAUSED) {
					html += "<li style='font-weight:bold;'>" + pluginDict["manifest"]["name"] + "</li>";
				}
			});
			html += "</ul>";
			html += "</div>";

			$('#lockscreen_active_plugins_overview').popover({
				"html": true,
				"content": html,
				"trigger": "hover"
			});

			html = "";
			html += "<div style='font-size:13px;padding:5px;'>";
			html += "<p data-i18n='plugin_security_notice_full_access'>";
			html += "Plugins are not sandboxed or restricted in any way and have full accesss to your client system including your BENED passphrase.";
			html += "</p>";
			html += "<p data-i18n='plugin_security_notice_trusted_sources'>";
			html += "Make sure to only run plugins downloaded from trusted sources, otherwise ";
			html += "you can loose your BENED! In doubt don't run plugins with accounts ";
			html += "used to store larger amounts of BENED now or in the future.";
			html += "</p>";
			html += "</div>";

			$('#lockscreen_active_plugins_security').popover({
				"html": true,
				"content": html,
				"trigger": "hover"
			});

			$("#lockscreen_active_plugins_warning").show();
		} else {
			$("#lockscreen_active_plugins_warning").hide();
		}
	};

	NRS.showLockscreen = function() {
		NRS.listAccounts();
		if (localStorage.getItem("logged_in")) {
			NRS.showLoginScreen();
		} else {
			NRS.showWelcomeScreen();
		}

		$("#center").show();
		if (!NRS.isShowDummyCheckbox) {
			$("#dummyCheckbox").hide();
		}
	};

	NRS.unlock = function() {
		if (!localStorage.getItem("logged_in")) {
			localStorage.setItem("logged_in", true);
		}
		$("#lockscreen").hide();
		$("body, html").removeClass("lockscreen");
		$("#login_error").html("").hide();
		$(document.documentElement).scrollTop = 0;
	};

	NRS.logout = function(stopForging) {
		if (stopForging && NRS.forgingStatus == NRS.constants.FORGING) {
			var stopForgingModal = $("#stop_forging_modal");
            stopForgingModal.find(".show_logout").show();
			stopForgingModal.modal("show");
		} else {
			NRS.setDecryptionPassword("");
			NRS.setPassword("");
			//window.location.reload();
			window.location.href = window.location.pathname;
		}
	};

	$("#logout_clear_user_data_confirm_btn").click(function(e) {
		e.preventDefault();
		if (NRS.database) {
			//noinspection JSUnresolvedFunction
			indexedDB.deleteDatabase(NRS.database.name);
		}
		if (NRS.legacyDatabase) {
			//noinspection JSUnresolvedFunction
			indexedDB.deleteDatabase(NRS.legacyDatabase.name);
		}
		NRS.removeItem("logged_in");
		NRS.removeItem("savedBenedAccounts");
		NRS.removeItem("language");
		NRS.removeItem("themeChoice");
		NRS.removeItem("remember_passphrase");
		NRS.localStorageDrop("data");
		NRS.localStorageDrop("polls");
		NRS.localStorageDrop("contacts");
		NRS.localStorageDrop("assets");
		NRS.logout();
	});

	NRS.setPassword = function(password) {
		NRS.setEncryptionPassword(password);
		NRS.setServerPassword(password);
	};


	return NRS;
}(NRS || {}, jQuery));
