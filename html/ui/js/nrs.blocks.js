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
	NRS.blocksPageType = null;
	NRS.tempBlocks = [];
	var trackBlockchain = false;
	NRS.averageBlockGenerationTime = 60;

	NRS.getBlock = function(blockID, callback, pageRequest) {
		NRS.sendRequest("getBlock" + (pageRequest ? "+" : ""), {
			"block": blockID
		}, function(response) {
			if (response.errorCode && response.errorCode == -1 || !NRS.constants || NRS.constants.EPOCH_BEGINNING == 0) {
				setTimeout(function (){ NRS.getBlock(blockID, callback, pageRequest); }, 2500);
			} else {
				if (callback) {
					response.block = blockID;
					callback(response);
				}
			}
		}, true);
	};

	NRS.handleInitialBlocks = function(response) {
		NRS.blocks.push(response);
		if (NRS.blocks.length < 10 && response.previousBlock) {
			NRS.getBlock(response.previousBlock, NRS.handleInitialBlocks);
		} else {
			NRS.checkBlockHeight(NRS.blocks[0].height);
			if (NRS.state) {
				//if no new blocks in 6 hours, show blockchain download progress..
				var timeDiff = NRS.state.time - NRS.blocks[0].timestamp;
				if (timeDiff > 60 * 60 * 18) {
					if (timeDiff > 60 * 60 * 24 * 14) {
						NRS.setStateInterval(30);
					} else if (timeDiff > 60 * 60 * 24 * 7) {
						//second to last week
						NRS.setStateInterval(15);
					} else {
						//last week
						NRS.setStateInterval(10);
					}
					NRS.downloadingBlockchain = true;

					$("#nrs_update_explanation_wait").attr("style", "display: none !important");

					$("#show_console").hide();
					NRS.updateBlockchainDownloadProgress();
				} else {
					//continue with faster state intervals if we still haven't reached current block from within 1 hour
					if (timeDiff < 60 * 60) {
						NRS.setStateInterval(30);
						trackBlockchain = false;
					} else {
						NRS.setStateInterval(10);
						trackBlockchain = true;
					}
				}
			}
			$("#nrs_current_block_time").empty().append(NRS.formatTimestamp(NRS.blocks[0].timestamp).replace(' ', '</br>'));
			$(".nrs_current_block").empty().append('Last block: '+String(NRS.blocks[0].height).escapeHTML());
		}
	};

	NRS.handleNewBlocks = function(response) {
		if (NRS.downloadingBlockchain) {
			//new round started...
			if (NRS.tempBlocks.length == 0 && NRS.state.lastBlock != response.block) {
				return;
			}
		}

		//we have all blocks
		if (response.height - 1 == NRS.lastBlockHeight || NRS.tempBlocks.length == 99) {
			var newBlocks = [];

			//there was only 1 new block (response)
			if (NRS.tempBlocks.length == 0) {
				//remove oldest block, add newest block
				NRS.blocks.unshift(response);
				newBlocks.push(response);
			} else {
				NRS.tempBlocks.push(response);
				//remove oldest blocks, add newest blocks
				[].unshift.apply(NRS.blocks, NRS.tempBlocks);
				newBlocks = NRS.tempBlocks;
				NRS.tempBlocks = [];
			}

			if (NRS.blocks.length > 100) {
				NRS.blocks = NRS.blocks.slice(0, 100);
			}
			NRS.checkBlockHeight(NRS.blocks[0].height);
			NRS.incoming.updateDashboardBlocks(newBlocks);
		} else {
			NRS.tempBlocks.push(response);
			NRS.getBlock(response.previousBlock, NRS.handleNewBlocks);
		}
	};

	NRS.checkBlockHeight = function(blockHeight) {
		if (blockHeight) {
			NRS.lastBlockHeight = blockHeight;
		}
	};

	//we always update the dashboard page..
	NRS.incoming.updateDashboardBlocks = function(newBlocks) {
        var timeDiff;
		if (NRS.downloadingBlockchain) {
			if (NRS.state) {
				timeDiff = NRS.state.time - NRS.blocks[0].timestamp;
				if (timeDiff < 60 * 60 * 18) {
					if (timeDiff < 60 * 60) {
						NRS.setStateInterval(30);
					} else {
						NRS.setStateInterval(10);
						trackBlockchain = true;
					}
					NRS.downloadingBlockchain = false;
					$("#dashboard_message").hide();
					$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").hide();
					$("#nrs_update_explanation_wait").removeAttr("style");
					if (NRS.settings["console_log"]) {
						$("#show_console").show();
					}
					//todo: update the dashboard blocks!
					$.growl($.t("success_blockchain_up_to_date"), {
						"type": "success"
					});
					NRS.checkAliasVersions();
					NRS.checkIfOnAFork();
				} else {
					if (timeDiff > 60 * 60 * 24 * 14) {
						NRS.setStateInterval(30);
					} else if (timeDiff > 60 * 60 * 24 * 7) {
						//second to last week
						NRS.setStateInterval(15);
					} else {
						//last week
						NRS.setStateInterval(10);
					}

					NRS.updateBlockchainDownloadProgress();
				}
			}
		} else if (trackBlockchain) {
			//continue with faster state intervals if we still haven't reached current block from within 1 hour
            timeDiff = NRS.state.time - NRS.blocks[0].timestamp;
			if (timeDiff < 60 * 60) {
				NRS.setStateInterval(30);
				trackBlockchain = false;
			} else {
				NRS.setStateInterval(10);
			}
		}

		block = NRS.blocks[0];
		$("#nrs_current_block_time").empty().append(NRS.formatTimestamp(block.timestamp).replace(' ', '</br>'));
		$(".nrs_current_block").empty().append('Last block: '+String(block.height).escapeHTML());

		//update number of confirmations... perhaps we should also update it in tne NRS.transactions array
		$("#dashboard_table").find("tr.confirmed td.confirmations").each(function() {
			if ($(this).data("incoming")) {
				$(this).removeData("incoming");
				return true;
			}
			var confirmations = parseInt($(this).data("confirmations"), 10);
			var nrConfirmations = confirmations + newBlocks.length;
			if (confirmations <= 10) {
				$(this).data("confirmations", nrConfirmations);
				$(this).attr("data-content", $.t("x_confirmations", {
					"x": NRS.formatAmount(nrConfirmations, false, true)
				}));

				if (nrConfirmations > 10) {
					nrConfirmations = '10+';
				}
				$(this).html(nrConfirmations);
			} else {
				$(this).attr("data-content", $.t("x_confirmations", {
					"x": NRS.formatAmount(nrConfirmations, false, true)
				}));
			}
		});
		var blockLink = $("#sidebar_block_link");
		if (blockLink.length > 0) {
			blockLink.html(NRS.getBlockLink(NRS.lastBlockHeight));
		}
	};

	NRS.pages.blocks = function() {
		if (NRS.blocksPageType == "forged_blocks") {
			$("#forged_fees_total_box, #forged_blocks_total_box").show();
			$("#blocks_transactions_per_hour_box, #blocks_generation_time_box").hide();

			NRS.sendRequest("getAccountBlocks+", {
				"account": NRS.account,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				if (response.blocks && response.blocks.length) {
					if (response.blocks.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.blocks.pop();
					}
					NRS.blocksPageLoaded(response.blocks);
				} else {
					NRS.blocksPageLoaded([]);
				}
			});
		} else {
			$("#forged_fees_total_box, #forged_blocks_total_box").hide();
			$("#blocks_transactions_per_hour_box, #blocks_generation_time_box").show();

			NRS.sendRequest("getBlocks+", {
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				if (response.blocks && response.blocks.length) {
					if (response.blocks.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.blocks.pop();
					}
					NRS.blocksPageLoaded(response.blocks);
				} else {
					NRS.blocksPageLoaded([]);
				}
			});
		}
	};

	NRS.incoming.blocks = function() {
		NRS.loadPage("blocks");
	};

	NRS.blocksPageLoaded = function(blocks) {
		var rows = "";
		var itemsDash = "";
		var totalAmount = new BigInteger("0");
		var totalFees = new BigInteger("0");
		var totalTransactions = 0;

		for (var i = 0; i < blocks.length; i++) {
			var block = blocks[i];
			totalAmount = totalAmount.add(new BigInteger(block.totalAmountNQT));
			totalFees = totalFees.add(new BigInteger(block.totalFeeNQT));
			totalTransactions += block.numberOfTransactions;
      if (NRS.compactTables == 1) {
        rows += "<tr>" +
                  "<td><a href='#' data-block='" + String(block.height).escapeHTML() + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block show_block_modal_action'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td>" +
                  "<td>" + NRS.formatTimestamp(block.timestamp) + "</td>" +
                  "<td>" + NRS.formatAmount(block.totalAmountNQT) + "</td>" +
                  "<td>" + NRS.formatAmount(block.totalFeeNQT) + "</td>" +
                  "<td>" + NRS.formatAmount(block.numberOfTransactions) + "</td>" +
                  "<td>" + NRS.getAccountLink(block, "generator") + "</td>" +
              "</tr>";

		itemsDash += '<div class="item"><div class="blocks d-flex justify-content-between align-items-center">';
		itemsDash += '<div class="height"><a href="#" data-block="' + String(block.height).escapeHTML() + '" data-blockid="'  + String(block.block).escapeHTML() +  '" class="block show_block_modal_action"'+ (block.numberOfTransactions > 0 ? ' style="font-weight:bold"' : "") + ">" + String(block.height).escapeHTML() + '</a></div>';
		itemsDash += '<div class="date">' + NRS.formatTimestamp(block.timestamp) + '</div>';
		itemsDash += '<div class="amount">' + NRS.formatAmount(block.totalAmountNQT) + '</div>';
		itemsDash += '<div class="fee">' + NRS.formatAmount(block.totalFeeNQT) + '</div>';
		itemsDash += '<div class="tx">' + NRS.formatAmount(block.numberOfTransactions) + '</div>';
		itemsDash += '<div class="generator">' + NRS.getAccountLink(block, "generator") + '</div>';
		itemsDash += '</div></div>';
      } else {
        rows += "<tr>" +
                  "<td><a href='#' data-block='" + String(block.height).escapeHTML() + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block show_block_modal_action'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td>" +
                  "<td>" + NRS.formatTimestamp(block.timestamp) + "</td>" +
                  "<td>" + NRS.formatAmount(block.totalAmountNQT) + "</td>" +
                  "<td>" + NRS.formatAmount(block.totalFeeNQT) + "</td>" +
                  "<td>" + NRS.formatAmount(block.numberOfTransactions) + "</td>" +
                  "<td>" + NRS.getAccountLink(block, "generator") + "</td>" +
                  "<td>" + NRS.formatVolume(block.payloadLength) + "</td>" +
  				"<td>" + NRS.baseTargetPercent(block) + "</td>" +
              "</tr>";

		itemsDash += '<div class="item"><div class="blocks d-flex justify-content-between align-items-center">';
		itemsDash += '<div class="height"><a href="#" data-block="' + String(block.height).escapeHTML() + '" data-blockid="'  + String(block.block).escapeHTML() +  '" class="block show_block_modal_action"'+ (block.numberOfTransactions > 0 ? ' style="font-weight:bold"' : "") + ">" + String(block.height).escapeHTML() + '</a></div>';
		itemsDash += '<div class="date">' + NRS.formatTimestamp(block.timestamp) + '</div>';
		itemsDash += '<div class="amount">' + NRS.formatAmount(block.totalAmountNQT) + '</div>';
		itemsDash += '<div class="fee">' + NRS.formatAmount(block.totalFeeNQT) + '</div>';
		itemsDash += '<div class="tx">' + NRS.formatAmount(block.numberOfTransactions) + '</div>';
		itemsDash += '<div class="generator">' + NRS.getAccountLink(block, "generator") + '</div>';
		itemsDash += '</div></div>';
      }

		}
    var blocksTable = $("#blocks_table");
    if (NRS.compactTables == 1) {
      blocksTable.find("tbody tr, thead tr").children(":nth-child(7)").hide();
    } else {
      blocksTable.find("tbody tr, thead tr").children(":nth-child(7)").show();
    }

        var blocksAverageAmount = $("#blocks_average_amount");
        if (NRS.blocksPageType == "forged_blocks") {
			NRS.sendRequest("getAccountBlockCount+", {
				"account": NRS.account
			}, function(response) {
				if (response.numberOfBlocks && response.numberOfBlocks > 0) {
					$("#forged_blocks_total").html(response.numberOfBlocks).removeClass("loading_dots");
                    var avgFee = new Big(NRS.accountInfo.forgedBalanceNQT).div(response.numberOfBlocks).div(new Big("1000000")).toFixed(2);
                    $("#blocks_average_fee").html(NRS.formatStyledAmount(NRS.convertToNQT(avgFee))).removeClass("loading_dots");
				} else {
					$("#forged_blocks_total").html(0).removeClass("loading_dots");
					$("#blocks_average_fee").html(0).removeClass("loading_dots");
				}
			});
			$("#forged_fees_total").html(NRS.formatStyledAmount(NRS.accountInfo.forgedBalanceNQT)).removeClass("loading_dots");
			blocksAverageAmount.removeClass("loading_dots");
			blocksAverageAmount.parent().parent().css('visibility', 'hidden');
			$("#blocks_page").find(".ion-stats-bars").parent().css('visibility', 'hidden');
		} else {
			var time;
            if (blocks.length) {
				var startingTime = blocks[blocks.length - 1].timestamp;
				var endingTime = blocks[0].timestamp;
				time = endingTime - startingTime;
			} else {
				time = 0;
			}
            var averageFee = 0;
            var averageAmount = 0;
			if (blocks.length) {
				averageFee = new Big(totalFees.toString()).div(new Big("1000000")).div(new Big(String(blocks.length))).toFixed(2);
				averageAmount = new Big(totalAmount.toString()).div(new Big("1000000")).div(new Big(String(blocks.length))).toFixed(2);
			}
			averageFee = NRS.convertToNQT(averageFee);
			averageAmount = NRS.convertToNQT(averageAmount);
			if (time == 0) {
				$("#blocks_transactions_per_hour").html("0").removeClass("loading_dots");
				$("#network-stats-perhour").html("0");
			} else {
				$("#blocks_transactions_per_hour").html(Math.round(totalTransactions / (time / 60) * 60)).removeClass("loading_dots");
				$("#network-stats-perhour").html(Math.round(totalTransactions / (time / 60) * 60));
			}
			$("#blocks_average_generation_time").html(Math.round(time / NRS.itemsPerPage) + "s").removeClass("loading_dots");
			$("#blocks_average_fee").html(NRS.formatStyledAmount(averageFee)).removeClass("loading_dots");
			blocksAverageAmount.parent().parent().css('visibility', 'visible');
			$("#blocks_page").find(".ion-stats-bars").parent().css('visibility', 'visible');
			blocksAverageAmount.html(NRS.formatStyledAmount(averageAmount)).removeClass("loading_dots");
			
			/* Show on Dashboard */
			$('#network-stats-amount').html(NRS.formatStyledAmount(averageAmount));
			$("#network-stats-fee").html(NRS.formatStyledAmount(averageFee));

			$('#network-stats-gentime').html(Math.round(time / NRS.itemsPerPage) + "s");
////!!!!
		}
		NRS.dataLoaded(rows);
		NRS.dataDashLoadedBlocks(itemsDash);
	};

	$("#blocks_page_type").find(".btn").click(function(e) {
		e.preventDefault();
		NRS.blocksPageType = $(this).data("type");
		$("#blocks_average_amount, #blocks_average_fee, #blocks_transactions_per_hour, #blocks_average_generation_time, #forged_blocks_total, #forged_fees_total").html("<span>.</span><span>.</span><span>.</span></span>").addClass("loading_dots");
    var blocksTable = $("#blocks_table");
    blocksTable.find("tbody").empty();
		blocksTable.parent().addClass("data-loading").removeClass("data-empty");
		NRS.loadPage("blocks");
	});

	$("#goto_forged_blocks").click(function(e) {
		e.preventDefault();
		$("#blocks_page_type").find(".btn:last").button("toggle");
		NRS.blocksPageType = "forged_blocks";
		NRS.goToPage("blocks");
	});

	return NRS;
}(NRS || {}, jQuery));
