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
var NRS = (function(NRS, $) {
	var INCOMING = "incoming";

NRS.jsondata = NRS.jsondata||{};

	NRS.pages.account_properties = function() {
        NRS.renderAccountProperties($("#account_properties_page_type").find(".active").data("type"));
	};

    NRS.renderAccountProperties = function(type) {
        NRS.hasMorePages = false;
        var view = NRS.simpleview.get('account_properties_section', {
            errorMessage: null,
            isLoading: true,
            isEmpty: false,
            properties: []
        });
        var params = {
            "firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
            "lastIndex": NRS.pageNumber * NRS.itemsPerPage
        };
        if (type == INCOMING) {
            params.recipient = NRS.account;
        } else {
            params.setter = NRS.account;
        }
        NRS.sendRequest("getAccountProperties+", params,
            function(response) {
                if (response.properties.length > NRS.itemsPerPage) {
                    NRS.hasMorePages = true;
                    response.properties.pop();
                }
                view.properties.length = 0;
                response.properties.forEach(
                    function (propertiesJson) {
                        view.properties.push( NRS.jsondata.properties(propertiesJson, type) );
                    }
                );
                view.render({
                    isLoading: false,
                    isEmpty: view.properties.length == 0,
                    header: type == INCOMING ? $.t("setter") : $.t("recipient")
                });
                NRS.pageLoaded();
            }
        );
    };

    NRS.normalizePropertyValue = function(value) {
        if (value == null) {
            return "";
        } else if (typeof value === "object") {
            return JSON.stringify(value);
        }
        return String(value).escapeHTML();
    };

    NRS.jsondata.properties = function (response, type) {
        var updateAction = "";
        var deleteAction = "";
        var recipientToken = "";
        if (response.recipientRS) {
            recipientToken = "data-recipient='" + response.recipientRS + "' ";
        }
        var value = NRS.normalizePropertyValue(response.value);
        if (type == INCOMING) {
            deleteAction = "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#delete_account_property_modal' " +
            "data-setter='" + response.setterRS + "' " +
            "data-recipient='" + NRS.accountRS + "' " +
            "data-property='" + String(response.property).escapeHTML() + "'>" + $.t("delete") + "</a>";

            if (response.setterRS == NRS.accountRS) {
                updateAction = "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#set_account_property_modal' " +
                "data-recipient='" + NRS.accountRS + "' " +
                "data-property='" + String(response.property).escapeHTML() + "' " +
                "data-value='" + value + "'>" + $.t("update") + "</a>";
            }
        } else {
            deleteAction = "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#delete_account_property_modal' " +
            "data-setter='" + NRS.accountRS + "' " +
            recipientToken +
            "data-property='" + String(response.property).escapeHTML() + "'>" + $.t("delete") + "</a>";

            updateAction = "<a href='#' class='btn btn-xs' data-toggle='modal' data-target='#set_account_property_modal' " +
            recipientToken +
            "data-property='" + String(response.property).escapeHTML() + "' " +
            "data-value='" + value + "'>" + $.t("update") + "</a>";
        }

        return {
            accountFormatted: type == INCOMING ? NRS.getAccountLink(response, "setter") : NRS.getAccountLink(response, "recipient"),
            property: String(response.property).escapeHTML(),
            value: value,
            action_update: updateAction,
            action_delete: deleteAction
        };
    };

	NRS.incoming.account_properties = function() {
		NRS.loadPage("account_properties");
	};

    $("#account_properties_page_type").find(".btn").click(function (e) {
        e.preventDefault();
        var propertiesTable = $("#account_properties_table");
        propertiesTable.find("tbody").empty();
        propertiesTable.parent().addClass("data-loading").removeClass("data-empty");
        NRS.renderAccountProperties($(this).data("type"));
    });

    $("#set_account_property_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var recipient = $invoker.data("recipient");
        var recipientInput = $("#set_account_property_recipient");
        var recipientButton = $(".recipient_selector").find(".btn");
        if (recipient) {
            recipientInput.val(recipient);
            recipientInput.prop('readonly', true);
            recipientButton.prop('disabled', true);
        } else {
            recipientInput.prop('readonly', false);
            recipientButton.prop('disabled', false);
        }
        var property = $invoker.data("property");
        var propertyInput = $("#set_account_property_property");
        if (property) {
            propertyInput.val(property);
            propertyInput.prop('readonly', true);
        } else {
            propertyInput.prop('readonly', false);
        }
        $("#set_account_property_value").val(NRS.normalizePropertyValue($invoker.data("value")));
    });

    $("#delete_account_property_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var setter = $invoker.data("setter");
        if (setter) {
            var setterInput = $("#delete_account_property_setter");
            setterInput.val(setter);
        }
        var recipient = $invoker.data("recipient");
        if (recipient) {
            var recipientInput = $("#delete_account_property_recipient");
            recipientInput.val(recipient);
        }
        var property = $invoker.data("property");
        if (property) {
            var propertyInput = $("#delete_account_property_property");
            propertyInput.val(property);
        }
    });

	return NRS;
}(NRS || {}, jQuery));