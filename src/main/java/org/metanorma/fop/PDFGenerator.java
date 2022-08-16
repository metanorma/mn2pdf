package org.metanorma.fop;

import org.metanorma.fop.annotations.Annotation;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sourceforge.jeuclid.fop.plugin.JEuclidFopFactoryConfigurator;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.fop.render.intermediate.IFSerializer;
import org.apache.fop.render.intermediate.IFUtil;
import static org.metanorma.Constants.*;
import static org.metanorma.fop.fontConfig.DEFAULT_FONT_PATH;
import static org.metanorma.fop.Util.getStreamFromResources;
import org.metanorma.utils.LoggerHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


/**
 *
 * @author Alexander Dyuzhev
 */
public class PDFGenerator {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private String fontsPath = "";

    private String fontsManifest = "";
    
    final private String inputXMLFilePath;
    
    SourceXMLDocument sourceXMLDocument;
    
    final private String inputXSLFilePath;
    
    final private String outputPDFFilePath;
    
    //private boolean isDebugMode = false;
    
    private boolean isSkipPDFGeneration = false;
    
    private boolean isSplitByLanguage = false;
    
    private boolean isAddMathAsText = false;
    
    private boolean isAddLineNumbers = false;
    
    private boolean isAddMathAsAttachment = false;
    
    private boolean isAddAnnotations = false;
    
    private boolean isTableExists = false;
    
    private String xmlTableIF = "";
    
    private Properties xsltParams = new Properties();
    
    private String encryptionParametersFile = "";
    
    private Map<String,Object> encryptionParams = new HashMap<>();
    
    private boolean isSyntaxHighlight = false;
    
    int pageCount = 0;
    
    boolean PDFUA_error = false;
    
    private String debugXSLFO = "";
    
    private long startTime;
    
    public void setFontsPath(String fontsPath) {
        this.fontsPath = fontsPath;
    }

    public void setFontsManifest(String fontsManifest) {
        this.fontsManifest = fontsManifest;
    }

    /*public void setDebugMode(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }*/

    public void setSkipPDFGeneration(boolean isSkipPDFGeneration) {
        this.isSkipPDFGeneration = isSkipPDFGeneration;
    }

    public void setSplitByLanguage(boolean isSplitByLanguage) {
        this.isSplitByLanguage = isSplitByLanguage;
    }

    
    public void setXSLTParams(Properties xsltParams) {
        this.xsltParams = xsltParams;
    }

    public void setEncryptionParametersFile(String encryptionParametersFile) {
        this.encryptionParametersFile = encryptionParametersFile;
    }

    public void setEncryptionLength(int encryptionLength) { 
        encryptionParams.put("encryption-length", encryptionLength);
    }

    public void setOwnerPassword(String ownerPassword) {
        encryptionParams.put("owner-password", ownerPassword);
    }

    public void setUserPassword(String userPassword) {
        encryptionParams.put("user-password", userPassword);
    }

    public void setAllowPrint(boolean isAllowPrint) {
        encryptionParams.put("noprint", !isAllowPrint);
    }

    public void setAllowPrintHQ(boolean isAllowPrintHQ) {
        encryptionParams.put("noprinthq", !isAllowPrintHQ);
    }

    public void setAllowCopyContent(boolean isAllowCopyContent) {
        encryptionParams.put("nocopy", !isAllowCopyContent);
    }

    public void setAllowEditContent(boolean isAllowEditContent) {
        encryptionParams.put("noedit", !isAllowEditContent);
    }

    public void setAllowEditAnnotations(boolean isAllowEditAnnotations) {
        encryptionParams.put("noannotations", !isAllowEditAnnotations);
    }

    public void setAllowFillInForms(boolean isAllowFillInForms) {
        encryptionParams.put("nofillinforms", !isAllowFillInForms);
    }

    public void setAllowAccessContent(boolean isAllowAccessContent) {
        encryptionParams.put("noaccesscontent", !isAllowAccessContent);
    }

    public void setAllowAssembleDocument(boolean isAllowAssembleDocument) {
        encryptionParams.put("noassembledoc", !isAllowAssembleDocument);
    }

    public void setEncryptMetadata(boolean isEncryptMetadata) {
        encryptionParams.put("encrypt-metadata", isEncryptMetadata);
    }
    
    public void setSyntaxHighlight(boolean isSyntaxHighlight) {
        this.isSyntaxHighlight = isSyntaxHighlight;
    }
    
    public PDFGenerator (String inputXMLFilePath, String inputXSLFilePath, String outputPDFFilePath) {
        this.inputXMLFilePath = inputXMLFilePath;
        this.inputXSLFilePath = inputXSLFilePath;
        this.outputPDFFilePath = outputPDFFilePath;
    }
    
    
    public boolean process() {
        try {
            
            logger.info("Preparing...");
            
            File fXML = new File(inputXMLFilePath);
            if (!fXML.exists()) {
                logger.severe(String.format(INPUT_NOT_FOUND, XML_INPUT, fXML));
                return false;
            }
            
            File fXSL = new File(inputXSLFilePath);
            if (!fXSL.exists()) {
                logger.severe(String.format(INPUT_NOT_FOUND, XSL_INPUT, fXSL));
                return false;
            }
            
            File fFontsManifest = null;
            if (!fontsManifest.isEmpty()) {
                fFontsManifest = new File(fontsManifest);
                if (!fFontsManifest.exists()) {
                    //System.out.println(String.format(INPUT_NOT_FOUND, "Font manifest", fFontManifest));
                    logger.severe(String.format(INPUT_NOT_FOUND, "Font manifest", fFontsManifest));
                    //System.exit(ERROR_EXIT_CODE);
                    return false;
                }
            }
            
            File fEncryptionParameters = null;
            if (!encryptionParametersFile.isEmpty()) {
                fEncryptionParameters = new File(encryptionParametersFile);
                if (!fEncryptionParameters.exists()) {
                    logger.severe(String.format(INPUT_NOT_FOUND, "Encryption parameters file", fEncryptionParameters));
                    return false;
                }
                readEncryptionParameters(fEncryptionParameters);
            }
            
            File fPDF = new File(outputPDFFilePath);
            
            if (!fontsManifest.isEmpty() && fontsPath.isEmpty()) {
                    // no output
            } else {
                //System.out.println(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, argFontsPath));
                logger.info(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, fontsPath));
            }
            
            if (fontsPath.isEmpty()) {
                fontsPath = DEFAULT_FONT_PATH;
            }
            
            logger.info(String.format(INPUT_LOG, XML_INPUT, fXML));
            logger.info(String.format(INPUT_LOG, XSL_INPUT, fXSL));
            
            if (!xsltParams.isEmpty()) {                    
                logger.info(String.format(INPUT_LOG, XSL_INPUT_PARAMS, xsltParams.toString()));
            }
            
            logger.info(String.format(OUTPUT_LOG, PDF_OUTPUT, fPDF));
            logger.info("");
            
            // read input XML to XML document and find tag '<review'
            String element_review =  Util.readValueFromXML(fXML, "//*[local-name() = 'review'][1]");
            isAddAnnotations = element_review.length() != 0;
            
            // find tag 'table' or 'dl'
            String element_table = Util.readValueFromXML(fXML, "//*[local-name() = 'table' or local-name() = 'dl'][1]");
            isTableExists = element_table.length() != 0;
            
            String element_math = Util.readValueFromXML(fXML, "//*[local-name() = 'math'][1]");
            
            // read XSL to XML Document and find param values
            String add_math_as_text = Util.readValueFromXML(fXSL, "/*[local-name() = 'stylesheet']/*[local-name() = 'param'][@name = 'add_math_as_text']");
            isAddMathAsText = add_math_as_text.equalsIgnoreCase("true") && element_math.length() != 0;
            
            String add_math_as_attachment = Util.readValueFromXML(fXSL, "/*[local-name() = 'stylesheet']/*[local-name() = 'param'][@name = 'add_math_as_attachment']");
            isAddMathAsAttachment = add_math_as_attachment.equalsIgnoreCase("true");
            
            sourceXMLDocument = new SourceXMLDocument(fXML);
            
            XSLTconverter xsltConverter = new XSLTconverter(fXSL);

            if (isSyntaxHighlight) {
                xsltParams.put("syntax-highlight", "true");
            }
            xsltConverter.setParams(xsltParams);
            
            
            fontConfig fontcfg = new fontConfig();
            fontcfg.setFontPath(fontsPath);

            fontcfg.setFontManifest(fFontsManifest);
            
            //debug
            fontcfg.outputFontManifestLog(Paths.get(fPDF.getAbsolutePath() + ".fontmanifest.log.txt"));
            
            
            convertmn2pdf(fontcfg, xsltConverter, fPDF);
            
            
            if (isSplitByLanguage) {
                int initial_page_number = 1;
                int coverpages_count = Util.getCoverPagesCount(fXSL);
                //determine how many documents in source XML
                ArrayList<String> languages = sourceXMLDocument.getLanguagesList(); 
                for (int i = 0; i< languages.size(); i++) {
                    if (i>=1)  {
                        xsltParams.setProperty("initial_page_number", "" + initial_page_number);
                    }
                    xsltParams.setProperty("doc_split_by_language", "" + languages.get(i));

                    xsltConverter.setParams(xsltParams);

                    //add language code to output PDF
                    String argPDFsplit = outputPDFFilePath;                            
                    argPDFsplit = argPDFsplit.substring(0, argPDFsplit.lastIndexOf(".")) + "_" + languages.get(i) + argPDFsplit.substring(argPDFsplit.lastIndexOf("."));
                    File fPDFsplit = new File(argPDFsplit);

                    logger.log(Level.INFO, "Generate PDF for language ''{0}''.", languages.get(i));
                    logger.log(Level.INFO, "Output: PDF ({0})", fPDFsplit);

                    convertmn2pdf(fontcfg, xsltConverter, fPDFsplit);

                    // initial page number for 'next' document
                    initial_page_number = (getPageCount() - coverpages_count) + 1;
                }                        
            }
            
            // flush temporary folder
            if (!DEBUG) {
                sourceXMLDocument.flushTempPath();
            }
            
            logger.info("Success!");
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    
    
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
    private void convertmn2pdf(fontConfig fontcfg, XSLTconverter xsltConverter, File pdf) throws IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {
        
        String imagesxml = sourceXMLDocument.getImageFilePath();
                
        String indexxml = sourceXMLDocument.getIndexFilePath();
        
        try {
            
            //Setup XSLT
            Properties additionalXSLTparams = new Properties();
            
            additionalXSLTparams.setProperty("svg_images", imagesxml);
            
            File fileXmlIF = new File(indexxml);
            if (fileXmlIF.exists()) {
                // for document by language
                // index.xml was created for bilingual document
                additionalXSLTparams.setProperty("external_index", fileXmlIF.getAbsolutePath());
            }
            
            String basepath = sourceXMLDocument.getDocumentFilePath() + File.separator;
            // redefine basepath 
            if (xsltParams.containsKey("baseassetpath")) {
                basepath = xsltParams.getProperty("baseassetpath") + File.separator;
            }
            additionalXSLTparams.setProperty("basepath", basepath);
            
            xsltConverter.setParams(additionalXSLTparams);
            
            setTablesWidths(fontcfg, xsltConverter, pdf);
            
            logger.info("[INFO] XSL-FO file preparation...");
            
            // transform XML to XSL-FO (XML .fo file)
            startTime = System.currentTimeMillis();
            
            xsltConverter.transform(sourceXMLDocument);
            
            printProcessingTime();

            String xmlFO = sourceXMLDocument.getXMLFO();
            debugXSLFO = xmlFO;
            
            String add_line_numbers = Util.readValueFromXMLString(xmlFO, "/*[local-name() = 'root']/processing-instruction('add_line_numbers')");
            isAddLineNumbers = add_line_numbers.equalsIgnoreCase("true");
            
            debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".fo.xml");
            
            fontcfg.setSourceDocumentFontList(sourceXMLDocument.getDocumentFonts());
            
            Source src = new StreamSource(new StringReader(xmlFO));
            
            
            src = runSecondPass (indexxml, src, fontcfg, additionalXSLTparams, xsltConverter, pdf);
            
            
            // FO processing by FOP
            
            //src = new StreamSource(new StringReader(xmlFO));
            
            runFOP(fontcfg, src, pdf);
            
            if(PDFUA_error) {
                logger.info("WARNING: Trying to generate PDF in non PDF/UA-1 mode.");
                fontcfg.setPDFUAmode("DISABLED");
                src = new StreamSource(new StringReader(xmlFO));
                runFOP(fontcfg, src, pdf);
                logger.info(WARNING_NONPDFUA);
            }
            
            for(String msg: fontcfg.getMessages()) {
            	logger.info(msg);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
        
            
    }
    
    
    private void runFOP (fontConfig fontcfg, Source src, File pdf) throws IOException, FOPException, SAXException, TransformerException {
        OutputStream out = null;
        String xmlIF = null;
        long startMethodTime = System.currentTimeMillis();
        try {
            
            String mime = MimeConstants.MIME_PDF;
            
            if (isAddMathAsText || isAddAnnotations || isAddLineNumbers) {
                if (isAddMathAsText) {
                    logger.info("Adding Math as text...");
                }
                logger.info("Transforming to Intermediate Format...");
                xmlIF = generateFOPIntermediateFormat(src, fontcfg.getConfig(), pdf, false, "");
                
                if (isAddMathAsText) {
                    logger.info("Updating Intermediate Format (adding hidden math)...");
                    xmlIF = applyXSLT("add_hidden_math.xsl", xmlIF, true);
                }
                
                debugXSLFO = xmlIF;
                
                debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if.mathtext.xml");
                
                if (isAddLineNumbers) {
                    logger.info("Updating Intermediate Format (adding line numbers)...");
                    xmlIF = applyXSLT("add_line_numbers.xsl", xmlIF, true);
                }
                
                debugXSLFO = xmlIF;
                
                debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if.linenumbers.xml");
                
                
                src = new StreamSource(new StringReader(xmlIF));
            }
            
            logger.info("Transforming to PDF...");
            
            TransformerFactory factory = TransformerFactory.newInstance();            
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            //System.out.println("Transforming...");
            
            // Step 1: Construct a FopFactory by specifying a reference to the configuration file
            FopFactory fopFactory = FopFactory.newInstance(fontcfg.getConfig());
            
            //debug
            fontcfg.outputFOPFontsLog(Paths.get(pdf.getAbsolutePath() + ".fopfonts.log.txt"));
            fontcfg.outputAvailableAWTFonts(Paths.get(pdf.getAbsolutePath() + ".awtfonts.log.txt"));

            JEuclidFopFactoryConfigurator.configure(fopFactory);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent
            foUserAgent.setProducer("Ribose Metanorma mn2pdf version " + Util.getAppVersion());
            setEncryptionParams(foUserAgent);
            
            //Adding a simple logging listener that writes to stdout and stderr            
            //foUserAgent.getEventBroadcaster().addEventListener(new SysOutEventListener());
            // Add your own event listener
            //foUserAgent.getEventBroadcaster().addEventListener(new MyEventListener());

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);
            
            if (isAddMathAsText || isAddAnnotations || isAddLineNumbers) { // process IF to PDF
                //Setup target handler
                IFDocumentHandler targetHandler = fopFactory.getRendererFactory().createDocumentHandler(
                        foUserAgent, mime);
                //Setup fonts
                IFUtil.setupFonts(targetHandler);
                targetHandler.setResult(new StreamResult(out));
                
                IFParser parser = new IFParser();
                
                //Send XSLT result to AreaTreeParser
                SAXResult res = new SAXResult(parser.getContentHandler(targetHandler, foUserAgent));
                
                //Start area tree parsing
                if (!isSkipPDFGeneration) {
                    transformer.transform(src, res);
                    //this.pageCount = fop.getResults().getPageCount();
                }
            } 
                
            else {


                // Construct fop with desired output format
                Fop fop = fopFactory.newFop(mime, foUserAgent, out);

                // Setup JAXP using identity transformer
                //factory = TransformerFactory.newInstance();
                //transformer = factory.newTransformer(); // identity transformer


                // Resulting SAX events (the generated FO) must be piped through to FOP
                Result res = new SAXResult(fop.getDefaultHandler());

                transformer.setErrorListener(new DefaultErrorListener());

                // Start XSLT transformation and FOP processing
                // Setup input stream   

                if (!isSkipPDFGeneration) {
                    transformer.transform(src, res);  

                    this.pageCount = fop.getResults().getPageCount();
                }
            }
            
        } catch (Exception e) {
            String excstr=e.toString();
            if (excstr.contains("PDFConformanceException") && excstr.contains("PDF/UA-1") && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                //System.err.println(e.toString());
                logger.severe(e.toString());
                PDFUA_error = true;
            } else {
                e.printStackTrace(System.err);
                if (!debugXSLFO.isEmpty()) {
                    debugXSLFO = debugXSLFO.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    String debugXSLFOfile = pdf.getAbsolutePath() + ".fo.debug.xml";
                    try ( 
                        BufferedWriter writer = Files.newBufferedWriter(Paths.get(debugXSLFOfile))) {
                            writer.write(debugXSLFO);                    
                    }
                    logger.log(Level.INFO, "XSL-FO file for debugging saved into: {0}", debugXSLFOfile);
                }
                System.exit(ERROR_EXIT_CODE);
            } 
            
        } finally {
            if (out != null) {
                out.close();
            }
        }
        
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
        
        if (isAddAnnotations) {
            logger.log(Level.INFO, "[INFO] Annotation processing...");
            try {
                String xml_review = applyXSLTExtended("xfdf.xsl", sourceXMLDocument.getStreamSource(), xmlIF, false);
                
                debugSaveXML(xml_review, pdf.getAbsolutePath() + ".if.xfdf.xml");
                
                Annotation annotations = new Annotation();
                annotations.process(pdf, xml_review);
            } catch (Exception ex) {
                logger.severe("Can't add annotation.");
                ex.printStackTrace();
            }
        }
        
    }
    
    private Source runSecondPass (String indexxml, Source sourceFO, fontConfig fontcfg, Properties xslparams, XSLTconverter xsltConverter, File pdf)  throws Exception, IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {
        Source src = sourceFO;
        
        File fileXmlIF = new File(indexxml);
        
        long startMethodTime = System.currentTimeMillis();
        
        if (!indexxml.isEmpty() && !fileXmlIF.exists()) { //there is index
             // if file exist - it means that now document by language is processing
            // and don't need to create intermediate file again

            String xmlIF = generateFOPIntermediateFormat(sourceFO, fontcfg.getConfig(), pdf, true, "");

            
            //Util.createIndexFile(indexxml, xmlIF);
            createIndexFile(indexxml, xmlIF);

            if (fileXmlIF.exists()) {
                // pass index.xml path to xslt (for second pass)
                xslparams.setProperty("external_index", fileXmlIF.getAbsolutePath());

                xsltConverter.setParams(xslparams);
            }
            
            System.out.println("[INFO] XSL-FO file preparation (second pass)...");
            // transform XML to XSL-FO (XML .fo file)
            xsltConverter.transform(sourceXMLDocument);

            String xmlFO = sourceXMLDocument.getXMLFO();
            debugXSLFO = xmlFO;
            
            debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".fo.2nd.xml");
            
            src = new StreamSource(new StringReader(xmlFO));
            
        }
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
        return src;
    }
    
    
    private String generateFOPIntermediateFormat(Source src, File fontConfig, File pdf, boolean isSecondPass, String sfx) throws SAXException, IOException, TransformerConfigurationException, TransformerException {
        String xmlIF = "";
        
        long startMethodTime = System.currentTimeMillis();
        
        // run 1st pass to produce FOP Intermediate Format
        FopFactory fopFactory = FopFactory.newInstance(fontConfig);
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
        ifSerializer.setEncoding("UTF-16");
        //Make sure the prepared document handler is used
        userAgent.setDocumentHandlerOverride(ifSerializer);
        if (isSecondPass) {
            userAgent.getEventBroadcaster().addEventListener(new SecondPassSysOutEventListener());
        }
        JEuclidFopFactoryConfigurator.configure(fopFactory);
        
        // Setup output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //out = new java.io.BufferedOutputStream(out);
        //String ifFilename = indexxml + ".if";
        //OutputStream out = new java.io.FileOutputStream(new File(ifFilename));
        try {
            // Construct FOP (the MIME type here is unimportant due to the override
            // on the user agent)
            Fop fop = fopFactory.newFop(null, userAgent, out);

            Result res = new SAXResult(fop.getDefaultHandler());

            // Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer

            transformer.setErrorListener(new DefaultErrorListener());
            if (isSecondPass) {
                System.out.println("[INFO] Rendering into intermediate format ..."); //  for index preparation
            }
            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            xmlIF = out.toString("UTF-16");
            debugXSLFO = xmlIF;
            
            debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if" + sfx + ".xml");

        } finally {
            out.close();
        }
        
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
        
        return xmlIF;
    }
    
    private void createIndexFile(String indexxmlFilePath, String intermediateXML) {
        
        long startMethodTime = System.currentTimeMillis();
        
        try {
            String xmlIndex = applyXSLT("index.xsl", intermediateXML, false);
            
            if (xmlIndex.length() != 0) {
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(indexxmlFilePath))) {
                        writer.write(xmlIndex.toString());                    
                }
            }
        }    
        catch (Exception ex) {
            //System.err.println("Can't save index.xml into temporary folder");
            logger.severe("Can't save index.xml into temporary folder");
            ex.printStackTrace();
        }
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
    }
    
    
    private String createTableIF(String intermediateXML) {
        String xmlTableIF = "";
        long startMethodTime = System.currentTimeMillis();
        try {
            xmlTableIF = applyXSLT("table_if.xsl", intermediateXML, false);
        } catch (Exception ex) {
            logger.severe("Can't generate information about tables from Intermediate Format.");
            ex.printStackTrace();
        }
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
        return xmlTableIF;
    }
    
    
    // Apply XSL tranformation (file xsltfile) for xml string
    private String applyXSLT(String xsltfile, String xmlStr, boolean fixSurrogatePairs) throws Exception {
        
        long startMethodTime = System.currentTimeMillis();
        
        Source srcXSL =  new StreamSource(getStreamFromResources(getClass().getClassLoader(), xsltfile));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(srcXSL);
        if (fixSurrogatePairs) {
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
        }
        Source src = new StreamSource(new StringReader(xmlStr));
        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        transformer.transform(src, sr);
        String xmlResult = resultWriter.toString();
        
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
        
        return xmlResult;
    }
    
    // Apply XSL tranformation (file xsltfile) for the source xml and IF string (parameter 'if_xml')
    private String applyXSLTExtended(String xsltfile, StreamSource sourceXML, String xmlIFStr, boolean fixSurrogatePairs) throws Exception {
        
        long startMethodTime = System.currentTimeMillis();
        
        Source srcXSL =  new StreamSource(getStreamFromResources(getClass().getClassLoader(), xsltfile));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(srcXSL);
        if (fixSurrogatePairs) {
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
        }
        
        // pass Apache FOP Intermediate Format XML via parameter 'if_xml'
        InputSource xmlIFIS = new InputSource(new StringReader(xmlIFStr));
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xmlIFDocument = dBuilder.parse(xmlIFIS);
        NodeList xmlIFDocumentNodeList = xmlIFDocument.getDocumentElement().getChildNodes();
        transformer.setParameter("if_xml", xmlIFDocumentNodeList);
        // ====================================================================
        
        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        transformer.transform(sourceXML, sr);
        String xmlResult = resultWriter.toString();
        
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
        
        return xmlResult;
    }
            
    private class DefaultErrorListener implements javax.xml.transform.ErrorListener {

        public void warning(TransformerException exc) {
            logger.severe(exc.toString());
        }

        public void error(TransformerException exc)
                throws TransformerException {
            throw exc;
        }

        public void fatalError(TransformerException exc)
                throws TransformerException {
            String excstr=exc.toString();
            if (excstr.contains("PDFConformanceException") && excstr.contains("PDF/UA-1") && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                //System.err.println(exc.toString());
                logger.severe(exc.toString());
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
    
    /** A simple event listener that writes the events to stdout and sterr. */
    private static class SecondPassSysOutEventListener implements org.apache.fop.events.EventListener {

        /** {@inheritDoc} */
        public void processEvent(Event event) {
            String msg = EventFormatter.format(event);
            EventSeverity severity = event.getSeverity();
            if (severity == EventSeverity.INFO) {
                if(msg.startsWith("Rendered page #")) {
                    //System.out.println("[INFO] Intermediate format. " + msg);
                    logger.log(Level.INFO, "[INFO] Intermediate format. {0}", msg);
                }
            } else if (severity == EventSeverity.WARN) {
                //System.out.println("[WARN] " + msg);
            } else if (severity == EventSeverity.ERROR) {
                //System.err.println("[ERROR] " + msg);
                logger.log(Level.SEVERE, "[ERROR] {0}", msg);
            } else if (severity == EventSeverity.FATAL) {
                //System.err.println("[FATAL] " + msg);
                logger.log(Level.SEVERE, "[FATAL] {0}", msg);
            } else {
                assert false;
            }
        }
    }
    
    
    private int getPageCount() {
        return pageCount;
    }
    
    private void setEncryptionParams(FOUserAgent userAgent) {
        for (Map.Entry<String, Object> entry : encryptionParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            userAgent.getRendererOptions().put(key, value); // not working for 'encryption-length', see below
        }
        
        if (encryptionParams.containsKey("encryption-length")) {
            PDFEncryptionParams encryptionConfig = new PDFEncryptionParams();
            encryptionConfig.setEncryptionLengthInBits((int)encryptionParams.get("encryption-length"));
            userAgent.getRendererOptions().put("encryption-params", encryptionConfig);
        }
    }
    
    private void readEncryptionParameters(File fEncryptionParameters) {
        Yaml yaml = new Yaml();
        try {
            Map<String, Object> obj = yaml.load(new FileInputStream(fEncryptionParameters));
            
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
            
                switch(key) {
                    case "encryption-length":
                        setEncryptionLength((int)(value));
                        break;
                    case "owner-password":
                        setOwnerPassword((String)value);
                        break;
                    case "user-password":
                        setUserPassword((String)value);
                        break;
                    case "allow-print":
                        setAllowPrint((Boolean)value);
                        break;
                    case "allow-print-hq":
                        setAllowPrintHQ((Boolean)value);
                        break;
                    case "allow-copy-content":
                        setAllowCopyContent((Boolean)value);
                        break;
                    case "allow-edit-content":
                        setAllowEditContent((Boolean)value);
                        break;
                    case "allow-edit-annotations":
                        setAllowEditAnnotations((Boolean)value);
                        break;
                    case "allow-fill-in-forms":
                        setAllowFillInForms((Boolean)value);
                        break;
                    case "allow-access-content":
                        setAllowAccessContent((Boolean)value);
                        break;
                    case "allow-assemble-document":
                        setAllowAssembleDocument((Boolean)value);
                        break;
                    case "encrypt-metadata":
                        setEncryptMetadata((Boolean)value);
                        break;
                    default:
                        logger.log(Level.INFO, "Unknown key in encryption parameters file: {0}", key);
                        break;
                }
            }
        } catch (FileNotFoundException ex) {
            // make no sense, checking in main method
        } catch (Exception ex) {
            logger.log(Level.INFO, "ERROR: Error in processing encryption parameters file: {0}", ex.toString());
            logger.info("Expected format:");
            logger.info("encryption-length: 128");
            logger.info("owner-password: mypass");
            logger.info("user-password: userpass");
            logger.info("allow-print: false");
            logger.info("allow-print-hq: true");
            logger.info("allow-copy-content: true");
            logger.info("allow-edit-content: false");
            logger.info("allow-edit-annotations: true");
            logger.info("allow-fill-in-forms: false");
            logger.info("allow-access-content: true");
            logger.info("allow-assemble-document: false");
            logger.info("encrypt-metadata: true");
            System.exit(ERROR_EXIT_CODE);
        }
    }
    
    private void setTablesWidths(fontConfig fontcfg, XSLTconverter xsltConverter, File pdf) {
        long startMethodTime = System.currentTimeMillis();
        try {
            if (isTableExists && xmlTableIF.isEmpty()) { 
                // generate IF with table width data
                xsltConverter.setParam("table_if", "true");
                logger.info("[INFO] Generation of XSL-FO with information about the table's widths ...");
                // transform XML to XSL-FO (XML .fo file)
                startTime = System.currentTimeMillis();
                xsltConverter.transform(sourceXMLDocument);
                
                printProcessingTime();
                
                String xmlFO = sourceXMLDocument.getXMLFO();
                
                
                debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".fo.tables.xml");
                
                fontcfg.setSourceDocumentFontList(sourceXMLDocument.getDocumentFonts());

                Source sourceFO = new StreamSource(new StringReader(xmlFO));
                logger.info("[INFO] Generation of Intermediate Format with information about the table's widths ...");
                String xmlIF = generateFOPIntermediateFormat(sourceFO, fontcfg.getConfig(), pdf, true, ".tables");

                xmlTableIF = createTableIF(xmlIF);
                
                debugSaveXML(xmlTableIF, pdf.getAbsolutePath() + ".tables.xml");
                
                xsltConverter.setParam("table_if", "false");
                logger.info("[INFO] Generated successfully!");
            }
            if (!xmlTableIF.isEmpty()) {
                // pass Table widths XML via parameter 'if_xml'
                logger.info("[INFO] Generation XML with table's widths ...");
                InputSource xmlTableIS = new InputSource(new StringReader(xmlTableIF));
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document xmlTableDocument = dBuilder.parse(xmlTableIS);
                NodeList xmlTableDocumentNodeList = xmlTableDocument.getDocumentElement().getChildNodes();
                xsltConverter.setParam("table_widths", xmlTableDocumentNodeList);
                logger.info("[INFO] Generated successfully!");
                // ====================================================================
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can''t obtain table's widths information: {0}", e.toString());
        }
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod().getName(), startMethodTime);
    }
    
    private void debugSaveXML(String xmlString, String pathTo) {
        try {
            if (DEBUG) {
                //DEBUG: write table width information to file                
                String xmlString_UTF8 = xmlString.replace("<?xml version=\"1.0\" encoding=\"UTF-16\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(pathTo))) {
                        writer.write(xmlString_UTF8);                    
                }
                //Setup output
                //OutputStream outstream = new java.io.FileOutputStream(pdf.getAbsolutePath() + ".fo.xml");
                //Resulting SAX events (the generated FO) must be piped through to FOP
                //Result res = new StreamResult(outstream);
                //Start XSLT transformation and FO generating
                //transformer.transform(src, res);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Can't save debug xml file '{0}': {1}", new Object[]{pathTo, ex.toString()});
        }
    }
    
    private void printProcessingTime() {
        if (DEBUG) {
            long endTime = System.currentTimeMillis();
            logger.log(Level.INFO, "processing time: {0} milliseconds", endTime - startTime);
        }
    }
    
    private void printProcessingTime(String methodName, long startTime) {
        if (DEBUG) {
            long endTime = System.currentTimeMillis();
            logger.log(Level.INFO, methodName + "(...) processing time: {0} milliseconds", endTime - startTime);
        }
    }
}
