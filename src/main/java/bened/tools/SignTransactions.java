/** ****************************************************************************
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
 ***************************************************************************** */
package bened.tools;

import bened.Bened;
import bened.Transaction;
import bened.util.Convert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;

public final class SignTransactions {

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.out.println("Usage: SignTransactions <unsigned transaction bytes file> <signed transaction bytes file>");
                System.exit(1);
            }
            File unsigned = new File(args[0]);
            if (!unsigned.exists()) {
                System.out.println("File not found: " + unsigned.getAbsolutePath());
                System.exit(1);
            }
            File signed = new File(args[1]);
            if (signed.exists()) {
                System.out.println("File already exists: " + signed.getAbsolutePath());
                System.exit(1);
            }
            String secretPhrase;
            Console console = System.console();
            if (console == null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    secretPhrase = reader.readLine();
                }
            } else {
                secretPhrase = new String(console.readPassword("Secret phrase: "));
            }
            int n = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(unsigned));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(signed))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    byte[] transactionBytes = Convert.parseHexString(line);
                    Transaction.Builder builder = Bened.newTransactionBuilder(transactionBytes);
                    Transaction transaction = builder.build(secretPhrase);
                    writer.write(Convert.toHexString(transaction.getBytes()));
                    writer.newLine();
                    n += 1;
                }
            }
            System.out.println("Signed " + n + " transactions");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
