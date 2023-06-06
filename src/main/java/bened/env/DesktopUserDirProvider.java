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

package bened.env;

import bened.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

abstract class DesktopUserDirProvider implements DirProvider {

    public static final String LOG_FILE_PATTERN = "java.util.logging.FileHandler.pattern";

    private File logFileDir;

    @Override
    public boolean isLoadPropertyFileFromUserDir() {
        return true;
    }

    @Override
    public void updateLogFileHandler(Properties loggingProperties) {
        if (loggingProperties.getProperty(LOG_FILE_PATTERN) == null) {
            logFileDir = new File(getUserHomeDir(), "logs");
            return;
        }
        Path logFilePattern = Paths.get(getUserHomeDir()).resolve(Paths.get(loggingProperties.getProperty(LOG_FILE_PATTERN)));
        loggingProperties.setProperty(LOG_FILE_PATTERN, logFilePattern.toString());

        Path logDirPath = logFilePattern.getParent();
        System.out.printf("Logs dir %s\n", logDirPath.toString());
        this.logFileDir = new File(logDirPath.toString());
        if (!Files.isReadable(logDirPath)) {
            System.out.printf("Creating dir %s\n", logDirPath);
            try {
                Files.createDirectory(logDirPath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot create " + logDirPath, e);
            }
        }
    }

    @Override
    public File getLogFileDir() {
        return logFileDir;
    }

    @Override
    public String getDbDir(String dbDir) {
        return Paths.get(getUserHomeDir()).resolve(Paths.get(dbDir)).toString();
    }

    @Override
    public File getConfDir() {
        return new File(getUserHomeDir(), "conf");
    }

    @Override
    public abstract String getUserHomeDir();

    @Override
    public boolean startNativeModule (String moduleName, String arguments) {
        Process process = null;
        final File executable = Paths.get("native", "modules", moduleName, "launcher.sh").toFile();
        if (!executable.exists()) {
            Logger.logWarningMessage("Failed to launch Native Module: " + moduleName + ", not found! Path: " + executable.getPath() + " Absolute: " + executable.getAbsolutePath());
            return false;
        }
        if (!executable.canRead()) {
            Logger.logWarningMessage("Failed to launch Native Module: " + moduleName + ", can't read! Path: " + executable.getPath() + " Absolute: " + executable.getAbsolutePath());
            return false;
        }
        if (!executable.canExecute()) {
            Logger.logWarningMessage("Failed to launch Native Module: " + moduleName + ", can't execute! Path: " + executable.getPath() + " Absolute: " + executable.getAbsolutePath());
            return false;
        }
        try {
            process = Runtime.getRuntime().exec("sh " + executable.getAbsolutePath() + " " + arguments);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.logWarningMessage("Failed to launch Native Module: " + moduleName + ", IO exception!");
            return false;
        }
        return process != null && process.isAlive();
    }

}
