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

	NRS.connectPeer = function(peer) {
		NRS.sendRequest("addPeer", {"peer": peer}, function(response) {
			if (response.errorCode || response.error || response.state != 1) {
				$.growl($.t("failed_connect_peer"), {
					"type": "danger"
				});
			} else {
				$.growl($.t("success_connect_peer"), {
					"type": "success"
				});
			}
			NRS.loadPage("peers");
		});
	};

    function getPeerServicesLabel(services) {
        var servicesText = "";
        for (var i=0; i<services.length; i++) {
            var shortcut = services[i].substring(0, 1) + services[i].substring(services[i].length - 1);
            servicesText += "<a data-toggle='tooltip' data-placement='top' data-container='body' title='" + $.t(services[i].toLowerCase() + "_service") + "'>" + shortcut + "</a>";
            if (i < services.length - 1) {
                servicesText += "<span> , </span>";
            }
        }
        return servicesText;
    }

    NRS.pages.peers = function() {
        NRS.sendRequest("getPeers+", {
			"active": "true",
			"includePeerInfo": "true"
		}, function(response) {
			if (response.peers && response.peers.length) {
				var rows = "";
				var uploaded = 0;
				var downloaded = 0;
				var connected = 0;
				var upToDate = 0;
				var activePeers = 0;

				for (var i = 0; i < response.peers.length; i++) {
					var peer = response.peers[i];

					if (!peer) {
						continue;
					}

					activePeers++;
					downloaded += peer.downloadedVolume;
					uploaded += peer.uploadedVolume;
					if (peer.state == 1) {
						connected++;
					}
/* original nrs.nrsversion.versionNr ne ischetca tk net konekta r nxt saytu     var versionToCompare = (!NRS.isTestNet ? NRS.nrsVersion.versionNr : NRS.state.version);
*/

					var versionToCompare = (NRS.state.version);

					if (NRS.versionCompare(peer.version, versionToCompare) >= 0) {
						upToDate++;
					}

					rows += "<tr>";
					rows += "<td>";
					rows += (peer.state == 1 ? "<i class='fa fa-check-circle' style='color:#5cb85c' title='Connected'></i>" : "<i class='fa fa-times-circle' style='color:#f0ad4e' title='Disconnected'></i>");
					rows += "&nbsp;&nbsp;" + (peer.announcedAddress ? String(peer.announcedAddress).escapeHTML() : "No name") + "</td>";
					rows += "<td" + (peer.weight > 0 ? " style='font-weight:bold'" : "") + ">" + NRS.formatWeight(peer.weight) + "</td>";
					rows += "<td>" + NRS.formatVolume(peer.downloadedVolume) + "</td>";
					rows += "<td>" + NRS.formatVolume(peer.uploadedVolume) + "</td>";
					rows += "<td><span class='label label-" + (NRS.versionCompare(peer.version, versionToCompare) >= 0 ? "success" : "danger") + "'>";
					rows += (peer.application && peer.version ? String(peer.application).escapeHTML() + " " + String(peer.version).escapeHTML() : "?") + "</label></td>";
					rows += "<td>" + (peer.platform ? String(peer.platform).escapeHTML() : "?") + "</td>";
					rows += "<td>" + getPeerServicesLabel(peer.services) + "</td>";
					rows += "<td style='text-align:right;'>";
					rows += "<a class='btn btn-xs btn-default' href='#' ";
					if (NRS.needsAdminPassword) {
						rows += "data-toggle='modal' style='border-radius:5px 5px 0 0;border-bottom:1px solid transparent;display:block;' data-target='#connect_peer_modal' data-peer='" + String(peer.announcedAddress).escapeHTML() + "'>";
					} else {
						rows += " style='border-radius:5px 5px 0 0;border-bottom:1px solid transparent;display:block;' onClick='NRS.connectPeer(\"" + String(peer.announcedAddress).escapeHTML() + "\");'>";
					}
					rows += $.t("connect") + "</a>";
					rows += "<a class='btn btn-xs btn-default' href='#' ";
					rows += "data-toggle='modal' style='border-radius:0 0 5px 5px;display:block;border-top:1px solid transparent;' data-target='#blacklist_peer_modal' data-peer='" + String(peer.announcedAddress).escapeHTML() + "'>" + $.t("blacklist") + "</a>";
					rows += "</td>";
					rows += "</tr>";
				}

				$("#peers_uploaded_volume").html(NRS.formatVolume(uploaded)).removeClass("loading_dots");
				$("#peers_downloaded_volume").html(NRS.formatVolume(downloaded)).removeClass("loading_dots");
				$("#peers_connected").html(connected).removeClass("loading_dots");
				$("#peers_up_to_date").html(upToDate + '/' + activePeers).removeClass("loading_dots");

				NRS.dataLoaded(rows);
			} else {
				$("#peers_uploaded_volume, #peers_downloaded_volume, #peers_connected, #peers_up_to_date").html("0").removeClass("loading_dots");
				NRS.dataLoaded();
			}
		});
	};

	NRS.incoming.peers = function() {
		NRS.loadPage("peers");
	};

	NRS.forms.addPeerComplete = function(response) {
		var message = "success_add_peer";
		var growlType = "success";
		if (response.state == 1) {
			message = "success_connect_peer";
		} else if (!response.isNewlyAdded) {
			message = "peer_already_added";
			growlType = "danger";
		}

		$.growl($.t(message), {
			"type": growlType
		});
		NRS.loadPage("peers");
	};

	NRS.forms.blacklistPeerComplete = function(response) {
		var message;
		var type;
		if (response.errorCode) {
			message = response.errorDescription.escapeHTML();
			type = "danger";
		} else {
			message = $.t("success_blacklist_peer");
			type = "success";
		}
		$.growl(message, {
			"type": type
		});
		NRS.loadPage("peers");
	};

	$("#add_peer_modal").on("show.bs.modal", function() {
		showAdminPassword("add");
	});

	$("#connect_peer_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);
		$("#connect_peer_address").html($invoker.data("peer"));
		$("#connect_peer_field_id").val($invoker.data("peer"));
		showAdminPassword("connect");
	});

	$("#blacklist_peer_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);
		$("#blacklist_peer_address").html($invoker.data("peer"));
		$("#blacklist_peer_field_id").val($invoker.data("peer"));
		showAdminPassword("blacklist");
	});

	function showAdminPassword(action) {
		if (!NRS.needsAdminPassword) {
			$("#" + action + "_peer_admin_password_wrapper").hide();
		} else {
			if (NRS.getAdminPassword() != "") {
				$("#" + action + "_peer_admin_password").val(NRS.getAdminPassword());
			}
		}
	}

	return NRS;
}(NRS || {}, jQuery));
