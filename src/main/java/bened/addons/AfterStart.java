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

package bened.addons;

import bened.Bened;
import bened.util.Logger;
import bened.util.ThreadPool;

public final class AfterStart implements AddOn {

    @Override
    public void init() {
        String afterStartScript = Bened.getStringProperty("bened.afterStartScript");
        if (afterStartScript != null) {
            ThreadPool.runAfterStart(() -> {
                try {
                    Runtime.getRuntime().exec(afterStartScript);
                } catch (Exception e) {
                    Logger.logErrorMessage("Failed to run after start script: " + afterStartScript, e);
                }
            });
        }
    }

}
