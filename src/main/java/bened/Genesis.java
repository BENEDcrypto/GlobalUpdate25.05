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

package bened;

import java.util.TreeMap;

public final class Genesis {

    public static final long GENESIS_BLOCK_ID = 4212222368369791350L;
    public static final long CREATOR_ID = 6961384895484640367L;
    //public static final long GENESIS_BLOCK_AMOUNT =Constants.START_BALANCE_centesimo;
    public static final byte[] CREATOR_PUBLIC_KEY = {
        -30, -21, -18, 24, 53, 118, -128, -13, -68, -91, -27, 91, 36, -110, 11, -82, -54, 102, 15, 39, 22, 49, -87, -22, 91, 77, 99, 42, -117, -124, -12, 119
    };
    
    
   public static TreeMap<Long, Long>  nb_recipients =new TreeMap<>();

    

     public static final byte[] GENESIS_BLOCK_SIGNATURE = new byte[64];

    private Genesis() {} // never

}
