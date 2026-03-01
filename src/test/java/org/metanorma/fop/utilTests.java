package org.metanorma.fop;

import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestName;
import org.metanorma.Constants;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.Node;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class utilTests {

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
    public void testSyntaxHighlight() throws TransformerException, TransformerConfigurationException  {
        System.out.println(name.getMethodName());
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
    public void checkCSSparsing() throws IOException {
        System.out.println(name.getMethodName());
        String cssString = "sourcecode .c, sourcecode .ch {\n" +
                "  color: #FF0000;\n" +
                "}\n" +
                "#toc li a, #toc > ul :is(h1, h2, h3, h4, h5, h6) li a {\n" +
                "  text-transform: none;\n" +
                "}\n" +
                ".clauses::after     { content: \"chapters\"; }";
        Node xmlNode = Util.parseCSS(cssString);
        String xmlStr = nodeToString(xmlNode);
        assertEquals("<css><class name=\"c\"><property name=\"color\" value=\"rgb(255, 0, 0)\"/></class>" +
                "<class name=\"ch\"><property name=\"color\" value=\"rgb(255, 0, 0)\"/></class>" +
                "<class name=\"clauses::after\"><property name=\"content\" value=\"chapters\"/></class>" +
                "</css>", xmlStr);
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

    @Test
    public void testFilenameFromPath() throws IOException {
        System.out.println(name.getMethodName());
        String file1 = "_test_attachments_attachments/program.c";
        String file2 = "program.c";
        String file3 = "_test_attachments_attachments\\program.c";
        assertTrue(Util.getFilenameFromPath(file1).equals("program.c"));
        assertTrue(Util.getFilenameFromPath(file2).equals("program.c"));
        assertTrue(Util.getFilenameFromPath(file3).equals("program.c"));
    }

    @Test
    public void testURIFromPath() throws IOException {
        System.out.println(name.getMethodName());
        String file1 = "img-art/Image_in_50%_gray.svg";
        assertTrue(Util.getURIFromPath(file1).equals("img-art/Image_in_50%25_gray.svg"));
    }

}
