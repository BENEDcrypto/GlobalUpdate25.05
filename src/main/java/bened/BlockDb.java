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

import bened.db.DbUtils;
import bened.util.Convert;
import bened.util.Logger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import org.json.simple.JSONObject;

public final class BlockDb {

    /** Block cache */
    static final int BLOCK_CACHE_SIZE = 10;
    static final Map<Long, BlockImpl> blockCache = new HashMap<>();
    static final SortedMap<Integer, BlockImpl> heightMap = new TreeMap<>();
    static final Map<Long, TransactionImpl> transactionCache = new HashMap<>();
    static final Blockchain blockchain = Bened.getBlockchain();
    static {
        Bened.getBlockchainProcessor().addListener((block) -> {
            synchronized (blockCache) {
                int height = block.getHeight();
                Iterator<BlockImpl> it = blockCache.values().iterator();
                while (it.hasNext()) {
                    Block cacheBlock = it.next();
                    int cacheHeight = cacheBlock.getHeight();
                    if (cacheHeight <= height - BLOCK_CACHE_SIZE || cacheHeight >= height) {
                        cacheBlock.getTransactions().forEach((tx) -> transactionCache.remove(tx.getId()));
                        heightMap.remove(cacheHeight);
                        it.remove();
                    }
                }
                block.getTransactions().forEach((tx) -> transactionCache.put(tx.getId(), (TransactionImpl)tx));
                heightMap.put(height, (BlockImpl)block);
                blockCache.put(block.getId(), (BlockImpl)block);
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
    }

    static private void clearBlockCache() {
        synchronized (blockCache) {
            blockCache.clear();
            heightMap.clear();
            transactionCache.clear();
        }
    }

    static BlockImpl findBlock(long blockId) {
        // Check the block cache
        synchronized (blockCache) {
            BlockImpl block = blockCache.get(blockId);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE id = ? limit 1")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block = null;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
                return block;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static boolean hasBlock(long blockId) {
        return hasBlock(blockId, Integer.MAX_VALUE);
    }

    static boolean hasBlock(long blockId, int height) {
        // Check the block cache
        synchronized(blockCache) {
            BlockImpl block = blockCache.get(blockId);
            if (block != null) {
                return block.getHeight() <= height;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT height FROM block WHERE id = ? limit 1")) {
            pstmt.setLong(1, blockId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt("height") <= height;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static long findBlockIdAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            BlockImpl block = heightMap.get(height);
            if (block != null) {
                return block.getId();
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static BlockImpl findBlockAtHeight(int height) {
        // Check the cache
        synchronized(blockCache) {
            BlockImpl block = heightMap.get(height);
            if (block != null) {
                return block;
            }
        }
        // Search the database
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                BlockImpl block;
                if (rs.next()) {
                    block = loadBlock(con, rs);
                } else {
                    throw new RuntimeException("Block at height " + height + " not found in database!");
                }
                return block;
            }
        } catch (SQLException e) {
           throw new RuntimeException(e.toString(), e);
        }
    }

    static BlockImpl findLastBlock() {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block ORDER BY timestamp DESC LIMIT 1")) {
            BlockImpl block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static BlockImpl findLastBlock(int timestamp) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block WHERE timestamp <= ? ORDER BY timestamp DESC LIMIT 1")) {
            pstmt.setInt(1, timestamp);
            BlockImpl block = null;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    block = loadBlock(con, rs);
                }
            }
            return block;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static JSONObject getBlockGenerators(int startHeight, JSONObject _jsonlist, int key) {
        Set<Long> generators = new HashSet<>();
        JSONObject json_end = new JSONObject();
        
        try (Connection con = Db.db.getConnection();
                PreparedStatement pstmt = con.prepareStatement(
                        "SELECT DISTINCT generator_id FROM block WHERE height >= ?")) {
            pstmt.setInt(1, startHeight);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generators.add(rs.getLong("generator_id"));
                }
            }
        } catch (SQLException e) {
            json_end = new JSONObject();
            json_end.put("rs", "CRASH get gen act:"+e ); 
            json_end.put("id", "--");
            json_end.put("effectBalans", "--");
            json_end.put("dedline", "--");
            _jsonlist.put(++key,json_end ); 
            return _jsonlist;
        }
        if( generators.size()<1){
            json_end = new JSONObject();
            json_end.put("rs", "blockchain empty" ); 
            json_end.put("id", "--");
            json_end.put("effectBalans", "--");
            json_end.put("dedline", "--");
            _jsonlist.put(++key,json_end ); 
            return _jsonlist;
        }
        //
        json_end = new JSONObject();
            json_end.put("rs", "found generators:"+generators.size() ); 
            json_end.put("id", "--");
            json_end.put("effectBalans", "--");
            json_end.put("dedline", "--");
            _jsonlist.put(++key,json_end ); 
        ///////////////
        Map<Long, Long> tMapIbal = new TreeMap<>(Collections.reverseOrder());
        
        getBlncIIdfromAcSoft(generators, tMapIbal);
        //
        json_end = new JSONObject();
            json_end.put("rs", "found balances:"+tMapIbal.size() ); 
            json_end.put("id", "--");
            json_end.put("effectBalans", "--");
            json_end.put("dedline", "--");
            _jsonlist.put(++key,json_end ); 
        ///////////////

        JSONObject json_ = new JSONObject();
        //k-bl, v id
        
//        Map<Long, JSONObject> lMapIbal = new TreeMap<>(); //Collections.reverseOrder()
        for (Map.Entry<Long, Long> entry : tMapIbal.entrySet()) {
            long _effectBL = entry.getKey()/1000000;
            if(_effectBL<1)continue;
            json_ = new JSONObject();
            json_.put("effectBalans", _effectBL);
            Long ac_ID = entry.getValue();
            String rs = Convert.rsAccount(ac_ID);
            json_.put("rs", rs); 
//            byte[] _pk = pkid.get(ac_ID);
            json_.put("id", ac_ID);
//            if(_pk!=null){
//                long _deadline = Math.max(( getHitTime(BigInteger.valueOf(Math.max(_effectBL, 0)), getHit(_pk, lastBlock), lastBlock)) - lastBlock.getTimestamp(), 0);
//                json_.put("dedline", _deadline);
//                lMapIbal.put(_deadline, json_);
//            }else{
                json_.put("dedline", "-look in your ems box-");
//            }
            _jsonlist.put(++key, json_ );
             if(key>50)break;
        }   
//        for (Map.Entry<Long, JSONObject> entry : lMapIbal.entrySet()) { 
//            _jsonlist.put(++a, entry.getValue() );
//        }       
            json_end = new JSONObject();
            json_end.put("rs", tMapIbal.size()>50?"and "+(tMapIbal.size()-50)+" more generators...":"all local generators "+tMapIbal.size() ); 
            json_end.put("id", "");
            json_end.put("effectBalans", "");
            json_end.put("dedline", "");
            _jsonlist.put(++key,json_end );
        return _jsonlist;
    }
    
    public static void getBlncIIdfromAcSoft(Set<Long> generators, Map<Long, Long> tMapIbal ){
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT balance,id FROM account WHERE id IN ("+ getLineOfQs(generators.size())+") And latest IS TRUE");) {
            int ind=0;
            for (Iterator<Long> iterator = generators.iterator(); iterator.hasNext();) {
                pstmt.setLong(++ind, iterator.next() );
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tMapIbal.put(rs.getLong("balance"), rs.getLong("id")) ;
                }
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BlockDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static String getLineOfQs(int num) {
    // Joiner and Iterables from the Guava library
    String n = "?";
        for (int i = 0; i < num-1; i++) {
            n=n+",?";
        }
    return n;
    }

    static BlockImpl loadBlock(Connection con, ResultSet rs) {
        return loadBlock(con, rs, false);
    }

    static BlockImpl loadBlock(Connection con, ResultSet rs, boolean loadTransactions) {
        try {
            int version = rs.getInt("version");
            int timestamp = rs.getInt("timestamp");
            long previousBlockId = rs.getLong("previous_block_id");
            long totalAmountNQT = rs.getLong("total_amount");
            long totalFeeNQT = rs.getLong("total_fee");
            int payloadLength = rs.getInt("payload_length");
            long generatorId = rs.getLong("generator_id");
            byte[] previousBlockHash = rs.getBytes("previous_block_hash");
            BigInteger cumulativeDifficulty = new BigInteger(rs.getBytes("cumulative_difficulty"));
            long baseTarget = rs.getLong("base_target");
            long nextBlockId = rs.getLong("next_block_id");
            int height = rs.getInt("height");
            byte[] generationSignature = rs.getBytes("generation_signature");
            byte[] blockSignature = rs.getBytes("block_signature");
            byte[] payloadHash = rs.getBytes("payload_hash");
            long id = rs.getLong("id");
            return new BlockImpl(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                    generatorId, generationSignature, blockSignature, previousBlockHash,
                    cumulativeDifficulty, baseTarget, nextBlockId, height, id, loadTransactions ? TransactionDb.findBlockTransactions(con, id) : null);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void saveBlock(Connection con, BlockImpl block) {
        try {
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, "
                    + "total_amount, total_fee, payload_length, previous_block_hash, cumulative_difficulty, "
                    + "base_target, height, generation_signature, block_signature, payload_hash, generator_id) "
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                int i = 0;
                pstmt.setLong(++i, block.getId());
                pstmt.setInt(++i, block.getVersion());
                pstmt.setInt(++i, block.getTimestamp());
                DbUtils.setLongZeroToNull(pstmt, ++i, block.getPreviousBlockId());
                pstmt.setLong(++i, block.getTotalAmountNQT());
                pstmt.setLong(++i, block.getTotalFeeNQT());
                pstmt.setInt(++i, block.getPayloadLength());
                pstmt.setBytes(++i, block.getPreviousBlockHash());
                pstmt.setBytes(++i, block.getCumulativeDifficulty().toByteArray());
                pstmt.setLong(++i, block.getBaseTarget());
                pstmt.setInt(++i, block.getHeight());
                pstmt.setBytes(++i, block.getGenerationSignature());
                pstmt.setBytes(++i, block.getBlockSignature());
                pstmt.setBytes(++i, block.getPayloadHash());
                pstmt.setLong(++i, block.getGeneratorId());
                pstmt.executeUpdate();
                TransactionDb.saveTransactions(con, block.getTransactions());
            }
            if (block.getPreviousBlockId() != 0) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?")) {
                    pstmt.setLong(1, block.getId());
                    pstmt.setLong(2, block.getPreviousBlockId());
                    pstmt.executeUpdate();
                }
                BlockImpl previousBlock;
                synchronized (blockCache) {
                    previousBlock = blockCache.get(block.getPreviousBlockId());
                }
                if (previousBlock != null) {
                    previousBlock.setNextBlockId(block.getId());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void deleteBlocksFromHeight(int height) {
        long blockId;
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT id FROM block WHERE height = ?")) {
            pstmt.setInt(1, height);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                blockId = rs.getLong("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        Logger.logDebugMessage("Deleting blocks starting from height %s", height);
        BlockDb.deleteBlocksFrom(blockId);
    }

    // relying on cascade triggers in the database to delete the transactions and public keys for all deleted blocks
    static BlockImpl deleteBlocksFrom(long blockId) {
        if (!Db.db.isInTransaction()) {
            BlockImpl lastBlock;
            try {
                Db.db.beginTransaction();
                lastBlock = deleteBlocksFrom(blockId);
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return lastBlock;
        }
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT db_id FROM block WHERE timestamp >= "
                     + "IFNULL ((SELECT timestamp FROM block WHERE id = ?), " + Integer.MAX_VALUE + ") ORDER BY timestamp DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE db_id = ?")) {
            try {
                pstmtSelect.setLong(1, blockId);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    Db.db.commitTransaction();
                    while (rs.next()) {
        	            pstmtDelete.setLong(1, rs.getLong("db_id"));
            	        pstmtDelete.executeUpdate();
                        Db.db.commitTransaction();
                    }
	            }
                BlockImpl lastBlock = findLastBlock();
                lastBlock.setNextBlockId(0);
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE block SET next_block_id = NULL WHERE id = ?")) {
                    pstmt.setLong(1, lastBlock.getId());
                    pstmt.executeUpdate();
                }
                Db.db.commitTransaction();
                return lastBlock;
            } catch (SQLException e) {
                Db.db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

    static void deleteAll() {
        if (!Db.db.isInTransaction()) {
            try {
                Db.db.beginTransaction();
                deleteAll();
                Db.db.commitTransaction();
            } catch (Exception e) {
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
            return;
        }
        Logger.logMessage("Deleting blockchain...");
        try (Connection con = Db.db.getConnection();
             Statement stmt = con.createStatement()) {
            try {
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
                stmt.executeUpdate("TRUNCATE TABLE transaction");
                stmt.executeUpdate("TRUNCATE TABLE block");
                BlockchainProcessorImpl.getInstance().getDerivedTables().forEach(table -> {
                    if (table.isPersistent()) {
                        try {
                            stmt.executeUpdate("TRUNCATE TABLE " + table.toString());
                        } catch (SQLException ignore) {}
                    }
                });
                stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
                Db.db.commitTransaction();
            } catch (SQLException e) {
                Db.db.rollbackTransaction();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            clearBlockCache();
        }
    }

}
