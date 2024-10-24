package org.metanorma.fop;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.cos.COSName;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;

import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.rules.TestName;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.metanorma.Constants;
import org.metanorma.fop.annotations.Annotation;
import org.metanorma.fop.utils.JapaneseToNumbers;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.Node;

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

    @Rule public TestName name = new TestName();
    
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
        System.out.println(name.getMethodName());
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
        System.out.println(name.getMethodName());
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
        System.out.println(name.getMethodName());
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
        System.out.println(name.getMethodName());
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
        System.out.println(name.getMethodName());
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
            String.format(fontConfig.WARNING_FONT, embed_url, "TestFont", "normal", "normal", newPath)) ||
                capturedLog.contains(
                        String.format(fontConfig.WARNING_FONT_NO_FILE, "TestFont", "TestFont", "normal", "normal", newPath)));
        assertTrue(Files.exists(pdf));
    }
    
    @Test
    public void successNonPDFUAmode() throws ParseException, IOException {
        System.out.println(name.getMethodName());
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
        System.out.println(name.getMethodName());
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
        System.out.println(name.getMethodName());
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
    public void successSVGRendering() throws ParseException, IOException {
        System.out.println(name.getMethodName());
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("iso.svgtest.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso.svgtest.pdf");

        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("OS: " + os);
        String manifestParam = "";
        String manifestFile = "";
        if (os.contains("mac") || os.contains("darwin") || os.contains("nux") || os.contains("win")) {
            manifestParam = "--font-manifest";
            manifestFile = classLoader.getResource("manifest.yml").getFile();
        }

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString(), manifestParam, manifestFile};
        mn2pdf.main(args);

        String capturedLog = getTestCapturedLog();
        assertTrue(!capturedLog.contains("SVG graphic could not be rendered"));
        assertTrue(Files.exists(pdf));
    }

    @Test
    public void checkSpacesInPDF() throws ParseException {
        System.out.println(name.getMethodName());
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
    public void checkAttachments() throws ParseException {
        System.out.println(name.getMethodName());
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("test_attachments.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "test.attachments.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);

        assertTrue(Files.exists(pdf));
        // check two attachments - one is embedded file, one is fileattachment annotation

        PDDocument doc;
        int countFileAttachmentAnnotation = 0;
        int countFileAttachmentEmbedded = 0;
        try {
            doc = PDDocument.load(pdf.toFile());

            int numberOfPages = doc.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
                PDPage page = doc.getPage(pageIndex);
                List<PDAnnotation> annotations = page.getAnnotations();

                for (PDAnnotation annotation: annotations) {
                    if (annotation instanceof PDAnnotationFileAttachment) {
                        countFileAttachmentAnnotation ++;
                    }
                }
                //document.getPage(pageIndex).setAnnotations(annotations);
            }

            PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            PDEmbeddedFilesNameTreeNode efTree = namesDictionary.getEmbeddedFiles();
            if (efTree != null)
            {
                Map<String, PDComplexFileSpecification> names = efTree.getNames();
                countFileAttachmentEmbedded = names.size();
            }

        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

        assertTrue(countFileAttachmentAnnotation == 1);
        assertTrue(countFileAttachmentEmbedded == 1);


    }

    @Test
    public void checkJapaneseNumbering() throws ParseException {
        System.out.println(name.getMethodName());
        String j1 = JapaneseToNumbers.numToWord(1);
        String j11 = JapaneseToNumbers.numToWord(11);
        String j23 = JapaneseToNumbers.numToWord(23);
        assertTrue(j1.equals("一"));
        assertTrue(j11.equals("十一"));
        assertTrue(j23.equals("二十三"));
    }
    
    @Test
    public void testDates() throws IOException {
        System.out.println(name.getMethodName());
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
