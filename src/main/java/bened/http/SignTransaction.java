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
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SignTransaction extends APIServlet.APIRequestHandler {

    static final SignTransaction instance = new SignTransaction();

    private SignTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionJSON", "unsignedTransactionBytes", "prunableAttachmentJSON", "secretPhrase", "validate");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionJSON = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));

        Transaction.Builder builder = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);

        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        boolean validate = !"false".equalsIgnoreCase(req.getParameter("validate"));

        JSONObject response = new JSONObject();
        try {
            Transaction transaction = builder.build(secretPhrase);
            JSONObject signedTransactionJSON = JSONData.unconfirmedTransaction(transaction);
            if (validate) {
                transaction.validate();
                response.put("verify", transaction.verifySignature());
            }
            response.put("transactionJSON", signedTransactionJSON);
            response.put("fullHash", signedTransactionJSON.get("fullHash"));
            response.put("signatureHash", signedTransactionJSON.get("signatureHash"));
            response.put("transaction", transaction.getStringId());
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            JSONData.putPrunableAttachment(response, transaction);
        } catch (BNDException.ValidationException|RuntimeException e) {
            JSONData.putException(response, e, "Incorrect unsigned transaction json or bytes");
        }
        return response;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
