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

import java.io.IOException;

public abstract class BNDException extends Exception {

    protected BNDException() {
        super();
    }

    protected BNDException(String message) {
        super(message);
    }

    protected BNDException(String message, Throwable cause) {
        super(message, cause);
    }

    protected BNDException(Throwable cause) {
        super(cause);
    }

    public static abstract class ValidationException extends BNDException {

        private ValidationException(String message) {
            super(message);
        }

        private ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class NotCurrentlyValidException extends ValidationException {

        public NotCurrentlyValidException(String message) {
            super(message);
        }

        public NotCurrentlyValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class ExistingTransactionException extends NotCurrentlyValidException {

        public ExistingTransactionException(String message) {
            super(message);
        }

    }

    public static final class NotYetEnabledException extends NotCurrentlyValidException {

        public NotYetEnabledException(String message) {
            super(message);
        }

        public NotYetEnabledException(String message, Throwable throwable) {
            super(message, throwable);
        }

    }

    public static final class NotValidException extends ValidationException {

        public NotValidException(String message) {
            super(message);
        }

        public NotValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class AccountControlException extends NotCurrentlyValidException {

        public AccountControlException(String message) {
            super(message);
        }

        public AccountControlException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }

    public static class InsufficientBalanceException extends NotCurrentlyValidException {

        public InsufficientBalanceException(String message) {
            super(message);
        }

        public InsufficientBalanceException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class NotYetEncryptedException extends IllegalStateException {

        public NotYetEncryptedException(String message) {
            super(message);
        }

        public NotYetEncryptedException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class StopException extends RuntimeException {

        public StopException(String message) {
            super(message);
        }

        public StopException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static final class BenedIOException extends IOException {

        public BenedIOException(String message) {
            super(message);
        }

        public BenedIOException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
