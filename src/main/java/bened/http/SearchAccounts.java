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
import bened.db.DbIterator;
import bened.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchAccounts extends APIServlet.APIRequestHandler {

    static final SearchAccounts instance = new SearchAccounts();

    private SearchAccounts() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        if (query.isEmpty()) {
            return JSONResponses.missing("query");
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        DbIterator<Account.AccountInfo> accounts = Account.searchAccounts(query, firstIndex, lastIndex);
            while (accounts.hasNext()) {
                Account.AccountInfo account = accounts.next();    
                JSONObject accountJSON = new JSONObject();
                JSONData.putAccount(accountJSON, "account", account.getAccountId());
                if (account.getName() != null) {
                    accountJSON.put("name", account.getName());
                }
                if (account.getDescription() != null) {
                    accountJSON.put("description", account.getDescription());
                }
                accountsJSONArray.add(accountJSON);
            }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
