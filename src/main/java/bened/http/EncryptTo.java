/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package bened.http;

import bened.Account;
import bened.BNDException;
import bened.crypto.EncryptedData;
import bened.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static bened.http.JSONResponses.INCORRECT_MESSAGE_TO_ENCRYPT;
import static bened.http.JSONResponses.INCORRECT_RECIPIENT;
import static bened.http.JSONResponses.MISSING_MESSAGE_TO_ENCRYPT;

public final class EncryptTo extends APIServlet.APIRequestHandler {

    static final EncryptTo instance = new EncryptTo();

    private EncryptTo() {
        super(new APITag[] {APITag.MESSAGES}, "recipient", "messageToEncrypt", "messageToEncryptIsText", "compressMessageToEncrypt", "secretPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws BNDException {

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        byte[] recipientPublicKey = Account.getPublicKey(recipientId);
        if (recipientPublicKey == null) {
            return INCORRECT_RECIPIENT;
        }
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText"));
        boolean compress = !"false".equalsIgnoreCase(req.getParameter("compressMessageToEncrypt"));
        String plainMessage = Convert.emptyToNull(req.getParameter("messageToEncrypt"));
        if (plainMessage == null) {
            return MISSING_MESSAGE_TO_ENCRYPT;
        }
        byte[] plainMessageBytes;
        try {
            plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
        } catch (RuntimeException e) {
            return INCORRECT_MESSAGE_TO_ENCRYPT;
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        EncryptedData encryptedData = Account.encryptTo(recipientPublicKey, plainMessageBytes, secretPhrase, compress);
        return JSONData.encryptedData(encryptedData);

    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
