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
import bened.crypto.Crypto;
import bened.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;

import static bened.http.JSONResponses.MISSING_SIGNATURE_HASH;

public final class CalculateFullHash extends APIServlet.APIRequestHandler {

    static final CalculateFullHash instance = new CalculateFullHash();

    private CalculateFullHash() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionBytes", "unsignedTransactionJSON", "signatureHash");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String unsignedBytesString = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String signatureHashString = Convert.emptyToNull(req.getParameter("signatureHash"));
        String unsignedTransactionJSONString = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));

        if (signatureHashString == null) {
            return MISSING_SIGNATURE_HASH;
        }
        JSONObject response = new JSONObject();
        try {
            Transaction transaction = ParameterParser.parseTransaction(unsignedTransactionJSONString, unsignedBytesString, null).build();
            MessageDigest digest = Crypto.sha256();
            digest.update(transaction.getUnsignedBytes());
            byte[] fullHash = digest.digest(Convert.parseHexString(signatureHashString));
            response.put("fullHash", Convert.toHexString(fullHash));
        } catch (BNDException.NotValidException e) {
            JSONData.putException(response, e, "Incorrect unsigned transaction json or bytes");
        }
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
