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
import bened.Transaction;
import bened.Bened;
import bened.BNDException;
import bened.Constants;
import bened.Genesis;
import bened.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static bened.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static bened.http.JSONResponses.missing;

public final class GetBlockchainTransactions extends APIServlet.APIRequestHandler {

    static final GetBlockchainTransactions instance = new GetBlockchainTransactions();

    private GetBlockchainTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS}, "account","publicKey", "timestamp", "type", "subtype",
                "firstIndex", "lastIndex", "numberOfConfirmations", "withMessage", "phasedOnly", "nonPhasedOnly",
                "includeExpiredPrunable", "includePhasingResult", "executedOnly");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws BNDException {
        
        long accountId  =0;
        long accountIdfP=0;
        try{
            byte[] publicKey = ParameterParser.getPublicKey(req);
            accountIdfP = Account.getId(publicKey);
        }catch(Exception e){
        }
        try{
            accountId = ParameterParser.getAccountId(req, true);
        }catch(Exception e){
        }
        if((accountId==0 && accountIdfP==0)){
           throw new ParameterException(missing("account and publickKey")); 
        }
        
//        if (accountId == Genesis.CREATOR_ID  || (accountId!=0 && accountIdfP!=0 && accountId!=accountIdfP)) {
//            return FEATURE_NOT_AVAILABLE;
//        }
        if ( (accountId!=0 && accountIdfP!=0 && accountId!=accountIdfP)) {
            return FEATURE_NOT_AVAILABLE;
        }
        accountId = accountId==0?accountIdfP:accountId;
        
        int timestamp = ParameterParser.getTimestamp(req);
        int numberOfConfirmations = ParameterParser.getNumberOfConfirmations(req);
        boolean withMessage = "true".equalsIgnoreCase(req.getParameter("withMessage"));
        boolean phasedOnly = "true".equalsIgnoreCase(req.getParameter("phasedOnly"));
        boolean nonPhasedOnly = "true".equalsIgnoreCase(req.getParameter("nonPhasedOnly"));
        boolean includeExpiredPrunable = "true".equalsIgnoreCase(req.getParameter("includeExpiredPrunable"));
        boolean includePhasingResult = "true".equalsIgnoreCase(req.getParameter("includePhasingResult"));
        boolean executedOnly = "true".equalsIgnoreCase(req.getParameter("executedOnly"));

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        boolean ignoreRequest = false;
        JSONArray transactions = new JSONArray();
        if (Constants.SERVE_ONLY_LATEST_TRANSACTIONS) {
            if (firstIndex > 901)
                ignoreRequest = true;
            if (lastIndex > 1000)
                lastIndex = 1000;
        }
        if (!ignoreRequest) {
            DbIterator<? extends Transaction> iterator = Bened.getBlockchain().getTransactions(accountId, numberOfConfirmations,
                    type, subtype, timestamp, withMessage, phasedOnly, nonPhasedOnly, firstIndex, lastIndex,
                    includeExpiredPrunable, executedOnly);
                while (iterator.hasNext()) {
                    Transaction transaction = iterator.next();
                    transactions.add(JSONData.transaction(transaction, includePhasingResult));
                }
            }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }

}
