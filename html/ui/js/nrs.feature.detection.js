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
var NRS = (function (NRS) {

    var isDesktopApplication = navigator.userAgent.indexOf("JavaFX") >= 0;

    NRS.isIndexedDBSupported = function() {
        return window.indexedDB !== undefined;
    };

    NRS.isCoinExchangePageAvailable = function() {
        return !isDesktopApplication; // JavaFX does not support CORS required by ShapeShift
    };

    NRS.isExternalLinkVisible = function() {
        // When using JavaFX add a link to a web wallet except on Linux since on Ubuntu it sometimes hangs
        return isDesktopApplication && navigator.userAgent.indexOf("Linux") == -1;
    };


    NRS.compareStrings = function (string1, string2) {
      var areEqual = string1.trim().valueOf() === string2.trim().valueOf();
      // console.log(string1 + ' equals ' + string2 + '? ' + areEqual);
      return areEqual;
    }

    NRS.isPollGetState = function() {
        return !isDesktopApplication; // When using JavaFX do not poll the server
    };

    NRS.isExportContactsAvailable = function() {
        return !isDesktopApplication; // When using JavaFX you cannot export the contact list
    };

    NRS.isShowDummyCheckbox = function() {
        return isDesktopApplication && navigator.userAgent.indexOf("Linux") >= 0; // Correct rendering problem of checkboxes on Linux
    };

    return NRS;
}(NRS || {}, jQuery));
