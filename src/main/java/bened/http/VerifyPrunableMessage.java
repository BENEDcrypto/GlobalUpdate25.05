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

import bened.Appendix;
import bened.Bened;
import bened.BNDException;
import bened.Transaction;
import bened.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static bened.http.JSONResponses.EITHER_MESSAGE_ENCRYPTED_MESSAGE;
import static bened.http.JSONResponses.MISSING_MESSAGE_ENCRYPTED_MESSAGE;
import static bened.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class VerifyPrunableMessage extends APIServlet.APIRequestHandler {

    static final VerifyPrunableMessage instance = new VerifyPrunableMessage();

    private static final JSONStreamAware NO_SUCH_PLAIN_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no plain message attachment");
        NO_SUCH_PLAIN_MESSAGE = JSON.prepare(response);
    }

    private static final JSONStreamAware NO_SUCH_ENCRYPTED_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no encrypted message attachment");
        NO_SUCH_ENCRYPTED_MESSAGE = JSON.prepare(response);
    }

    private VerifyPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction",
                "message", "messageIsText",
                "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "compressMessageToEncrypt");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws BNDException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = Bened.getBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        Appendix.PrunablePlainMessage plainMessage = (Appendix.PrunablePlainMessage) ParameterParser.getPlainMessage(req, true);
        Appendix.PrunableEncryptedMessage encryptedMessage = (Appendix.PrunableEncryptedMessage) ParameterParser.getEncryptedMessage(req, null, true);

        if (plainMessage == null && encryptedMessage == null) {
            return MISSING_MESSAGE_ENCRYPTED_MESSAGE;
        }
        if (plainMessage != null && encryptedMessage != null) {
            return EITHER_MESSAGE_ENCRYPTED_MESSAGE;
        }

        if (plainMessage != null) {
            Appendix.PrunablePlainMessage myPlainMessage = transaction.getPrunablePlainMessage();
            if (myPlainMessage == null) {
                return NO_SUCH_PLAIN_MESSAGE;
            }
            if (!Arrays.equals(myPlainMessage.getHash(), plainMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myPlainMessage.getJSONObject();
            response.put("verify", true);
            return response;
        } else if (encryptedMessage != null) {
            Appendix.PrunableEncryptedMessage myEncryptedMessage = transaction.getPrunableEncryptedMessage();
            if (myEncryptedMessage == null) {
                return NO_SUCH_ENCRYPTED_MESSAGE;
            }
            if (!Arrays.equals(myEncryptedMessage.getHash(), encryptedMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myEncryptedMessage.getJSONObject();
            response.put("verify", true);
            return response;
        }

        return JSON.emptyJSON;
    }

}
