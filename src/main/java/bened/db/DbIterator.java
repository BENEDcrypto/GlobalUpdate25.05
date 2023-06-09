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

package bened.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class DbIterator<T> implements Iterator<T>, Iterable<T>, AutoCloseable {

    public interface ResultSetReader<T> {
        T get(Connection con, ResultSet rs) throws Exception;
    }

    private final Connection con;
    private final PreparedStatement pstmt;
    private final ResultSetReader<T> rsReader;
    private final ResultSet rs;

    private boolean hasNext;
    private boolean iterated;

    public DbIterator(Connection con, PreparedStatement pstmt, ResultSetReader<T> rsReader) {
        this.con = con;
        this.pstmt = pstmt;
        this.rsReader = rsReader;
        try {
            this.rs = pstmt.executeQuery();
            this.hasNext = rs.next();
        } catch (SQLException e) {
            DbUtils.close(pstmt, con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean hasNext() {
        if (! hasNext) {
            DbUtils.close(rs, pstmt, con);
        }
        return hasNext;
    }

    @Override
    public T next() {
        if (! hasNext) {
            DbUtils.close(rs, pstmt, con);
            throw new NoSuchElementException();
        }
        try {
            T result = rsReader.get(con, rs);
            hasNext = rs.next();
            return result;
        } catch (Exception e) {
            DbUtils.close(rs, pstmt, con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal not supported");
    }

    @Override
    public void close() {
        DbUtils.close(rs, pstmt, con);
    }

    @Override
    public Iterator<T> iterator() {
        if (iterated) {
            throw new IllegalStateException("Already iterated");
        }
        iterated = true;
        return this;
    }
}
