package com.metanorma.fop;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    
    static boolean PDFUA_error = false;
    
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
            addOption(Option.builder("v")
               .longOpt("version")
               .desc("display application version")
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
    public void convertmn2pdf(String fontPath, File xml, File xsl, File pdf) throws IOException, FOPException, SAXException, TransformerException, TransformerConfigurationException, TransformerConfigurationException, ParserConfigurationException {

        File srcxml = ImageUpdate(xml);
        
        try {
            //Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xsl));

            //Setup input for XSLT transformation
            Source src = new StreamSource(srcxml);

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
            
            fontConfig fontcfg = new fontConfig(fontPath);
         
            runFOP(fontcfg, src, pdf, transformer);
            
            if(PDFUA_error) {
                System.out.println("INFO: Trying to generate PDF in non PDF/UA-1 mode.");
                fontcfg.setPDFUAmode("DISABLED");
                runFOP(fontcfg, src, pdf, transformer);
            }
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
    }

    private void runFOP (fontConfig fontcfg, Source src, File pdf, Transformer transformer) throws IOException, FOPException, SAXException, TransformerException, TransformerConfigurationException, TransformerConfigurationException {
        OutputStream out = null;
        try {
            // Step 1: Construct a FopFactory by specifying a reference to the configuration file
            FopFactory fopFactory = FopFactory.newInstance(fontcfg.getUpdatedConfig());

            JEuclidFopFactoryConfigurator.configure(fopFactory);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent

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

            transformer.setErrorListener(new DefaultErrorListener());

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
    
    private class DefaultErrorListener implements ErrorListener {

        public void warning(TransformerException exc) {
            System.err.println(exc.toString());
        }

        public void error(TransformerException exc)
                throws TransformerException {
            throw exc;
        }

        public void fatalError(TransformerException exc)
                throws TransformerException {
            String excstr=exc.toString();
            if (excstr.contains("PDFConformanceException") && excstr.contains("all fonts, even the base 14 fonts, have to be embedded") && !PDFUA_error) {
                System.err.println(exc.toString());
                PDFUA_error = true;
            } else {
                throw exc;
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
        
        String ver = Util.getAppVersion();
        
        try {
            
            try {
                CommandLine cmdInfo = parser.parse(optionsInfo, args);
                if (cmdInfo.hasOption("version")) {
                    System.out.println(ver);
                }
            } catch( ParseException exp ) {
            
                CommandLine cmd = parser.parse(options, args);

                System.out.println("mn2pdf ");
                if (cmd.hasOption("version")) {
                    System.out.println(ver);
                }
                System.out.println("\n");
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

                if (cmd.hasOption("version")) {
                    System.out.println(ver);
                }

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
    
    private File ImageUpdate(File xml) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputStream xmlstream = new FileInputStream(xml);
        Document sourceXML = dBuilder.parse(xmlstream);
        NodeList images = sourceXML.getElementsByTagName("image");
        String strTmpPath = System.getProperty("java.io.tmpdir");
        String uuid = UUID.randomUUID().toString();
        Path tmpfilepath = Paths.get(strTmpPath, xml.getName(), uuid.toString());
        boolean changed = false;
        for (int i = 0; i < images.getLength(); i++) {
            Node image = images.item(i);
            Node mimetype = image.getAttributes().getNamedItem("mimetype");
            if (mimetype != null && mimetype.getTextContent().equals("image/svg+xml")) {
                // decode base64 svg into external tmp file
                Node src = image.getAttributes().getNamedItem("src");
                if (src != null && src.getTextContent().startsWith("data:image")) {
                    String base64svg = src.getTextContent().substring(src.getTextContent().indexOf("base64,")+7);
                    String xmlsvg = Util.getDecodedBase64SVGnode(base64svg);
                    Files.createDirectories(tmpfilepath);
                    Path svgpath = Paths.get(tmpfilepath.toString(), "" + i + ".svg");
                    try (BufferedWriter bw = Files.newBufferedWriter(svgpath)) 
                    {
                        bw.write(xmlsvg);
                    }
                    src.setNodeValue(svgpath.toFile().toURI().toURL().toString());
                    changed = true;
                }
            }
        }
        if (changed) {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
               
            //transform document to string 
            transformer.transform(new DOMSource(sourceXML), new StreamResult(writer));
            String xmlString = writer.getBuffer().toString();
            Path updatedxmlpath = Paths.get(tmpfilepath.toString(), "doc.xml");
            try (BufferedWriter bw = Files.newBufferedWriter(updatedxmlpath))
            {
                bw.write(xmlString);
            }
            return updatedxmlpath.toFile();
        } else {
            return xml;
        }
        
    }
}
