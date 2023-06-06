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
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
    var accountDetailsModal = $("#account_details_modal");
	// var copyAddress = $('#copy-address-block').attr('address');
	// // $('#copy-address-block').html(
	// 	NRS.createCopyButton($('#copy-address-block'));
	// // ));
    // // $('#copy-address-block').html('<button onclick="NRS.copyTextToClipboard('+ copyAddress +', this, false)">Copy Address</button>');

    accountDetailsModal.on("show.bs.modal", function(e) {
        var qrData = String(NRS.account) + ":" + String(NRS.publicKey);
        NRS.sendRequestQRCode("#account_details_modal_qr_code", qrData, 200, 200);
		    $("#account_details_modal_balance").show();

        var accountBalanceWarning = $("#account_balance_warning");
        if (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode != 5) {
			$("#account_balance_table").hide();
			accountBalanceWarning.html(String(NRS.accountInfo.errorDescription).escapeHTML()).show();
		} else {
			accountBalanceWarning.hide();
            var accountBalancePublicKey = $("#account_balance_public_key");
            if (NRS.accountInfo.errorCode && NRS.accountInfo.errorCode == 5) {
				$("#account_balance_balance, #account_balance_unconfirmed_balance, #account_balance_effective_balance, #account_balance_guaranteed_balance, #account_balance_forged_balance").html("0 BENED");
				accountBalancePublicKey.html(String(NRS.publicKey).escapeHTML() + NRS.createCopyButton(String(NRS.publicKey).escapeHTML()));
				$("#account_balance_account_rs").html(NRS.getAccountLink(NRS, "account", undefined, undefined, true));
				$("#account_balance_account").html(String(NRS.account).escapeHTML() + NRS.createCopyButton(String(NRS.account).escapeHTML()));
			} else {
				$("#account_balance_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.balanceNQT)) + " BENED");
				$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.unconfirmedBalanceNQT)) + " BENED");
				$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountInfo.effectiveBalanceBened) + " BENED");
				$("#account_balance_guaranteed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.guaranteedBalanceNQT)) + " BENED");
				$("#account_balance_forged_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.forgedBalanceNQT)) + " BENED");

				accountBalancePublicKey.html(String(NRS.accountInfo.publicKey).escapeHTML() + NRS.createCopyButton(String(NRS.accountInfo.publicKey).escapeHTML()));
				$("#account_balance_account_rs").html(NRS.getAccountLink(NRS.accountInfo, "account", undefined, undefined, true) + NRS.createCopyButton(NRS.accountInfo.accountRS));
				$("#account_balance_account").html(String(NRS.account).escapeHTML() + NRS.createCopyButton(String(NRS.account).escapeHTML()));

				if (!NRS.accountInfo.publicKey) {
					accountBalancePublicKey.html("/");
                    var warning = NRS.publicKey != 'undefined' ? $.t("public_key_not_announced_warning", { "public_key": NRS.publicKey }) : $.t("no_public_key_warning");
					accountBalanceWarning.html(warning + " " + $.t("public_key_actions")).show();
				}
			}
		}

		var $invoker = $(e.relatedTarget);
		var tab = $invoker.data("detailstab");
		if (tab) {
			_showTab(tab)
		}
	});

	function _showTab(tab){
		var tabListItem = $("#account_details_modal li[data-tab=" + tab + "]");
		tabListItem.siblings().removeClass("active");
		tabListItem.addClass("active");

		$(".account_details_modal_content").hide();

		var content = $("#account_details_modal_" + tab);

		content.show();
	}

	accountDetailsModal.find("ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		_showTab(tab);
	});

	accountDetailsModal.on("hidden.bs.modal", function() {
		$(this).find(".account_details_modal_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$("#account_details_balance_nav").addClass("active");
		$("#account_details_modal_qr_code").empty();
	});

	return NRS;
}(NRS || {}, jQuery));
