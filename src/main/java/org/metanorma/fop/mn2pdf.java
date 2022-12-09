package org.metanorma.fop;

import java.io.*;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import static org.metanorma.Constants.APP_NAME;
import static org.metanorma.Constants.ERROR_EXIT_CODE;
import static org.metanorma.Constants.DEBUG;
import org.metanorma.utils.LoggerHelper;


/**
 * This class for the conversion of an XML file to PDF using FOP and JEuclid
 */
public class mn2pdf {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private static long startTime;
    
    static final String CMD = "java -Xss5m -Xmx2048m -jar " + APP_NAME + ".jar";
    
    static final Options optionsInfo = new Options() {
        {   
            addOption(Option.builder("v")
               .longOpt("version")
               .desc("display application version")
               .required(true)
               .build());
            }
    };
    
    static final Options options = new Options() {
        {   
            addOption(Option.builder("f")
                .longOpt("font-path")
                .desc("optional path to fonts folder")
                .hasArg()
                .argName("folder")
                .required(false)
                .build());
            addOption(Option.builder("m")
                .longOpt("font-manifest")
                .desc("optional fontist manifest file")
                .hasArg()
                .argName("file")
                .required(false)
                .build());
            addOption(Option.builder("x")
                .longOpt("xml-file")
                .desc("path to source XML file")
                .hasArg()
                .argName("file")
                .required(true)
                .build());
            addOption(Option.builder("s")
                .longOpt("xsl-file")
                .desc("path to XSL file")
                .hasArg()
                .argName("file")
                .required(true)
                .build());
            addOption(Option.builder("o")
                .longOpt("pdf-file")
                .desc("path to output PDF file")
                .hasArg()
                .argName("file")
                .required(true)
                .build());
            addOption(Option.builder("p")
                .longOpt("param")
                .argName("name=value")
                .hasArgs()
                .valueSeparator()
                .numberOfArgs(2)
                .desc("parameter(s) for xslt")
                .required(false)
                .build()); 
            addOption(Option.builder("d")
                .longOpt("debug")
                .desc("debug mode, write intermediate fo.xml file and log files")
                .required(false)
                .build());
            addOption(Option.builder("skippdf")
                .longOpt("skip-pdf-generation")
                .desc("skip PDF generation (in debug mode only)")
                .required(false)
                .build());
            addOption(Option.builder("split")
                .longOpt("split-by-language")
                .desc("additionally create a PDF for each language in XML")
                .required(false)
                .build());
            addOption(Option.builder("hl")
                .longOpt("syntax-highlight")
                .desc("source code syntax highlighting")
                .required(false)
                .build());
            addOption(Option.builder("ep")
                .longOpt("encryption-parameters")
                .desc("path to YAML file with encryption parameters")
                .hasArg()
                .argName("file")
                .required(false)
                .build());
            addOption(Option.builder("el")
                .longOpt("encryption-length")
                .desc("encryption length in bit (default 128)")
                .hasArg()
                .argName("integer")
                .type(Number.class)
                .required(false)
                .build());
            addOption(Option.builder("op")
                .longOpt("owner-password")
                .desc("owner password")
                .hasArg()
                .argName("string")
                .required(false)
                .build());
            addOption(Option.builder("up")
                .longOpt("user-password")
                .desc("user password")
                .hasArg()
                .argName("string")
                .required(false)
                .build());
            addOption(Option.builder("ap")
                .longOpt("allow-print")
                .desc("allow printing")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("aph")
                .longOpt("allow-print-hq")
                .desc("allow high quality printing")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("ac")
                .longOpt("allow-copy-content")
                .desc("allow copy content")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("aec")
                .longOpt("allow-edit-content")
                .desc("allow editing")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("aea")
                .longOpt("allow-edit-annotations")
                .desc("allow editing of annotations")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("af")
                .longOpt("allow-fill-in-forms")
                .desc("allow filling in forms")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("aac")
                .longOpt("allow-access-content")
                .desc("allow text and graphics extraction for accessibility purposes")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("aad")
                .longOpt("allow-assemble-document")
                .desc("allow assembling documents")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("em")
                .longOpt("encrypt-metadata")
                .desc("encrypt the Metadata stream")
                .hasArg()
                .argName("true(default)|false")
                .type(Boolean.class)
                .required(false)
                .build());
            addOption(Option.builder("v")
               .longOpt("version")
               .desc("display application version")
               .required(false)
               .build());
        }
    };
    
    static final String USAGE = getUsage();
    
    
    /**
     * Main method.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) throws ParseException {
        
        LoggerHelper.setupLogger();
        
        startTime = System.currentTimeMillis();
        
        CommandLineParser parser = new DefaultParser();
        
        String ver = Util.getAppVersion();
        
        boolean cmdFail = false;
        
        try {
            CommandLine cmdInfo = parser.parse(optionsInfo, args);
            if (cmdInfo.hasOption("version")) {
                System.out.println(ver);
            }
        } catch( ParseException exp ) {
            cmdFail = true;
        }
        
        if (cmdFail) {
        
            try {
            
                CommandLine cmd = parser.parse(options, args);

                System.out.println("mn2pdf ");
                if (cmd.hasOption("version")) {
                    System.out.println(ver);
                }
                System.out.println("\n");
                
                final String argXML = cmd.getOptionValue("xml-file");
                final String argXSL = cmd.getOptionValue("xsl-file");
                final String argPDF = cmd.getOptionValue("pdf-file");

                LoggerHelper.setupLogger(argPDF);

                PDFGenerator pdfGenerator = new PDFGenerator(argXML, argXSL, argPDF);
                
                if (cmd.hasOption("font-path")) {
                    pdfGenerator.setFontsPath(cmd.getOptionValue("font-path"));
                }
                
                if (cmd.hasOption("font-manifest")) {
                    pdfGenerator.setFontsManifest(cmd.getOptionValue("font-manifest"));
                }
                
                DEBUG = cmd.hasOption("debug");
                
                pdfGenerator.setSkipPDFGeneration(cmd.hasOption("skip-pdf-generation") && cmd.hasOption("debug"));
                
                pdfGenerator.setSplitByLanguage(cmd.hasOption("split-by-language"));

                if (cmd.hasOption("param")) {
                    pdfGenerator.setXSLTParams(cmd.getOptionProperties("param"));
                }
                
                if (cmd.hasOption("encryption-parameters")) {
                    pdfGenerator.setEncryptionParametersFile(cmd.getOptionValue("encryption-parameters"));
                }

                if (cmd.hasOption("encryption-length")) {
                    pdfGenerator.setEncryptionLength(Integer.valueOf(cmd.getOptionValue("encryption-length")));
                }

                if (cmd.hasOption("owner-password")) {
                    pdfGenerator.setOwnerPassword(cmd.getOptionValue("owner-password"));
                }

                if (cmd.hasOption("user-password")) {
                    pdfGenerator.setUserPassword(cmd.getOptionValue("user-password"));
                }

                if (cmd.hasOption("allow-print")) {
                    pdfGenerator.setAllowPrint(Boolean.valueOf(cmd.getOptionValue("allow-print")));
                }
                
                if (cmd.hasOption("allow-print-hq")) {
                    pdfGenerator.setAllowPrintHQ(Boolean.valueOf(cmd.getOptionValue("allow-print-hq")));
                }

                if (cmd.hasOption("allow-copy-content")) {
                    pdfGenerator.setAllowCopyContent(Boolean.valueOf(cmd.getOptionValue("allow-copy-content")));
                }
                
                if (cmd.hasOption("allow-edit-content")) {
                    pdfGenerator.setAllowEditContent(Boolean.valueOf(cmd.getOptionValue("allow-edit-content")));
                }
                
                if (cmd.hasOption("allow-edit-annotations")) {
                    pdfGenerator.setAllowEditAnnotations(Boolean.valueOf(cmd.getOptionValue("allow-edit-annotations")));
                }
                
                if (cmd.hasOption("allow-fill-in-forms")) {
                    pdfGenerator.setAllowFillInForms(Boolean.valueOf(cmd.getOptionValue("allow-fill-in-forms")));
                }

                if (cmd.hasOption("allow-access-content")) {
                    pdfGenerator.setAllowAccessContent(Boolean.valueOf(cmd.getOptionValue("allow-access-content")));
                }
                
                if (cmd.hasOption("allow-assemble-document")) {
                    pdfGenerator.setAllowAssembleDocument(Boolean.valueOf(cmd.getOptionValue("allow-assemble-document")));
                }
                
                if (cmd.hasOption("encrypt-metadata")) {
                    pdfGenerator.setEncryptMetadata(Boolean.valueOf(cmd.getOptionValue("encrypt-metadata")));
                }
                
                pdfGenerator.setSyntaxHighlight(cmd.hasOption("syntax-highlight"));
                
                try {
                    if (!pdfGenerator.process()) {
                        System.exit(ERROR_EXIT_CODE);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    System.exit(ERROR_EXIT_CODE);
                }
            
            } catch( ParseException exp ) {
                logger.info(USAGE);
                System.exit(ERROR_EXIT_CODE);
            }
        }

        LoggerHelper.closeFileHandler();
        Profiler.printProcessingTime("Total mn2pdf", startTime);

        Profiler.removeMethodCall();

        Profiler.printFullProcessingTime();
    }

    private static String getUsage() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, 80, CMD, "", options, 0, 0, "");
        pw.flush();
        return stringWriter.toString();
    }
}
