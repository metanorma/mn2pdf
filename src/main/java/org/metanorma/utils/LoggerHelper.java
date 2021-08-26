package org.metanorma.utils;

import org.metanorma.fop.mn2pdf;

/**
 *
 * @author Alexander Dyuzhev
 */
public final class LoggerHelper {
    public static final String LOGGER_NAME = mn2pdf.class.getPackage().getName() + "." + mn2pdf.class;
    
    private LoggerHelper() {
     
    }
    
    public static void setupLogger() {
        //System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$s] %5$s%6$s%n");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");
        //System.setProperty("handlers", "java.util.logging.ConsoleHandler");
    }
}
