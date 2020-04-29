package com.metanorma.fop;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.CodeSource;
import java.text.MessageFormat;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathFactory;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import net.sourceforge.jeuclid.fop.plugin.JEuclidFopFactoryConfigurator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.xml.sax.SAXException;

/**
 * This class for the conversion of an XML file to PDF using FOP and JEuclid
 */
public class mn2pdf {

    static final String CMD = "java -jar mn2pdf.jar";
    static final String INPUT_NOT_FOUND = "Error: %s file '%s' not found!";
    static final String FONTS_FOLDER_INPUT = "Fonts path";
    static final String XML_INPUT = "XML";
    static final String XSL_INPUT = "XSL";
    static final String INPUT_LOG = "Input: %s (%s)";

    static final String DEFAULT_FONT_PATH = "~/.metanorma/fonts";
    
    static boolean DEBUG = false;
    
    static final Options options = new Options() {
        {   
            addOption(Option.builder("f")
                .longOpt("font-path")
                .desc("optional path to fonts folder")
                .hasArg()
                .argName("folder")
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
            addOption(Option.builder("d")
                .longOpt("debug")
                .desc("write intermediate fo.xml file")
                .required(false)
                .build());
        }
    };
    
    static final String USAGE = getUsage();
    
    static final int ERROR_EXIT_CODE = -1;

    /**
     * Converts an XML file to a PDF file using FOP
     *
     * @param config the FOP config file
     * @param xml the XML source file
     * @param xsl the XSL file
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException, SAXException In case of a FOP problem
     */
    public void convertmn2pdf(String fontPath, File xml, File xsl, File pdf) throws IOException, FOPException, SAXException, TransformerException, TransformerConfigurationException, TransformerConfigurationException {

        OutputStream out = null;
        try {
            //Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xsl));

            //Setup input for XSLT transformation
            Source src = new StreamSource(xml);

            //DEBUG: write intermediate FO to file
            if (DEBUG) {
                
                OutputJaxpImplementationInfo();
                
                // Step 0. Convert XML to FO file with XSL
                
                //Setup output
                OutputStream outstream = new java.io.FileOutputStream(pdf.getAbsolutePath() + ".fo.xml");
                //Resulting SAX events (the generated FO) must be piped through to FOP
                Result res = new StreamResult(outstream);

                //Start XSLT transformation and FO generating
                transformer.transform(src, res);
                
                // using resultWriter
                //StringWriter resultWriter = new StringWriter();
                //StreamResult sr = new StreamResult(resultWriter);
                //transformer.transform(src, sr);
                //String xmlFO = resultWriter.toString();
                //BufferedWriter writer = new BufferedWriter(new FileWriter("fo.xml"));
                //writer.write(xmlFO);
                //writer.close();
            }
            
            // Step 1: Construct a FopFactory by specifying a reference to the configuration file
            fontConfig fontcfg = new fontConfig(fontPath);

            FopFactory fopFactory = FopFactory.newInstance(fontcfg.getUpdatedConfig());

            JEuclidFopFactoryConfigurator.configure(fopFactory);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            // Setup JAXP using identity transformer
            //factory = TransformerFactory.newInstance();
            //transformer = factory.newTransformer(); // identity transformer

            // Setup input stream
            //Source srcFO = new StreamSource(new StringReader(xmlFO));

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Main method.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) throws ParseException {
        
        CommandLineParser parser = new DefaultParser();
        
        
        try {
            CommandLine  cmd = parser.parse(options, args);
        
            System.out.println("mn2pdf\n");
            System.out.println("Preparing...");

            //Setup font path, input and output files
            final String argFontsPath = (cmd.hasOption("font-path") ? cmd.getOptionValue("font-path") : DEFAULT_FONT_PATH);
            
            final String argXML = cmd.getOptionValue("xml-file");
            File fXML = new File(argXML);
            if (!fXML.exists()) {
                System.out.println(String.format(INPUT_NOT_FOUND, XML_INPUT, fXML));
                System.exit(ERROR_EXIT_CODE);
            }
            final String argXSL = cmd.getOptionValue("xsl-file");
            File fXSL = new File(argXSL);
            if (!fXSL.exists()) {
                System.out.println(String.format(INPUT_NOT_FOUND, XSL_INPUT, fXSL));
                System.exit(ERROR_EXIT_CODE);
            }
            final String argPDF = cmd.getOptionValue("pdf-file");
            File fPDF = new File(argPDF);
            
            DEBUG = cmd.hasOption("debug");
            
            System.out.println(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, argFontsPath));
            System.out.println(String.format(INPUT_LOG, XML_INPUT, fXML));
            System.out.println(String.format(INPUT_LOG, XSL_INPUT, fXSL));
            System.out.println("Output: PDF (" + fPDF + ")");
            System.out.println();
            System.out.println("Transforming...");

            try {
                mn2pdf app = new mn2pdf();
                app.convertmn2pdf(argFontsPath, fXML, fXSL, fPDF);
                System.out.println("Success!");
            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(ERROR_EXIT_CODE);
            }
        }
        catch( ParseException exp ) {
            System.out.println(USAGE);
            System.exit(ERROR_EXIT_CODE);
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
    
    private static void OutputJaxpImplementationInfo() {
        System.out.println(getJaxpImplementationInfo("DocumentBuilderFactory", DocumentBuilderFactory.newInstance().getClass()));
        System.out.println(getJaxpImplementationInfo("XPathFactory", XPathFactory.newInstance().getClass()));
        System.out.println(getJaxpImplementationInfo("TransformerFactory", TransformerFactory.newInstance().getClass()));
        System.out.println(getJaxpImplementationInfo("SAXParserFactory", SAXParserFactory.newInstance().getClass()));
    }

    private static String getJaxpImplementationInfo(String componentName, Class componentClass) {
        CodeSource source = componentClass.getProtectionDomain().getCodeSource();
        return MessageFormat.format(
                "{0} implementation: {1} loaded from: {2}",
                componentName,
                componentClass.getName(),
                source == null ? "Java Runtime" : source.getLocation());
    }
}
