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

import bened.BNDException;
import bened.Transaction;
import bened.util.Convert;
import bened.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    static final ParseTransaction instance = new ParseTransaction();

    private ParseTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionJSON", "transactionBytes", "prunableAttachmentJSON");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws BNDException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));

        Transaction transaction = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON).build();
        JSONObject response = JSONData.unconfirmedTransaction(transaction);
        try {
            transaction.validate();
        } catch (BNDException.ValidationException|RuntimeException e) {
            Logger.logDebugMessage(e.getMessage(), e);
            response.put("validate", false);
            JSONData.putException(response, e, "Invalid transaction");
        }
        response.put("verify", transaction.verifySignature());
        return response;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
