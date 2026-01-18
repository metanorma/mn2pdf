package org.metanorma.fop;

import org.metanorma.fop.annotations.Annotation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import net.sourceforge.jeuclid.fop.plugin.JEuclidFopFactoryConfigurator;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFParser;
import org.apache.fop.render.intermediate.IFSerializer;
import org.apache.fop.render.intermediate.IFUtil;
import static org.metanorma.Constants.*;
import static org.metanorma.fop.fontConfig.DEFAULT_FONT_PATH;
import static org.metanorma.fop.Util.getStreamFromResources;

import org.metanorma.fop.annotations.FileAttachmentAnnotation;
import org.metanorma.fop.eventlistener.LoggingEventListener;
import org.metanorma.fop.eventlistener.SecondPassSysOutEventListener;
import org.metanorma.fop.form.FormItem;
import org.metanorma.fop.ifhandler.*;
import org.metanorma.fop.portfolio.PDFMetainfo;
import org.metanorma.fop.portfolio.PDFPortfolio;
import org.metanorma.fop.portfolio.PDFPortfolioItem;
import org.metanorma.fop.signature.PDFSign;
import org.metanorma.fop.tags.TableCaption;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.Node;
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

    String sourceDocumentFilePath = "";

    final private String inputXSLFilePath;

    private String inputXSLoverrideFilePath;

    private boolean isPDFPortfolio = false;

    private KeyStore keystore;
    private String keystoreFilename;
    private String keystorePassword;

    final private String outputPDFFilePath;
    
    //private boolean isDebugMode = false;
    
    private boolean isSkipPDFGeneration = false;
    
    private boolean isSplitByLanguage = false;
    
    private boolean isAddMathAsText = false;
    
    private boolean isAddLineNumbers = false;

    private boolean isAddCommentaryPageNumbers = false;

    private boolean isAddForms = false;
    
    private boolean isAddMathAsAttachment = false;

    private boolean isApplyAutolayoutAlgorithm = true;

    private boolean isComplexScriptsFeatures = true;

    private boolean isAddAnnotations = false;
    private boolean isAddFileAttachmentAnnotations = false;
    
    private boolean isTableExists = false;
    
    private String xmlTableIF = "";
    
    private Properties xsltParams = new Properties();
    
    private String encryptionParametersFile = "";
    
    private Map<String,Object> encryptionParams = new HashMap<>();
    
    private boolean isSyntaxHighlight = false;
    
    int pageCount = 0;
    
    boolean PDFUA_error = false;

    boolean PDFUA_enabled = true;
    boolean PDFA_enabled = true;

    private String debugXSLFO = "";

    public void setInputXSLoverrideFilePath(String inputXSLoverrideFilePath) {
        this.inputXSLoverrideFilePath = inputXSLoverrideFilePath;
    }

    public void setPDFPortfolio(boolean PDFPortfolio) {
        isPDFPortfolio = PDFPortfolio;
    }

    public void setKeystore(String keystoreFilename) {
        this.keystoreFilename = keystoreFilename;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

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

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

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

            File fXSLoverride = null;
            if (inputXSLoverrideFilePath != null && !inputXSLoverrideFilePath.isEmpty()) {
                fXSLoverride = new File(inputXSLoverrideFilePath);
                if (!fXSLoverride.exists()) {
                    logger.severe(String.format(INPUT_NOT_FOUND, XSL_INPUT, fXSLoverride));
                    return false;
                }
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

            PDFResult pdfResult = PDFResult.PDFResult(fPDF);

            sourceDocumentFilePath = fXML.getParent();
            if (sourceDocumentFilePath == null) {
                sourceDocumentFilePath = System.getProperty("user.dir");
            }

            if (keystoreFilename != null && !keystoreFilename.isEmpty()) {
                File fKeystore = new File(keystoreFilename);
                if (!fKeystore.toPath().isAbsolute()) { // if keystore path is relative
                    fKeystore = Paths.get(getBasePath(), keystoreFilename).toAbsolutePath().toFile();
                }
                if (!fKeystore.exists()) {
                    logger.severe(String.format(INPUT_NOT_FOUND, "Keystore file", fKeystore));
                    return false;
                }
                // load the keystore
                keystore = KeyStore.getInstance("PKCS12");
                char[] password = keystorePassword.toCharArray();
                try (InputStream is = new FileInputStream(fKeystore.getAbsolutePath()))
                {
                    keystore.load(is, password);
                }
            }

            List<PDFPortfolioItem> pdfPortfolioItems = new ArrayList<>();
            String portfolioAuthor = "";

            //File fPresentationPartXML = getPresentationPartXML(fXML, fPDF.getParent());
            List<PDFMetainfo> listPresentationParts = getPresentationPartsFromXML(fXML, pdfResult);
            for (PDFMetainfo entry : listPresentationParts)
            {
                File fPresentationPartXML = new File(entry.getXmlFilePath());

                if (isPDFPortfolio) {
                    Path pdfItemPath = Paths.get(fPDF.getAbsoluteFile().getParent(), entry.getPDFFileName());
                    fPDF = pdfItemPath.toFile();
                    // To do?: if PDF exists, it means that PDF generated already
                    // and no need to generate it again
                    // just add to the pdf list for PDF Portfolio generation
                    /*if (fPDF.exists()) {
                        pdfFilesMap.put(fPDF.getAbsolutePath(), entry.getDocumentIdentifier());
                        continue;
                    }*/
                }

                sourceXMLDocument = new SourceXMLDocument(fPresentationPartXML);

                isAddAnnotations = sourceXMLDocument.hasAnnotations();
                isAddFileAttachmentAnnotations = sourceXMLDocument.hasFileAttachmentAnnotations();
                isTableExists = sourceXMLDocument.hasTables();
                isAddForms = sourceXMLDocument.hasForms();
                boolean isMathExists = sourceXMLDocument.hasMath();

                XSLTconverter xsltConverter = new XSLTconverter(fXSL, fXSLoverride, sourceXMLDocument.getPreprocessXSLT(), fPDF.getAbsolutePath());

                isAddMathAsText = xsltConverter.hasParamAddMathAsText() && isMathExists;
                isAddMathAsAttachment = xsltConverter.hasParamAddMathAsAttachment();

                isApplyAutolayoutAlgorithm = xsltConverter.isApplyAutolayoutAlgorithm();

                isComplexScriptsFeatures = !xsltConverter.isIgnoreComplexScripts();

                if (isSyntaxHighlight) {
                    xsltParams.put("syntax-highlight", "true");
                }
                xsltConverter.setParams(xsltParams);

                fontConfig fontcfg = new fontConfig();
                fontcfg.setFontPath(fontsPath);
                fontcfg.setFontConfigPath(fPDF.getAbsolutePath());
                fontcfg.setFontManifest(fFontsManifest);
                fontcfg.saveFontManifest(fPDF.getParent()); // for debug purposes

                //debug
                fontcfg.outputFontManifestLog(Paths.get(fPDF.getAbsolutePath() + ".fontmanifest.log.txt"));

                convertmn2pdf(fontcfg, xsltConverter, fPDF);

                pdfPortfolioItems.add(
                        new PDFPortfolioItem(fPDF.getAbsolutePath(), entry.getDocumentIdentifier(), true)
                );

                if (isSplitByLanguage) {
                    int initial_page_number = 1;
                    int coverpages_count = Util.getCoverPagesCount(fXSL);
                    //determine how many documents in source XML
                    ArrayList<String> languages = sourceXMLDocument.getLanguagesList();
                    for (int i = 0; i < languages.size(); i++) {
                        if (i >= 1) {
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

                        pdfPortfolioItems.add(
                                new PDFPortfolioItem(fPDFsplit.getAbsolutePath(), fPDFsplit.getName(), true)
                        );

                        // initial page number for 'next' document
                        initial_page_number = (getPageCount() - coverpages_count) + 1;
                    }
                }

                // flush temporary folder
                if (!DEBUG) {
                    if (!fPresentationPartXML.getAbsolutePath().equals(fXML.getAbsolutePath())) {
                        try {
                            Files.deleteIfExists(fPresentationPartXML.toPath());
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                        }
                    }
                    xsltConverter.deleteTmpXSL();
                    fontcfg.deleteConfigFile();
                }

            }

            if (isPDFPortfolio) {
                PDFPortfolio pdfPortfolio = new PDFPortfolio(pdfPortfolioItems);
                pdfPortfolio.setAuthor(portfolioAuthor); // To do
                pdfPortfolio.setDefaultPDFFilename(fXML);
                pdfPortfolio.generate(outputPDFFilePath);
                if (!DEBUG) {
                    pdfPortfolio.flushTempPDF();
                }

            }

            if (keystoreFilename != null && !keystoreFilename.isEmpty()) {
                // sign PDF
                char[] password = keystorePassword.toCharArray();
                PDFSign signing = new PDFSign(keystore, password);
                File inFile = new File(outputPDFFilePath);
                File outFile = new File(outputPDFFilePath + ".signed.pdf");
                signing.signDetached(inFile, outFile);
                Files.move(outFile.toPath(), inFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // flush temporary folder
            if (!DEBUG) {
                sourceXMLDocument.flushTempPath();
                pdfResult.flushOutTmpImagesFolder();
            }
            
            logger.info("Success!");
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
        Profiler.removeMethodCall();
        return true;
    }

    private String getBasePath() {
        String basepath = sourceDocumentFilePath + File.separator;
        // redefine basepath
        if (xsltParams.containsKey("baseassetpath")) {
            basepath = xsltParams.getProperty("baseassetpath") + File.separator;
        }
        return basepath;
    }

    private List<PDFMetainfo> getPresentationPartsFromXML(File fXML, PDFResult pdfResult) {
        List<PDFMetainfo> listPDFMetaInfo = new ArrayList<>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FOPXMLPresentationHandler fopXMLPresentationHandler = new FOPXMLPresentationHandler();
            String sourceXML = Util.readFile(fXML);
            InputSource inputSource = new InputSource( new StringReader(sourceXML));
            saxParser.parse(inputSource, fopXMLPresentationHandler);
            StringBuilder resultedXML = fopXMLPresentationHandler.getResultedXML();
            String outputFolder = pdfResult.getOutFolder();
            File outputFile = Paths.get(outputFolder, fXML.getName() + "_tmp").toFile();

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                writer.write(resultedXML.toString());
            }
            if (isPDFPortfolio) {
                // split collection XML into separate documents

                // iterate each //entry with @target
                //   @target points to the /metanorma
                //   @pdf-file is the output file name
                //   if missing, then concatenate docidentifier + _ + bibdata/docidentifier + ".pdf"
                //   extract //doc-container[@id=@target]/metanorma and save to temp xml file

                InputSource xmlPresentationIS = new InputSource(new StringReader(resultedXML.toString()));
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document xmlPresentationDocument = dBuilder.parse(xmlPresentationIS);

                XPath xPathEntries = XPathFactory.newInstance().newXPath();
                XPathExpression queryAllEntries = xPathEntries.compile("//*[local-name() = 'metanorma-collection']//*[local-name() = 'entry'][@target]");
                NodeList nodesEntries = (NodeList)queryAllEntries.evaluate(xmlPresentationDocument, XPathConstants.NODESET);
                for (int i = 0; i < nodesEntries.getLength(); i++) {
                    Node nodeEntry = nodesEntries.item(i);

                    String target = "";
                    Node nodeTarget = nodeEntry.getAttributes().getNamedItem("target");
                    if (nodeTarget != null) {
                        target = nodeTarget.getTextContent();
                    }

                    String pdffile = "";
                    Node nodePdfFile = nodeEntry.getAttributes().getNamedItem("pdf-file");
                    if (nodePdfFile != null) {
                        pdffile = nodePdfFile.getTextContent();
                    }

                    String docidentifier = "";
                    XPath xPathDocidentifier = XPathFactory.newInstance().newXPath();
                    XPathExpression queryDocidentifier = xPathDocidentifier.compile("./*[local-name() = 'bibdata']/*[local-name() = 'docidentifier'][1]");
                    Node nodeDocidentifier = (Node) queryDocidentifier.evaluate(nodeEntry, XPathConstants.NODE);
                    if (nodeDocidentifier != null) {
                        docidentifier = nodeDocidentifier.getTextContent();
                    }

                    if (pdffile.isEmpty()) {
                        String entry_identifier = "";
                        XPath xPathIdentifier = XPathFactory.newInstance().newXPath();
                        XPathExpression queryIdentifier = xPathIdentifier.compile("./*[local-name() = 'identifier'][1]");
                        Node nodeIdentifier = (Node) queryIdentifier.evaluate(nodeEntry, XPathConstants.NODE);
                        if (nodeIdentifier != null) {
                            entry_identifier = nodeIdentifier.getTextContent();
                        }
                        pdffile = entry_identifier + "_" + docidentifier;
                        String restrictedCharsRegex = "[\\\\/:*?\"<>|]";
                        pdffile = pdffile.replaceAll(restrictedCharsRegex, "_") + ".pdf";
                    }

                    // extract //doc-container[@id=@target]/metanorma and save to temp xml file
                    if (!target.isEmpty()) {
                        XPath xPathDocument = XPathFactory.newInstance().newXPath();
                        XPathExpression queryDocument = xPathDocument.compile("//*[local-name() = 'doc-container'][@id = '" + target + "'][1]/*[1]");
                        Node nodeDocument = (Node) queryDocument.evaluate(xmlPresentationDocument, XPathConstants.NODE);
                        if (nodeDocument != null ){

                            DOMSource source = new DOMSource(nodeDocument);

                            File outputFilePart = Paths.get(outputFolder, fXML.getName() + "_" + target + "_tmp").toFile();

                            StringWriter writer = new StringWriter();
                            StreamResult srPart = new StreamResult(writer);
                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                            Transformer transformer = transformerFactory.newTransformer();
                            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
                            try {
                                transformer.transform(source, srPart);
                                String xmlPartString = writer.getBuffer().toString();

                                try (OutputStreamWriter outwriter = new OutputStreamWriter(new FileOutputStream(outputFilePart), StandardCharsets.UTF_8)) {
                                    outwriter.write(xmlPartString);
                                }
                                if (DEBUG) {
                                    logger.info("XML saved to " + outputFilePart.getAbsolutePath());
                                }
                                PDFMetainfo pdfMetainfo =
                                        new PDFMetainfo(outputFilePart.getAbsolutePath(), pdffile, docidentifier);
                                listPDFMetaInfo.add(pdfMetainfo);
                            } catch (TransformerException e) {
                                logger.severe("Can't save the document from document-collection.");
                                e.printStackTrace();
                            } finally {
                                try {
                                    writer.close(); // Close the FileWriter
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                if (listPDFMetaInfo.isEmpty()) {
                    PDFMetainfo pdfMetainfo =
                            new PDFMetainfo(outputFile.getAbsolutePath(), pdfResult.getPDFFilename(), pdfResult.getPDFFilename());
                    listPDFMetaInfo.add(pdfMetainfo);
                } else {
                    // no need to save XML outputFile
                    if (!DEBUG) {
                        if (!outputFile.getAbsolutePath().equals(fXML.getAbsolutePath())) {
                            try {
                                Files.deleteIfExists(outputFile.toPath());
                            } catch (IOException e) {
                                e.printStackTrace(System.err);
                            }
                        }
                    }
                }

            } else {
                PDFMetainfo pdfMetainfo =
                        new PDFMetainfo(outputFile.getAbsolutePath(), pdfResult.getPDFFilename(), pdfResult.getPDFFilename());
                listPDFMetaInfo.add(pdfMetainfo);
            }
        }
        catch (Exception ex) {
            logger.severe("Can't obtain the presentation part of the XML:");
            logger.severe(ex.getMessage());
            ex.printStackTrace();
            PDFMetainfo pdfMetainfo =
                    new PDFMetainfo(fXML.getAbsolutePath(), outputPDFFilePath, "");
            listPDFMetaInfo.add(pdfMetainfo);
        }
        finally {
            return listPDFMetaInfo;
        }
    }


    
    /**
     * Converts an XML file to a PDF file using FOP
     *
     * @param fontcfg the FOP config file
     * @param xsltConverter the XSL converter
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException, SAXException In case of a FOP problem
     */
    private void convertmn2pdf(fontConfig fontcfg, XSLTconverter xsltConverter, File pdf) throws IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

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

            additionalXSLTparams.setProperty("basepath", getBasePath());

            File fInputXML = new File(inputXMLFilePath);
            String fInputXMLParent = fInputXML.getAbsoluteFile().getParent() + File.separator;
            String fOutputPDFParent = pdf.getAbsoluteFile().getParent() + File.separator;
           
            additionalXSLTparams.setProperty("inputxml_basepath", fInputXMLParent);
            additionalXSLTparams.setProperty("inputxml_filename", fInputXML.getName());

            additionalXSLTparams.setProperty("output_path", pdf.getAbsolutePath());
            additionalXSLTparams.setProperty("outputpdf_basepath", fOutputPDFParent);

            xsltConverter.setParams(additionalXSLTparams);
            
            setTablesWidths(fontcfg, xsltConverter, pdf);
            
            logger.info("[INFO] XSL-FO file preparation...");
            
            // transform XML to XSL-FO (XML .fo file)
            if (shouldCreateIFFile(indexxml)) {
                // IF file will be created later in runSecondPass, so no need to set "final_transform" = true (i.e. attach embedded files)
                xsltConverter.transform(sourceXMLDocument, false);
            } else {
                xsltConverter.transform(sourceXMLDocument);
            }

            String xmlFO = sourceXMLDocument.getXMLFO();
            saveDebugFO(xmlFO);
            
            String add_line_numbers = Util.readValueFromXMLString(xmlFO, "/*[local-name() = 'root']/processing-instruction('add_line_numbers')");
            isAddLineNumbers = add_line_numbers.equalsIgnoreCase("true");

            String add_commentary_page_numbers = Util.readValueFromXMLString(xmlFO, "//*[@id = '_independent_page_number_commentary']/@id");
            isAddCommentaryPageNumbers = !add_commentary_page_numbers.isEmpty();

            debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".fo.xml");
            
            fontcfg.setSourceDocumentFontList(sourceXMLDocument.getDocumentFonts());

            fontcfg.setComplexScriptFeatures(isComplexScriptsFeatures);

            Source src = new StreamSource(new StringReader(xmlFO));
            
            
            src = runSecondPass (indexxml, src, fontcfg, additionalXSLTparams, xsltConverter, pdf);
            
            
            // FO processing by FOP
            
            //src = new StreamSource(new StringReader(xmlFO));
            
            runFOP(fontcfg, src, pdf);
            
            if(PDFUA_error) {
                logger.warning("WARNING: Trying to generate PDF in non " + PDF_UA_MODE + " and non " + PDF_A_MODE + " modes.");
                fontcfg.setPDFUAmode("DISABLED");
                PDFUA_enabled = false;
                PDFA_enabled = false;
                src = new StreamSource(new StringReader(xmlFO));
                runFOP(fontcfg, src, pdf);
                logger.warning(WARNING_NONPDFUA);
            }

            // validate PDF by veraPDF
            /*VeraPDFValidator veraPDFValidator = new VeraPDFValidator();
            if (PDFA_enabled) {
                veraPDFValidator.validate(pdf, PDF_A_MODE);
            }
            if (PDFUA_enabled) {
                veraPDFValidator.validate(pdf, PDF_UA_MODE);
            }*/

            fontcfg.printMessages();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }

        Profiler.removeMethodCall();
    }
    
    
    private void runFOP (fontConfig fontcfg, Source src, File pdf) throws IOException, FOPException, SAXException, TransformerException {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        PDFUA_error = false;

        OutputStream out = null;
        String xmlIF = null;
        long startMethodTime = System.currentTimeMillis();
        try {
            
            String mime = MimeConstants.MIME_PDF;

            boolean isPostprocessing = isAddMathAsText ||
                    isAddAnnotations ||
                    isAddLineNumbers ||
                    isAddCommentaryPageNumbers ||
                    isAddForms;

            if (isPostprocessing) {
                logger.info("Starting post-processing...");

                // release memory resources
                sourceXMLDocument.flushResources();
                xmlTableIF = "";

                logger.info("Transforming to Intermediate Format...");
                xmlIF = generateFOPIntermediateFormat(src, fontcfg.getConfig(), pdf, false, "");

                src = null;

                if (isAddMathAsText) {
                    logger.info("Updating Intermediate Format (adding hidden math)...");
                    //xmlIF = applyXSLT("add_hidden_math.xsl", xmlIF, true);
                    xmlIF = addHiddenMath(xmlIF);

                    saveDebugFO(xmlIF);
                
                    debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if.mathtext.xml");
                }
                
                
                if (isAddLineNumbers) {
                    logger.info("Updating Intermediate Format (adding line numbers)...");
                    xmlIF = applyXSLT("add_line_numbers.xsl", xmlIF, true);

                    saveDebugFO(xmlIF);
                
                    debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if.linenumbers.xml");
                }

                if (isAddCommentaryPageNumbers) {
                    logger.info("Updating Intermediate Format (adding commentary pages)...");
                    xmlIF = applyXSLT("add_commentary_page_numbers.xsl", xmlIF, true);

                    saveDebugFO(xmlIF);

                    debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if.commentarypagenumbers.xml");
                }

                if (isAddForms) {
                    logger.info("Read Forms information from Intermediate Format...");


                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    FOPIFFormsHandler fopIFFormsHandler = new FOPIFFormsHandler();
                    InputSource srcIntermediateXML = new InputSource(new StringReader(xmlIF));
                    saxParser.parse(srcIntermediateXML, fopIFFormsHandler);

                    //String xmlIFForm = applyXSLT("forms_if.xsl", xmlIF, true);

                    xmlIF = fopIFFormsHandler.getResultedXML();
                    List<FormItem> formItems =  fopIFFormsHandler.getFormsItems();

                    debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if.forms.xml");
                }
                
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
            //foUserAgent.setProducer("Ribose Metanorma mn2pdf version " + Util.getAppVersion());
            foUserAgent.setProducer(Util.getPDFProducer());

            if (encryptionParams.isEmpty()) {
                if (PDFA_enabled) {
                    foUserAgent.getRendererOptions().put("pdf-a-mode", PDF_A_MODE);
                }
            } else {
                logger.severe("PDF/A doesn't allow encrypted PDFs. PDF will be generated in non-PDF/A mode.");
            }

            setEncryptionParams(foUserAgent);
            
            //Adding a simple logging listener that writes to stdout and stderr            
            //foUserAgent.getEventBroadcaster().addEventListener(new SysOutEventListener());
            // Add your own event listener
            //foUserAgent.getEventBroadcaster().addEventListener(new MyEventListener());

            foUserAgent.getEventBroadcaster().addEventListener(new LoggingEventListener());

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);
            
            if (isPostprocessing) { // process IF to PDF
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
                    this.pageCount = getIFPageCount(xmlIF);
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
            if (PDFConformanceChecker.hasException(e.toString()) && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
                //System.err.println(e.toString());
                logger.severe(e.toString());
                PDFUA_error = true;
            } else {
                //e.printStackTrace(System.err);
                logger.log(Level.SEVERE,e.getMessage(), e);
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


        if (PDFUA_error == false) {
            logger.log(Level.INFO, "[INFO] Table Caption tag processing...");
            TableCaption tableCaption = new TableCaption();
            tableCaption.process(pdf);
        }

        
        if (isAddAnnotations && PDFUA_error == false) {
            logger.log(Level.INFO, "[INFO] Annotation processing...");
            try {

                String xmlIFflat = flatIFforXFDF(xmlIF);

                debugSaveXML(xmlIFflat, pdf.getAbsolutePath() + ".if.flat.xfdf.xml");

                String xml_review = applyXSLTExtended("xfdf_simple.xsl", sourceXMLDocument.getStreamSource(), xmlIFflat, false);

                debugSaveXML(xml_review, pdf.getAbsolutePath() + ".if.xfdf.xml");
                
                Annotation annotations = new Annotation();
                annotations.process(pdf, xml_review);
            } catch (Exception ex) {
                logger.severe("Can't add annotation (" + ex.toString() + ").");
                ex.printStackTrace();
            }
        }

        if (isAddFileAttachmentAnnotations && PDFUA_error == false) {
            logger.log(Level.INFO, "[INFO] File attachment annotation processing...");
            try {
                FileAttachmentAnnotation annotations = new FileAttachmentAnnotation();
                annotations.process(pdf);
            } catch (Exception ex) {
                logger.severe("Can't process file attachment annotation (" + ex.toString() + ").");
                ex.printStackTrace();
            }
        }

        Profiler.printProcessingTime(methodName, startMethodTime);
        Profiler.removeMethodCall();
    }
    
    private Source runSecondPass (String indexxml, Source sourceFO, fontConfig fontcfg, Properties xslparams, XSLTconverter xsltConverter, File pdf)  throws Exception, IOException, FOPException, SAXException, TransformerException, ParserConfigurationException {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        Source src = sourceFO;
        
        File fileXmlIF = new File(indexxml);
        
        long startMethodTime = System.currentTimeMillis();
        
        if (shouldCreateIFFile(indexxml)) { //there is index
             // if file exist - it means that now document by language is processing
            // and don't need to create intermediate file again

            String xmlIF = generateFOPIntermediateFormat(sourceFO, fontcfg.getConfig(), pdf, true, "");

            createIndexFile(indexxml, xmlIF, pdf);

            if (fileXmlIF.exists()) {
                // pass index.xml path to xslt (for second pass)
                xslparams.setProperty("external_index", fileXmlIF.getAbsolutePath());

                xsltConverter.setParams(xslparams);
            }
            
            System.out.println("[INFO] XSL-FO file preparation (second pass)...");
            // transform XML to XSL-FO (XML .fo file)
            xsltConverter.transform(sourceXMLDocument);

            String xmlFO = sourceXMLDocument.getXMLFO();
            saveDebugFO(xmlFO);
            
            debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".fo.2nd.xml");
            
            src = new StreamSource(new StringReader(xmlFO));
            
        }
        Profiler.printProcessingTime(methodName, startMethodTime);
        Profiler.removeMethodCall();
        return src;
    }

    private boolean shouldCreateIFFile(String indexxml) {
        return !indexxml.isEmpty() && !(new File(indexxml)).exists();
    }
    
    private String generateFOPIntermediateFormat(Source src, File fontConfig, File pdf, boolean isSecondPass, String sfx) throws SAXException, IOException, TransformerConfigurationException, TransformerException {

        long startMethodTime = System.currentTimeMillis();
        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        String xmlIF = "";

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
        } else {
            userAgent.getEventBroadcaster().addEventListener(new LoggingEventListener());
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
            saveDebugFO(xmlIF);
            
            debugSaveXML(xmlIF, pdf.getAbsolutePath() + ".if" + sfx + ".xml");

        } finally {
            out.close();
        }

        Profiler.printProcessingTime(methodName, startMethodTime);
        Profiler.removeMethodCall();

        return xmlIF;
    }
    
    private void createIndexFile(String indexxmlFilePath, String intermediateXML, File pdf) {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        long startMethodTime = System.currentTimeMillis();
        
        try {
            //String xmlIndex = applyXSLTC("index.xsl", intermediateXML, false);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FOPIFIndexHandler fopIFIndexHandler = new FOPIFIndexHandler();
            InputSource srcIntermediateXML = new InputSource(new StringReader(intermediateXML));
            saxParser.parse(srcIntermediateXML, fopIFIndexHandler);

            String xmlIndex = fopIFIndexHandler.getIndexItems();

            if (xmlIndex.length() != 0) {
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(indexxmlFilePath))) {
                        writer.write(xmlIndex.toString());                    
                }
            }
            debugSaveXML(xmlIndex, pdf.getAbsolutePath() + ".index.xml");
        }    
        catch (Exception ex) {
            //System.err.println("Can't save index.xml into temporary folder");
            logger.severe("Can't save index.xml into temporary folder");
            ex.printStackTrace();
        }
        Profiler.printProcessingTime(methodName, startMethodTime);
        Profiler.removeMethodCall();
    }
    
    private String addHiddenMath(String sourceXML) {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        long startMethodTime = System.currentTimeMillis();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FOPIFHiddenMathHandler fopIFHiddenMathHandler = new FOPIFHiddenMathHandler();
            InputSource inputSource = new InputSource( new StringReader(sourceXML));
            saxParser.parse(inputSource, fopIFHiddenMathHandler);
            sourceXML = null;
            //StringBuilder result = fopIFHiddenMathHandler.getResultedXML();
            String result = fopIFHiddenMathHandler.getResultedXML();
            //System.out.println("result string length: " + result.length());
            //System.out.println("result string capacity: " + result.capacity());
            fopIFHiddenMathHandler = null;
            Profiler.printProcessingTime(methodName, startMethodTime);
            Profiler.removeMethodCall();
            return result.toString();
        }
        catch (Exception ex) {
            logger.severe("Can't update IF for hidden math.");
            ex.printStackTrace();
        }
        Profiler.removeMethodCall();
        return sourceXML;
    }

    private String flatIFforXFDF(String sourceXML) {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        long startMethodTime = System.currentTimeMillis();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FOPIFFlatHandler fopIFFlatHandler = new FOPIFFlatHandler();
            InputSource inputSource = new InputSource( new StringReader(sourceXML));
            saxParser.parse(inputSource, fopIFFlatHandler);
            String result = fopIFFlatHandler.getResultedXML();
            Profiler.printProcessingTime(methodName, startMethodTime);
            Profiler.removeMethodCall();
            return result;
        }
        catch (Exception ex) {
            logger.severe("Can't flat IF.");
            ex.printStackTrace();
        }
        Profiler.removeMethodCall();
        return sourceXML;
    }

    private String createTableIF(String intermediateXML) {

        logger.info("[INFO] Processing of Intermediate Format with information about the table's widths (table_if.xsl) ...");

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

        String xmlTableIF = "";
        long startMethodTime = System.currentTimeMillis();
        try {
            xmlTableIF = applyXSLT("table_if.xsl", intermediateXML, false);
        } catch (Exception ex) {
            logger.severe("Can't generate information about tables from Intermediate Format.");
            ex.printStackTrace();
        }
        Profiler.printProcessingTime(methodName, startMethodTime);
        Profiler.removeMethodCall();
        return xmlTableIF;
    }
    
    
    // Apply XSL tranformation (file xsltfile) for xml string
    /*private String applyXSLT(String xsltfile, String xmlStr, boolean fixSurrogatePairs) throws Exception {
        String xmlTableIF = "";
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
        
        printProcessingTime(new Object(){}.getClass().getEnclosingMethod(), startMethodTime);
        
        return xmlResult;
    }*/
    
    // Apply XSL tranformation (file xsltfile) for XML String or StreamSource
    private String applyXSLT(String xsltfile, Object sourceXML, boolean fixSurrogatePairs) throws Exception {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);
        long startMethodTime = System.currentTimeMillis();
        
        Source srcXSL =  new StreamSource(getStreamFromResources(getClass().getClassLoader(), xsltfile));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(srcXSL);
        if (fixSurrogatePairs) {
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
        }
        
        Source src = (sourceXML instanceof StreamSource) ? (StreamSource)sourceXML : new StreamSource(new StringReader((String)sourceXML));
        
        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        transformer.transform(src, sr);
        String xmlResult = resultWriter.toString();

        Profiler.printProcessingTime(methodName, startMethodTime, xsltfile);
        Profiler.removeMethodCall();

        return xmlResult;
    }

    // Apply XSL tranformation (file xsltfile) for XML String or StreamSource, by using Compiling processor
    // XSLT should be simple, without extension function
    /*private String applyXSLTC(String xsltfile, Object sourceXML, boolean fixSurrogatePairs) throws Exception {
        long startMethodTime = System.currentTimeMillis();

        String key = "javax.xml.transform.TransformerFactory";
        String value_old = System.getProperty(key);
        String value_new = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";

        System.setProperty(key, value_new);

        Source srcXSL =  new StreamSource(getStreamFromResources(getClass().getClassLoader(), xsltfile));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(srcXSL);
        if (fixSurrogatePairs) {
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
        }

        Source src = (sourceXML instanceof StreamSource) ? (StreamSource)sourceXML : new StreamSource(new StringReader((String)sourceXML));

        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        transformer.transform(src, sr);
        String xmlResult = resultWriter.toString();

        if (value_old != null && !value_old.isEmpty()) {
            // restore previous value
            System.setProperty(key, value_old);
        } else {
            System.clearProperty(key);
        }

        printProcessingTime(new Object(){}.getClass().getEnclosingMethod(), startMethodTime, xsltfile);

        return xmlResult;
    }*/

    // Apply XSL tranformation (file xsltfile) for the source xml and IF string (parameter 'if_xml')
    private String applyXSLTExtended(String xsltfile, StreamSource sourceXML, String xmlIFStr, boolean fixSurrogatePairs) throws Exception {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);

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

        Profiler.printProcessingTime(methodName, startMethodTime, xsltfile);
        Profiler.removeMethodCall();

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
            if (PDFConformanceChecker.hasException(exc.toString()) && !PDFUA_error) { // excstr.contains("all fonts, even the base 14 fonts, have to be embedded")
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

        int TABLE_CELLS_COUNT_MAX = 10000;//30000; 20000

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);
        long startMethodTime = System.currentTimeMillis();

        try {
            if (isTableExists && xmlTableIF.isEmpty() && isApplyAutolayoutAlgorithm) {
                // generate IF with table width data
                xsltConverter.setParam("table_if", "true");
                logger.info("[INFO] Generation of XSL-FO with information about the table's widths ...");
                
                String xmlTablesOnly = "";
                try {
                    xmlTablesOnly = applyXSLT("tables_only.xsl", sourceXMLDocument.getStreamSource(), true);
                } catch (Exception ex) {
                    logger.severe("Can't generate information about tables from Intermediate Format.");
                    ex.printStackTrace();
                }

                debugSaveXML(xmlTablesOnly, pdf.getAbsolutePath() + ".tablesonly.xml");

                SourceXMLDocument sourceXMLDocumentTablesOnly = new SourceXMLDocument(xmlTablesOnly);

                int countTableCells = sourceXMLDocumentTablesOnly.getCountTableCells();
                if (countTableCells < TABLE_CELLS_COUNT_MAX) {
                    // transform XML to XSL-FO (XML .fo file)
                    xsltConverter.transform(sourceXMLDocumentTablesOnly, false);

                    String xmlFO = sourceXMLDocumentTablesOnly.getXMLFO();

                    //debug
                    debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".fo.tables.xml");

                    fontcfg.outputFontManifestLog(Paths.get(pdf.getAbsolutePath() + ".tables.fontmanifest.log.txt"));

                    fontcfg.setSourceDocumentFontList(sourceXMLDocumentTablesOnly.getDocumentFonts());

                    Source sourceFO = new StreamSource(new StringReader(xmlFO));

                    logger.info("[INFO] Generation of Intermediate Format with information about the table's widths ...");
                    String xmlIF = generateFOPIntermediateFormat(sourceFO, fontcfg.getConfig(), pdf, true, ".tables");

                    xmlTableIF = createTableIF(xmlIF);

                } else { // for large tables, or large number of tables

                    List<String> xmlTablesIF = new ArrayList<>();

                    Map<String,Integer> tablesCellsCountMap = sourceXMLDocumentTablesOnly.getTablesCellsCountMap();

                    int portion = 1;
                    while(!tablesCellsCountMap.isEmpty()) {
                        int totalCells = 0;
                        List<String> tablesProcessed = new ArrayList<>();

                        Iterator<Map.Entry<String, Integer>> iterator = tablesCellsCountMap.entrySet().iterator();
                        while (iterator.hasNext() && totalCells < TABLE_CELLS_COUNT_MAX) {
                            Map.Entry<String, Integer> entry = iterator.next();
                            if (totalCells == 0 || totalCells + entry.getValue() < TABLE_CELLS_COUNT_MAX) {
                                totalCells += entry.getValue();
                                tablesProcessed.add(entry.getKey());
                            }
                        }

                        /*for (Map.Entry<String, Integer> entry : tablesCellsCountMap.entrySet()) {
                             else {
                                break;
                            }
                        }*/
                        logger.info("[INFO] Generation of XSL-FO (portion " + portion + ") with information about the table widths...");

                        // "table1 table2 table3 " (with space at the end)
                        String tableIds = tablesProcessed.stream().collect(Collectors.joining(" ")) + " ";
                        // call XSLT and pass the tables ids

                        // process table with ids=tableIds only
                        xsltConverter.setParam("table_only_with_ids", tableIds);

                        // transform XML to XSL-FO (XML .fo file)
                        xsltConverter.transform(sourceXMLDocumentTablesOnly, false);

                        String xmlFO = sourceXMLDocumentTablesOnly.getXMLFO();

                        //debug
                        debugSaveXML(xmlFO, pdf.getAbsolutePath() + ".portion_" + portion + ".fo.tables.xml");

                        fontcfg.outputFontManifestLog(Paths.get(pdf.getAbsolutePath() + ".portion_" + portion + ".tables.fontmanifest.log.txt"));

                        fontcfg.setSourceDocumentFontList(sourceXMLDocumentTablesOnly.getDocumentFonts());

                        Source sourceFO = new StreamSource(new StringReader(xmlFO));

                        logger.info("[INFO] Generation of Intermediate Format with information about the table's widths (portion " + portion + ") ...");
                        String xmlIF = generateFOPIntermediateFormat(sourceFO, fontcfg.getConfig(), pdf, true, ".portion_" + portion + ".tables");

                        xmlTableIF = createTableIF(xmlIF);

                        debugSaveXML(xmlTableIF, pdf.getAbsolutePath() + ".portion_" + portion + ".tables.xml");

                        xmlTableIF = tableWidthsCleanup(xmlTableIF);

                        xmlTablesIF.add(xmlTableIF);

                        // remove processed tables
                        tablesCellsCountMap.keySet().removeAll(tablesProcessed);
                        portion++;
                    }

                    /*List<String> tablesIds = sourceXMLDocumentTablesOnly.readElementsIds("//*[local-name() = 'table' or local-name() = 'dl']");
                    // process each table separatery for memory consumption optimization
                    int tableCounter = 0;
                    int tableCount = tablesIds.size();
                    for (String tableId : tablesIds) {
                        tableCounter++;
                        logger.info("[INFO] Generation of XSL-FO (" + tableCounter + "/" + tableCount + ") with information about the table widths with id='" + tableId + "'...");
                    }*/
                    xmlTableIF = tablesWidthsUnion(xmlTablesIF);
                    xsltConverter.setParam("table_only_with_id", ""); // further process all tables
                    xsltConverter.setParam("table_only_with_ids", ""); // further process all tables
                }

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
            xsltConverter.setParam("table_if", "false");
            xsltConverter.setParam("table_only_with_id", "");
            xsltConverter.setParam("table_only_with_ids", "");
            logger.log(Level.SEVERE, "Can''t obtain table''s widths information: {0}", e.toString());
        }
        Profiler.printProcessingTime(methodName, startMethodTime);
        Profiler.removeMethodCall();
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


    private int getIFPageCount(String xmlIF) {
        int pagecount = 0;
        if (xmlIF != null) {
            pagecount = xmlIF.split("<page ", -1).length - 1;
        }
        return pagecount;
    }

    private void saveDebugFO(String debugXSLFO) {
        if (DEBUG) {
            int MAX_LENGTH = 5000000;
            if (debugXSLFO.length() > MAX_LENGTH) {
                this.debugXSLFO = debugXSLFO.substring(0, MAX_LENGTH);
            } else {
                this.debugXSLFO = debugXSLFO;
            }
        }
    }

    private String tableWidthsCleanup(String table) {
        try {
            table = applyXSLT("table_if_clean.xsl", table, false);
        } catch (Exception ex) {
            logger.severe("Can't simplify the tables width information XML.");
            ex.printStackTrace();
        }
        /*int startPos = table.indexOf("<table ");
        int endPos = table.indexOf("</tables>");
        table = table.substring(startPos, endPos);
        int startPosTbody =  table.indexOf("<tbody>");
        table = table.substring(0,startPosTbody) + "</table>";*/
        return table;
    }

    private String tablesWidthsUnion(List<String> tables) {
        StringBuilder sbTablesIF = new StringBuilder();
        if (!tables.isEmpty()) {
            sbTablesIF.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tables>");
        }
        for (String itemTableIF: tables) {
            int startPos = itemTableIF.indexOf("<table ");
            int endPos = itemTableIF.indexOf("</tables>");
            itemTableIF = itemTableIF.substring(startPos, endPos);
            sbTablesIF.append(itemTableIF);
        }
        if (!tables.isEmpty()) {
            sbTablesIF.append("</tables>");
        }
        return sbTablesIF.toString();
    }

}
