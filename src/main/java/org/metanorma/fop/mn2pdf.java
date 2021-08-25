package org.metanorma.fop;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import static org.metanorma.fop.PDFGenerator.logger;
import org.metanorma.utils.LoggerHelper;


/**
 * This class for the conversion of an XML file to PDF using FOP and JEuclid
 */
public class mn2pdf {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
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
