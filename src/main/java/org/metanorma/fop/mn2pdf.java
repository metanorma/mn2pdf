package org.metanorma.fop;

import static org.metanorma.fop.Util.getStreamFromResources;
import static org.metanorma.fop.fontConfig.DEFAULT_FONT_PATH;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.events.Event;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.events.EventFormatter;

import net.sourceforge.jeuclid.fop.plugin.JEuclidFopFactoryConfigurator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFSerializer;
import static org.metanorma.Constants.APP_NAME;
import static org.metanorma.Constants.ERROR_EXIT_CODE;
import static org.metanorma.Constants.DEBUG;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.xml.sax.SAXException;

/**
 * This class for the conversion of an XML file to PDF using FOP and JEuclid
 */
public class mn2pdf {

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
                
                
                
                
                
                //Setup font path, input and output files
                //final String argFontsPath = (cmd.hasOption("font-path") ? cmd.getOptionValue("font-path") : DEFAULT_FONT_PATH); //DEFAULT_FONT_PATH

                //final String argFontManifest = (cmd.hasOption("font-manifest") ? cmd.getOptionValue("font-manifest") : "");
                
                
                
                
                /*File fXML = new File(argXML);
                if (!fXML.exists()) {
                    System.out.println(String.format(INPUT_NOT_FOUND, XML_INPUT, fXML));
                    System.exit(ERROR_EXIT_CODE);
                }*/

                DEBUG = cmd.hasOption("debug");
                //pdfGenerator.setDebugMode(cmd.hasOption("debug"));
                
                //SKIP_PDF = cmd.hasOption("skip-pdf-generation") && cmd.hasOption("debug");
                pdfGenerator.setSkipPDFGeneration(cmd.hasOption("skip-pdf-generation") && cmd.hasOption("debug"));
                
                //SPLIT_BY_LANGUAGE = cmd.hasOption("split-by-language");
                pdfGenerator.setSplitByLanguage(cmd.hasOption("split-by-language"));

                /*if (cmd.hasOption("version")) {
                    System.out.println(ver);
                }*/
                
                //Properties xslparams = new Properties();
                if (cmd.hasOption("param")) {
                    //xslparams = cmd.getOptionProperties("param");                    
                    pdfGenerator.setXSLTParams(cmd.getOptionProperties("param"));
                }
                
                /*if (cmd.hasOption("font-manifest") && !cmd.hasOption("font-path")) {
                    // no output
                } else {
                    System.out.println(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, argFontsPath));
                }*/
                
                /*System.out.println(String.format(INPUT_LOG, XML_INPUT, fXML));
                System.out.println(String.format(INPUT_LOG, XSL_INPUT, fXSL));
                if (!xslparams.isEmpty()) {                    
                    System.out.println(String.format(INPUT_LOG, XSL_INPUT_PARAMS, xslparams.toString()));
                }
                System.out.println("Output: PDF (" + fPDF + ")");
                
                System.out.println();*/
                
                
                try {
                    
                    if (!pdfGenerator.process()) {
                        System.exit(ERROR_EXIT_CODE);
                    }
                    
                    //System.out.println("Success!");
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    System.exit(ERROR_EXIT_CODE);
                }
            
            } catch( ParseException exp ) {
                System.out.println(USAGE);
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
