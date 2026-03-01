package org.metanorma.utils;

import org.metanorma.Constants;
import org.metanorma.fop.Util;
import org.metanorma.fop.mn2pdf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.metanorma.fop.PDFGenerator;

/**
 *
 * @author Alexander Dyuzhev
 */
public final class LoggerHelper {
    public static final String LOGGER_NAME = mn2pdf.class.getPackage().getName() + "." + mn2pdf.class;

    protected static final Logger logger = Logger.getLogger(LOGGER_NAME);

    private static String logFilename = "";

    private static FileHandler fileHandler;

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
            logFilename = logPath + ".err";
            try {
                Files.deleteIfExists(new File(logFilename).toPath());
            } catch (Exception ex) {}

            fileHandler = new FileHandler(logFilename);
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
            fileHandler.setLevel(Level.WARNING);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            //fileHandler.setLevel(Level.INFO);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void closeFileHandler() {
        if (fileHandler != null) {
            fileHandler.close();
        }
        File logfile = new File(logFilename);
        boolean isXSLFOlocation = false;
        if (Util.getFileSize(logfile) > 0) {
            System.out.println("There are warnings and errors:");

            // add additional lines into log
            List<String> sbLog = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(logFilename))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sbLog.add(line);
                    // if log contains location to XSL-FO
                    if (line.contains("(See position ")) {
                        isXSLFOlocation = true;
                        sbLog.add("Please check the PDF and if necessary, report this issue at the following link: https://github.com/metanorma/mn-native-pdf/issues/new, and attach the XSL-FO file or fragment: '" + PDFGenerator.getXSLFOfilename() + "'.");
                    }
                }
            }catch (Exception e) { }

            try (FileWriter writer = new FileWriter(logFilename)) {
                for (String str : sbLog) {
                    System.out.println(str);
                    writer.write(str + System.lineSeparator());
                }
            } catch (Exception e) {
                System.out.println("Can't write a log file to " + logFilename);
            }
            System.out.println("Please check the log file '" + logfile.getAbsolutePath() + "'");
        } else {
            try {
                Files.deleteIfExists(logfile.toPath());
            } catch (IOException e) { }
        }
        // delete XSL-FO file if there isn't position to the XSL-FO line
        if (!isXSLFOlocation && !Constants.DEBUG) {
            try {
                Files.deleteIfExists(Paths.get(PDFGenerator.getXSLFOfilename()));
            } catch (IOException e) { }
        }
    }

}
