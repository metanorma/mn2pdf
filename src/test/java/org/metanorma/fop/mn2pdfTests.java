package org.metanorma.fop;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.cos.COSName;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;

import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.metanorma.Constants;
import static org.metanorma.Constants.ERROR_EXIT_CODE;
import static org.metanorma.fop.PDFGenerator.logger;
import org.metanorma.utils.LoggerHelper;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.Node;
import org.w3c.dom.css.*;

public class mn2pdfTests {

    private static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;
    
    @Rule
    public final ExpectedSystemExit exitRule = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();
    
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final EnvironmentVariables envVarRule = new EnvironmentVariables();

    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        LoggerHelper.setupLogger();
    }
    
    @Before
    public void attachLogCapturer()
    {
        logCapturingStream = new ByteArrayOutputStream();
        Handler[] handlers = logger.getParent().getHandlers();
        customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
        logger.addHandler(customLogHandler);
    }
    
    public String getTestCapturedLog() throws IOException
    {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }
    
    @Test
    public void notEnoughArguments() throws ParseException, IOException {
        exitRule.expectSystemExitWithStatus(-1);
        String[] args = new String[]{"1", "2", "3"};
        mn2pdf.main(args);

        String capturedLog = getTestCapturedLog();
        //assertTrue(systemOutRule.getLog().contains(mn2pdf.USAGE));
        assertTrue(capturedLog.contains(mn2pdf.USAGE));
    }

    /*@Test
    public void fopConfingNotExists() {
    	exitRule.expectSystemExitWithStatus(-1);

    	ClassLoader classLoader = getClass().getClassLoader();
		String config = classLoader.getResource("pdf_fonts_config.xml").getFile();

		String[] args = new String[] { "1", "2", "3", "4" };
 		mn2pdf.main(args);

 		assertTrue(systemOutRule.getLog().contains(
 				String.format(mn2pdf.INPUT_NOT_FOUND, mn2pdf.FOP_CONFIG_INPUT, args[0])));
    }*/
    
    @Test
    public void xmlNotExists() throws ParseException, IOException {
        exitRule.expectSystemExitWithStatus(-1);

        String fontpath = System.getProperty("buildDirectory") + File.separator + ".." + File.separator + "fonts";

        String[] args = new String[]{"--xml-file", "1", "--xsl-file", "2", "--pdf-file", "3"};
        mn2pdf.main(args);

        String capturedLog = getTestCapturedLog();
        //assertTrue(systemOutRule.getLog().contains(
        assertTrue(capturedLog.contains(
                String.format(Constants.INPUT_NOT_FOUND, Constants.XML_INPUT, args[1])));
    }

    @Test
    public void xslNotExists() throws ParseException, IOException {
        exitRule.expectSystemExitWithStatus(-1);

        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = System.getProperty("buildDirectory") + File.separator + ".." + File.separator + "fonts";
        String xml = classLoader.getResource("G.191.xml").getFile();

        String[] args = new String[]{"--xml-file", xml, "--xsl-file", "3", "--pdf-file", "4"};
        mn2pdf.main(args);

        String capturedLog = getTestCapturedLog();
        //assertTrue(systemOutRule.getLog().contains(
        assertTrue(capturedLog.contains(
                String.format(Constants.INPUT_NOT_FOUND, Constants.XSL_INPUT, args[2])));
    }

    /*@Test
    public void missingEnvVariable() {
    	exitRule.expectSystemExitWithStatus(-1);
    	envVarRule.clear(fontConfig.ENV_FONT_PATH);

    	ClassLoader classLoader = getClass().getClassLoader();
		String config = classLoader.getResource("pdf_fonts_config.xml").getFile();
		String xml = classLoader.getResource("G.191.xml").getFile();
		String xsl = classLoader.getResource("itu.recommendation.xsl").getFile();

		String[] args = new String[] { config, xml, xsl, "4" };
 		mn2pdf.main(args);

 		assertTrue(systemOutRule.getLog().contains(fontConfig.ENV_FONT_PATH));
    }*/
    @Test
    public void success() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("G.191.xml").getFile();
        String xsl = classLoader.getResource("itu.recommendation.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "G.191.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);

        assertTrue(Files.exists(pdf));
    }
    
    /*@Test
    public void additionalXMLnotfound() throws ParseException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("iec-rice.xml").getFile();
        String xsl = classLoader.getResource("iec.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iec-rice.pdf");

        String additionalXMLs = "iec-rice.fr.xml";
        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--param", "additionalXMLs=" + additionalXMLs, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);
        
        assertTrue(systemErrRule.getLog().contains(additionalXMLs + " (")); //"Can not load requested doc"
    }*/
    
    @Test
    public void successFontReplacement() throws ParseException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        
        String xml = classLoader.getResource("G.191.xml").getFile();
        String xsl = classLoader.getResource("itu.recommendation.testfont.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "itu.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);
        
        String embed_url = Paths.get(fontpath, "TestFont.ttf").toString();
        /*try 
            embed_url = new File(embed_url).toURI().toURL().toString();
        } catch (MalformedURLException ex) {}*/
//        String newPath = Paths.get(fontpath, "SourceSansPro-Regular.ttf").toString();
        String newPath = Paths.get(fontpath, "NotoSans-Regular.ttf").toString();
        
        String capturedLog = getTestCapturedLog();
        //assertTrue(systemOutRule.getLog().contains(
        assertTrue(capturedLog.contains(
            String.format(fontConfig.WARNING_FONT, embed_url, "TestFont", "normal", "normal", newPath)));
        assertTrue(Files.exists(pdf));
    }
    
    @Test
    public void successNonPDFUAmode() throws ParseException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("rice-en.svgtest.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso-rice.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);
        
        String capturedLog = getTestCapturedLog();
        //assertTrue(systemOutRule.getLog().contains(Constants.WARNING_NONPDFUA));
        assertTrue(capturedLog.contains(Constants.WARNING_NONPDFUA));
        assertTrue(Files.exists(pdf));
    }
    
    @Test
    public void checkResultedPDF() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("rice-en.final.metadata.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso-rice.allmetadata.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);
        
        // check all font embedded
        boolean allEmbedded = true;
        
        int pagecount = 0;
        String PDFtitle = "";
        String PDFauthors = "";
        String PDFsubject = "";
        String PDFkeywords = "";
        
        PDDocument  doc;
        try {
            doc = PDDocument.load(pdf.toFile());
        
            PDPageTree  pages = doc.getDocumentCatalog().getPages();
            for (int i = 0; i < pages.getCount(); i++) {
                PDPage page = pages.get(i);
                PDResources resources = page.getResources();                
                for (COSName cosname: resources.getFontNames()) {                    
                    PDFont font = resources.getFont(cosname);
                    allEmbedded = font.isEmbedded();
                    if (!allEmbedded) {
                        break;
                    }
                }
            }
        
            // check metadata fields
            PDDocumentInformation info = doc.getDocumentInformation();            
            pagecount = doc.getNumberOfPages();
            PDFtitle = info.getTitle();
            PDFauthors = info.getAuthor();
            PDFsubject = info.getSubject();
            PDFkeywords = info.getKeywords();
     
        } catch (IOException ex) {
            allEmbedded = false;
            System.out.println(ex.toString());
        }
        
        assertTrue(pagecount>0);
        assertTrue(PDFtitle.length() != 0);
        assertTrue(PDFauthors.length() != 0);
        //assertTrue(PDFsubject.length() != 0);
        assertTrue(PDFkeywords.length() != 0);
        assertTrue(allEmbedded);
        
    }

    @Test
    public void checkResultedEncryptedPDF() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("rice-en.final.metadata.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        String encryption_params = classLoader.getResource("pdf-encryption.yaml").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso-rice.encrypted.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString(), "--encryption-parameters", encryption_params};
        mn2pdf.main(args);
        
        // check pdf permissions
        int encryptionLength = 128;
        boolean allowPrint = true;
        boolean allowPrintHQ = true;
        boolean allowCopyContent = true;
        boolean allowEditContent = true;
        boolean allowEditAnnotations = true;
        boolean allowFillInForms = true;
        boolean allowAccessContent = true;
        boolean allowAssembleDocument = true;
        boolean encryptMetadata = false;
                
        PDDocument  doc;
        try {
            doc = PDDocument.load(pdf.toFile(), "userpass");
        
            AccessPermission ap = doc.getCurrentAccessPermission();
            allowPrint = ap.canPrint();
            allowPrintHQ = ap.canPrintDegraded();
            allowCopyContent = ap.canExtractContent();
            allowEditContent = ap.canModify();
            allowEditAnnotations = ap.canModifyAnnotations();
            allowFillInForms = ap.canFillInForm();
            allowAccessContent = ap.canExtractForAccessibility();
            allowAssembleDocument = ap.canAssembleDocument();
            PDEncryption penc = doc.getEncryption();
            encryptionLength = penc.getLength();
            encryptMetadata = penc.isEncryptMetaData();
            
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        
        assertTrue(encryptionLength == 256);
        assertTrue(allowPrint == false);
        assertTrue(allowPrintHQ == false);
        assertTrue(allowCopyContent == false);
        assertTrue(allowEditContent == false);
        assertTrue(allowEditAnnotations == false);
        assertTrue(allowFillInForms == false);
        assertTrue(allowAccessContent == false);
        assertTrue(allowAssembleDocument == false);
        assertTrue(encryptMetadata == true);
        
    }
    

    @Test
    public void testSyntaxHighlight() throws TransformerException, TransformerConfigurationException  {
        String code = "<root><a></a><b>text</b><c key='value'/></root>";
        Node node = Util.syntaxHighlight(code, "xml");
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        String value = writer.toString();
        String exprectedValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><syntax><span class=\"hljs-tag\">&lt;<span class=\"hljs-name\">root</span>&gt;</span><span class=\"hljs-tag\">&lt;<span class=\"hljs-name\">a</span>&gt;</span><span class=\"hljs-tag\">&lt;/<span class=\"hljs-name\">a</span>&gt;</span><span class=\"hljs-tag\">&lt;<span class=\"hljs-name\">b</span>&gt;</span>text<span class=\"hljs-tag\">&lt;/<span class=\"hljs-name\">b</span>&gt;</span><span class=\"hljs-tag\">&lt;<span class=\"hljs-name\">c</span> <span class=\"hljs-attr\">key</span>=<span class=\"hljs-string\">'value'</span>/&gt;</span><span class=\"hljs-tag\">&lt;/<span class=\"hljs-name\">root</span>&gt;</span></syntax>";
        assertTrue(value.equals(exprectedValue));
    }

    @Test
    public void successSVGRendering() throws ParseException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("iso.svgtest.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso.svgtest.pdf");

        String os = System.getProperty("os.name").toLowerCase();
        String manifestParam = "";
        if (os.contains("mac") || os.contains("darwin") ||os.contains("nux")) {
            String manifestFile = classLoader.getResource("manifest.yml").getFile();
            manifestParam = "--font-manifest " + manifestFile;
        }

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString(), manifestParam};
        mn2pdf.main(args);

        String capturedLog = getTestCapturedLog();
        assertTrue(!capturedLog.contains("SVG graphic could not be rendered"));
        assertTrue(Files.exists(pdf));
    }

    @Test
    public void checkSpacesInPDF() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("iso.zerowidthspacetest.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso.zerowidthspacetest.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);

        String pdftext = "";
        PDDocument  doc;
        try {
            doc = PDDocument.load(pdf.toFile());
            pdftext = new PDFTextStripper().getText(doc);
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        assertTrue(pdftext.contains("the_integers") && pdftext.contains("elementary_space") && pdftext.contains("make_elementary_space"));
    }

    @Test
    public void checkCSSparsing() throws IOException {
        String cssString = "sourcecode .c, sourcecode .ch {\n" +
                "  color: #FF0000;\n" +
                "}";
        Node xmlNode = Util.parseCSS(cssString);
        String xmlStr = nodeToString(xmlNode);
        assertEquals("<css><class name=\"c\"><property name=\"color\" value=\"rgb(255, 0, 0)\"/></class><class name=\"ch\"><property name=\"color\" value=\"rgb(255, 0, 0)\"/></class></css>", xmlStr);
    }

    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    
    @Test
    public void testDates() throws IOException {
        String date1 = "20180125T0121";
        Calendar cdate1 = Util.getCalendarDate(date1);
        Calendar cdate1_etalon = Calendar.getInstance();
        cdate1_etalon.clear();
        cdate1_etalon.set(2018,0,25,1,21,0);
        
        assertTrue(cdate1_etalon.compareTo(cdate1) == 0);
        
        String date2 = "20220422T000000";
        Calendar cdate2 = Util.getCalendarDate(date2);
        Calendar cdate2_etalon = Calendar.getInstance();
        cdate2_etalon.clear();
        cdate2_etalon.set(2022,03,22,0,0,0);
        assertTrue(cdate2_etalon.compareTo(cdate2) == 0);
        
        String date3 = "2017-01-01T00:00:00Z";
        Calendar cdate3 = Util.getCalendarDate(date3);
        Calendar cdate3_etalon = Calendar.getInstance();
        cdate3_etalon.clear();
        cdate3_etalon.set(2017,0,1,0,0,0);
        assertTrue(cdate3_etalon.compareTo(cdate3) == 0);
        
    }
    
}
