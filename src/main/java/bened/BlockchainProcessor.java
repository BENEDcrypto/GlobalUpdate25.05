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

import static bened.Constants.sleemlog;
import bened.db.DerivedDbTable;
import bened.peer.Peer;
import bened.util.Observable;
import org.json.simple.JSONObject;

import java.util.List;

public interface BlockchainProcessor extends Observable<Block,BlockchainProcessor.Event> {

    enum Event {
        BLOCK_PUSHED, BLOCK_POPPED, BLOCK_GENERATED, BLOCK_SCANNED,
        RESCAN_BEGIN, RESCAN_END,
        BEFORE_BLOCK_ACCEPT, AFTER_BLOCK_ACCEPT,
        BEFORE_BLOCK_APPLY, AFTER_BLOCK_APPLY
    }

    Peer getLastBlockchainFeeder();

    int getLastBlockchainFeederHeight();

    boolean isScanning();

    boolean isDownloading();

    boolean isProcessingBlock();

    int getMinRollbackHeight();

    int getInitialScanHeight();

    void processPeerBlock(JSONObject request) throws BNDException;

    void fullReset();

    void scan(int height, boolean validate);

    void fullScanWithShutdown();

    void setGetMoreBlocks(boolean getMoreBlocks);

    List<? extends Block> popOffTo(int height);

    void registerDerivedTable(DerivedDbTable table);

    void trimDerivedTables();

    int restorePrunedData();

    Transaction restorePrunedTransaction(long transactionId);

    class BlockNotAcceptedException extends BNDException {

        private final BlockImpl block;

        BlockNotAcceptedException(String message, BlockImpl block) {
            super(message);
            this.block = block;
        }

        BlockNotAcceptedException(Throwable cause, BlockImpl block) {
            super(cause);
            this.block = block;
        }

        @Override
        public String getMessage() {
            Blockchain bh = Bened.getBlockchain();
            return block == null ? super.getMessage() : super.getMessage() + ", block " + block.getStringId() + " " 
                    + (sleemlog?"[timestamp:"+block.getTimestamp()+" trxsizw:"+block.getTransactions().size()
                    +" height:"+(bh.getHeight()+1)+"]" :block.getJSONObject().toJSONString());
        }

    }

    class TransactionNotAcceptedException extends BlockNotAcceptedException {

        private final TransactionImpl transaction;

        TransactionNotAcceptedException(String message, TransactionImpl transaction) {
            super(message, transaction.getBlock());
            this.transaction = transaction;
        }

        TransactionNotAcceptedException(Throwable cause, TransactionImpl transaction) {
            super(cause, transaction.getBlock());
            this.transaction = transaction;
        }

        public TransactionImpl getTransaction() {
            return transaction;
        }

        @Override
        public String getMessage() {
            return super.getMessage() + ", transaction " + transaction.getStringId() + " " + transaction.getJSONObject().toJSONString();
        }
    }

    class BlockOutOfOrderException extends BlockNotAcceptedException {

        BlockOutOfOrderException(String message, BlockImpl block) {
            super(message, block);
        }

	}

}
