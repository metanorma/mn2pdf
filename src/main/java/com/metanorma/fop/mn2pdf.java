package com.metanorma.fop;

import static com.metanorma.fop.fontConfig.DEFAULT_FONT_PATH;
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
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

/**
 * This class for the conversion of an XML file to PDF using FOP and JEuclid
 */
public class mn2pdf {

    static final String CMD = "java -Xss5m -Xmx1024m -jar mn2pdf.jar";
    static final String INPUT_NOT_FOUND = "Error: %s file '%s' not found!";
    static final String FONTS_FOLDER_INPUT = "Fonts path";
    static final String XML_INPUT = "XML";
    static final String XSL_INPUT = "XSL";
    static final String XSL_INPUT_PARAMS = "XSL parameters";
    static final String INPUT_LOG = "Input: %s (%s)";
    static final String WARNING_NONPDFUA = "WARNING: PDF generated in non PDF/UA-1 mode.";
    
    static boolean DEBUG = false;
    
    static boolean SKIP_PDF = false;
    
    static boolean SPLIT_BY_LANGUAGE = false;
    
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
    
    static final int ERROR_EXIT_CODE = -1;

    
    int pageCount = 0;
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
    public void convertmn2pdf(fontConfig fontcfg, SourceXMLDocument sourceXMLDocument, XSLTconverter xsltConverter, File pdf) throws IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {
        
        String imagesxml = sourceXMLDocument.getImageFilePath();
                
        String indexxml = sourceXMLDocument.getIndexFilePath();
        
        try {
            
            File fileXmlIF = new File(indexxml);
            
            //Setup XSLT
            
            Properties xslparams = new Properties();
            xslparams.setProperty("svg_images", imagesxml);
            if (fileXmlIF.exists()) {
                // for document by language
                // index.xml was created for bilingual document
                xslparams.setProperty("external_index", fileXmlIF.getAbsolutePath());
            }
            xsltConverter.setParams(xslparams);
            System.out.println("[INFO] XSL-FO file preparation...");
            // transform XML to XSL-FO (XML .fo file)
            xsltConverter.transform(sourceXMLDocument);

            String xmlFO = sourceXMLDocument.getXMLFO();
            
            if (DEBUG) {   
                //DEBUG: write intermediate FO to file                
                
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".fo.xml"))) {
                        writer.write(xmlFO);                    
                }
                //Setup output
                //OutputStream outstream = new java.io.FileOutputStream(pdf.getAbsolutePath() + ".fo.xml");
                //Resulting SAX events (the generated FO) must be piped through to FOP
                //Result res = new StreamResult(outstream);
                //Start XSLT transformation and FO generating
                //transformer.transform(src, res);
            }
            
            fontcfg.setSourceDocumentFontList(sourceXMLDocument.getDocumentFonts());
            
            Source src = new StreamSource(new StringReader(xmlFO));
            
            src = runSecondPass (indexxml, src, fontcfg, xslparams, sourceXMLDocument, xsltConverter, pdf);
            
            
            // FO processing by FOP
            
            //src = new StreamSource(new StringReader(xmlFO));
            
            runFOP(fontcfg, src, pdf);
            
            if(PDFUA_error) {
                System.out.println("WARNING: Trying to generate PDF in non PDF/UA-1 mode.");
                fontcfg.setPDFUAmode("DISABLED");
                src = new StreamSource(new StringReader(xmlFO));
                runFOP(fontcfg, src, pdf);
                System.out.println(WARNING_NONPDFUA);
            }
            
            for(String msg: fontcfg.getMessages()) {
            	System.out.println(msg);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
        
            
    }
    private void runFOP (fontConfig fontcfg, Source src, File pdf) throws IOException, FOPException, SAXException, TransformerException {
        OutputStream out = null;
        try {
            
            TransformerFactory factory = TransformerFactory.newInstance();            
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            System.out.println("Transforming...");
            // Step 1: Construct a FopFactory by specifying a reference to the configuration file
            FopFactory fopFactory = FopFactory.newInstance(fontcfg.getConfig());
            
            //debug
            fontcfg.outputFOPFontsLog(Paths.get(pdf.getAbsolutePath() + ".fopfonts.log.txt"));
            fontcfg.outputAvailableAWTFonts(Paths.get(pdf.getAbsolutePath() + ".awtfonts.log.txt"));

            JEuclidFopFactoryConfigurator.configure(fopFactory);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent
            foUserAgent.setProducer("Ribose Metanorma mn2pdf version " + Util.getAppVersion());
            
            //Adding a simple logging listener that writes to stdout and stderr            
            //foUserAgent.getEventBroadcaster().addEventListener(new SysOutEventListener());
            // Add your own event listener
            //foUserAgent.getEventBroadcaster().addEventListener(new MyEventListener());

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            // Setup JAXP using identity transformer
            //factory = TransformerFactory.newInstance();
            //transformer = factory.newTransformer(); // identity transformer

            
            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());
            
            transformer.setErrorListener(new DefaultErrorListener());
            
            // Start XSLT transformation and FOP processing
            // Setup input stream   
            
            if (!SKIP_PDF) {
                transformer.transform(src, res);  

                this.pageCount = fop.getResults().getPageCount();
            }
            
        } catch (Exception e) {
            String excstr=e.toString();
            if (excstr.contains("PDFConformanceException") && excstr.contains("PDF/UA-1") && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                System.err.println(e.toString());
                PDFUA_error = true;
            } else {
                e.printStackTrace(System.err);
                System.exit(ERROR_EXIT_CODE);
            } 
            
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    private class DefaultErrorListener implements javax.xml.transform.ErrorListener {

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
            if (excstr.contains("PDFConformanceException") && excstr.contains("PDF/UA-1") && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                System.err.println(exc.toString());
                PDFUA_error = true;
            } else {
                throw exc;
            }            
        }
    }
    
   /* private static class MyEventListener implements org.apache.fop.events.EventListener {

        public void processEvent(Event event) {
            if ("org.apache.fop.layoutmgr.BlockLevelEventProducer.overconstrainedAdjustEndIndent".
                    equals(event.getEventID())) {
                //skip
            } else
            if("org.apache.fop.render.RendererEventProducer.endPage".
                    equals(event.getEventID())) {
                //skip
            }else 
            if ("org.apache.fop.pdf.PDFConformanceException".
                    equals(event.getEventID())) {
                System.err.println(new RuntimeException(EventFormatter.format(event)).toString());
                PDFUA_error = true;
            } 
            else
            if ("org.apache.fop.ResourceEventProducer.imageNotFound"
                    .equals(event.getEventID())) {

                //Get the FileNotFoundException that's part of the event's parameters
                //FileNotFoundException fnfe = (FileNotFoundException)event.getParam("fnfe");

                System.out.println("---=== imageNotFound Event for " + event.getParam("uri")
                        + "!!! ===---");
                //Stop processing when an image could not be found. Otherwise, FOP would just
                //continue without the image!

                System.out.println("Throwing a RuntimeException...");
                //throw new RuntimeException(EventFormatter.format(event), fnfe);
            } else {
                //ignore all other events
            }
        }

    }*/

    /** A simple event listener that writes the events to stdout and sterr. */
    //private static class SysOutEventListener implements org.apache.fop.events.EventListener {

        /** {@inheritDoc} */
   /*     public void processEvent(Event event) {
            String msg = EventFormatter.format(event);
            EventSeverity severity = event.getSeverity();
            if (severity == EventSeverity.INFO) {
                System.out.println("[INFO ] " + msg);
            } else if (severity == EventSeverity.WARN) {
                System.out.println("[WARN ] " + msg);
            } else if (severity == EventSeverity.ERROR) {
                System.err.println("[ERROR] " + msg);
            } else if (severity == EventSeverity.FATAL) {
                System.err.println("[FATAL] " + msg);
            } else {
                assert false;
            }
        }
    }*/

    
    
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
                
                System.out.println("Preparing...");

                //Setup font path, input and output files
                final String argFontsPath = (cmd.hasOption("font-path") ? cmd.getOptionValue("font-path") : DEFAULT_FONT_PATH); //DEFAULT_FONT_PATH

                final String argFontManifest = (cmd.hasOption("font-manifest") ? cmd.getOptionValue("font-manifest") : "");
                
                File fFontManifest = null;
                if (!argFontManifest.isEmpty()) {
                    fFontManifest = new File(argFontManifest);
                    if (!fFontManifest.exists()) {
                        System.out.println(String.format(INPUT_NOT_FOUND, "Font manifest", fFontManifest));
                        System.exit(ERROR_EXIT_CODE);
                    }
                }
                
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
                SKIP_PDF = cmd.hasOption("skip-pdf-generation") && cmd.hasOption("debug");
                
                SPLIT_BY_LANGUAGE = cmd.hasOption("split-by-language");

                if (cmd.hasOption("version")) {
                    System.out.println(ver);
                }
                
                Properties xslparams = new Properties();
                if (cmd.hasOption("param")) {
                    xslparams = cmd.getOptionProperties("param");                    
                }
                
                if (cmd.hasOption("font-manifest") && !cmd.hasOption("font-path")) {
                    // no output
                } else {
                    System.out.println(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, argFontsPath));
                }
                
                System.out.println(String.format(INPUT_LOG, XML_INPUT, fXML));
                System.out.println(String.format(INPUT_LOG, XSL_INPUT, fXSL));
                if (!xslparams.isEmpty()) {                    
                    System.out.println(String.format(INPUT_LOG, XSL_INPUT_PARAMS, xslparams.toString()));
                }
                System.out.println("Output: PDF (" + fPDF + ")");
                
                System.out.println();
                
                SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(fXML);
                
                XSLTconverter xsltConverter = new XSLTconverter(fXSL);
                xsltConverter.setParams(xslparams);
                
                fontConfig fontcfg = new fontConfig();
                fontcfg.setFontPath(argFontsPath);
                
                fontcfg.setFontManifest(fFontManifest);
                
                //debug
                fontcfg.outputFontManifestLog(Paths.get(fPDF.getAbsolutePath() + ".fontmanifest.log.txt"));
                
                
                try {
                    mn2pdf app = new mn2pdf();
                    app.convertmn2pdf(fontcfg, sourceXMLDocument, xsltConverter, fPDF);
                    
                    if (SPLIT_BY_LANGUAGE) {
                        int initial_page_number = 1;
                        int coverpages_count = getCoverPagesCount(fXSL);
                        //determine how many documents in source XML
                        ArrayList<String> languages = sourceXMLDocument.getLanguagesList(); 
                        for (int i = 0; i< languages.size(); i++) {
                            if (i>=1)  {
                                xslparams.setProperty("initial_page_number", "" + initial_page_number);
                            }
                            xslparams.setProperty("doc_split_by_language", "" + languages.get(i));
                            
                            xsltConverter.setParams(xslparams);
                            
                            //add language code to output PDF
                            String argPDFsplit = argPDF;                            
                            argPDFsplit = argPDFsplit.substring(0, argPDFsplit.lastIndexOf(".")) + "_" + languages.get(i) + argPDFsplit.substring(argPDFsplit.lastIndexOf("."));
                            File fPDFsplit = new File(argPDFsplit);
                            
                            System.out.println("Generate PDF for language '" + languages.get(i) + "'.");
                            System.out.println("Output: PDF (" + fPDFsplit + ")");
                            
                            app.convertmn2pdf(fontcfg, sourceXMLDocument, xsltConverter, fPDFsplit);
                            
                            // initial page number for 'next' document
                            initial_page_number = (app.getPageCount() - coverpages_count) + 1;
                        }                        
                    }
                    
                    // flush temporary folder
                    if (!DEBUG) {
                        sourceXMLDocument.flushTempPath();
                    }
                    
                    System.out.println("Success!");
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
    
    
    
       

    
    
    private static int getCoverPagesCount (File fXSL) {
        int countpages = 0;
        try {            
            // open XSL and find 
            // <xsl:variable name="coverpages_count">2</xsl:variable>
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream xmlstream = new FileInputStream(fXSL);
            Document sourceXML = dBuilder.parse(xmlstream);
            
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//*[local-name() = 'variable'][@name='coverpages_count']");
            NodeList vars = (NodeList) expr.evaluate(sourceXML, XPathConstants.NODESET);
            if (vars.getLength() > 0) {
                countpages = Integer.valueOf(vars.item(0).getTextContent());
            }
        } catch (Exception ex) {
            System.err.println("Can't read coverpages_count variable from source XSL.");
            ex.printStackTrace();
        }        
        return countpages;
    }
    
    public int getPageCount() {
        return pageCount;
    }


    private Source runSecondPass (String indexxml, Source sourceFO, fontConfig fontcfg, Properties xslparams, SourceXMLDocument sourceXMLDocument, XSLTconverter xsltConverter, File pdf)  throws Exception, IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {
        Source src = sourceFO;
        
        File fileXmlIF = new File(indexxml);
        
        if (!indexxml.isEmpty() && !fileXmlIF.exists()) { //there is index
             // if file exist - it means that now document by language is processing
            // and don't need to create intermediate file again


            // run 1st pass to produce FOP Intermediate Format
            FopFactory fopFactory = FopFactory.newInstance(fontcfg.getConfig());
            //Create a user agent
            FOUserAgent userAgent = fopFactory.newFOUserAgent();
            //Create an instance of the target document handler so the IFSerializer
            //can use its font setup
            IFDocumentHandler targetHandler = userAgent.getRendererFactory().createDocumentHandler(
                    userAgent, MimeConstants.MIME_PDF);
            //Create the IFSerializer to write the intermediate format
            IFSerializer ifSerializer = new IFSerializer(new IFContext(userAgent));
            //Tell the IFSerializer to mimic the target format
            ifSerializer.mimicDocumentHandler(targetHandler);
            //Make sure the prepared document handler is used
            userAgent.setDocumentHandlerOverride(ifSerializer);
            userAgent.getEventBroadcaster().addEventListener(new SecondPassSysOutEventListener());
            JEuclidFopFactoryConfigurator.configure(fopFactory);

            // Setup output
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            //out = new java.io.BufferedOutputStream(out);
            //String ifFilename = indexxml + ".if";
            //OutputStream out = new java.io.FileOutputStream(new File(ifFilename));
            String xmlIF = "";
            try {
                // Construct FOP (the MIME type here is unimportant due to the override
                // on the user agent)
                Fop fop = fopFactory.newFop(null, userAgent, out);

                Result res = new SAXResult(fop.getDefaultHandler());

                // Setup XSLT
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer(); // identity transformer

                transformer.setErrorListener(new DefaultErrorListener());
                System.out.println("[INFO] Rendering into intermediate format for index preparation...");
                // Start XSLT transformation and FOP processing
                transformer.transform(src, res);

                xmlIF = out.toString("UTF-8");

            } finally {
                out.close();
            }
            Util.createIndexFile(indexxml, xmlIF);

            if (fileXmlIF.exists()) {
                // pass index.xml path to xslt (for second pass)
                xslparams.setProperty("external_index", fileXmlIF.getAbsolutePath());

                xsltConverter.setParams(xslparams);

                System.out.println("[INFO] XSL-FO file preparation (second pass)...");
                // transform XML to XSL-FO (XML .fo file)
                xsltConverter.transform(sourceXMLDocument);

                String xmlFO = sourceXMLDocument.getXMLFO();

                if (DEBUG) {   
                    //DEBUG: write intermediate FO to file                
                    try ( 
                        BufferedWriter writer = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".fo.2nd.xml"))) {
                            writer.write(xmlFO);                    
                    }
                }
                src = new StreamSource(new StringReader(xmlFO));
            }
        }
        return src;
    }

    
    /** A simple event listener that writes the events to stdout and sterr. */
    private static class SecondPassSysOutEventListener implements org.apache.fop.events.EventListener {

        /** {@inheritDoc} */
        public void processEvent(Event event) {
            String msg = EventFormatter.format(event);
            EventSeverity severity = event.getSeverity();
            if (severity == EventSeverity.INFO) {
                if(msg.startsWith("Rendered page #")) {
                    System.out.println("[INFO] Intermediate format. " + msg);
                }
            } else if (severity == EventSeverity.WARN) {
                //System.out.println("[WARN] " + msg);
            } else if (severity == EventSeverity.ERROR) {
                System.err.println("[ERROR] " + msg);
            } else if (severity == EventSeverity.FATAL) {
                System.err.println("[FATAL] " + msg);
            } else {
                assert false;
            }
        }
    }
    
}
