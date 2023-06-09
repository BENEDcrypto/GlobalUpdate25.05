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
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliasesLike extends APIServlet.APIRequestHandler {

    static final GetAliasesLike instance = new GetAliasesLike();

    private GetAliasesLike() {
        super(new APITag[] {APITag.ALIASES, APITag.SEARCH}, "aliasPrefix", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws BNDException {
        return JSONResponses.FEATURE_NOT_AVAILABLE;
    }

}
