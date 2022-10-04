package org.metanorma.utils;

import org.metanorma.fop.mn2pdf;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public final class LoggerHelper {
    public static final String LOGGER_NAME = mn2pdf.class.getPackage().getName() + "." + mn2pdf.class;

    protected static final Logger logger = Logger.getLogger(LOGGER_NAME);

    private LoggerHelper() {
     
    }
    
    public static void setupLogger() {
        //System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s%6$s%n");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");
        //System.setProperty("handlers", "java.util.logging.ConsoleHandler");
    }

    public static void setupLogger(String logPath) {
        setupLogger();
        try {
            Handler fileHandler = new FileHandler(logPath + ".err");
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
            fileHandler.setLevel(Level.WARNING);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
