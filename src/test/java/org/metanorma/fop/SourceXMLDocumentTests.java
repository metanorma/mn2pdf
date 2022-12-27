package org.metanorma.fop;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.apache.commons.cli.ParseException;

import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.SystemErrRule;

public class SourceXMLDocumentTests {

    @Rule
    public final ExpectedSystemExit exitRule = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();
    
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final EnvironmentVariables envVarRule = new EnvironmentVariables();

    
    @Test
    public void testGetLanguageList() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String xml = classLoader.getResource("G.191.xml").getFile();
        SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(new File(xml));
        
        ArrayList<String> langs = sourceXMLDocument.getLanguagesList();
        assertTrue(langs.size() == 1);
        assertTrue(langs.get(0).equals("en"));
    }
    
    @Test
    public void testGetImageFilePath() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String xml = classLoader.getResource("rice-en.svgtest.xml").getFile();
        SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(new File(xml));
        
        String path = sourceXMLDocument.getImageFilePath();
        assertTrue(path.length() > 0);
        Path tmpPath = Paths.get(sourceXMLDocument.getTempPath(), "images.xml");
        assertTrue(Files.exists(tmpPath));        
        sourceXMLDocument.flushTempPath();
        Path tmpPathNotExist = Paths.get(sourceXMLDocument.getTempPath());
        assertTrue(!Files.exists(tmpPath));
    }
    
    @Test    
    public void testGetDocumentFonts() throws ParseException, TransformerException {
        ClassLoader classLoader = getClass().getClassLoader();
        String xml = classLoader.getResource("G.191.xml").getFile();
        SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(new File(xml));
        
        String xsl = classLoader.getResource("itu.recommendation.xsl").getFile();
        XSLTconverter xsltConverter = new XSLTconverter(new File(xsl));
        xsltConverter.transform(sourceXMLDocument);
        List<String> fonts = sourceXMLDocument.getDocumentFonts();
        
        assertTrue(fonts.size() == 16);
        assertTrue(fonts.get(1).equals("STIX Two Math"));
    }

    @Test
    public void testGetDocumentPreprocessXSLT() {
        ClassLoader classLoader = getClass().getClassLoader();
        String xml = classLoader.getResource("a.presentation.xml").getFile();
        SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(new File(xml));

        String strProcessXSLT = sourceXMLDocument.getPreprocessXSLT();

        String strProcessXSLTEtalon = "<!-- note/name -->\n" +
                "<xsl:template xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" match=\"*[local-name() = 'note']/*[local-name() = 'name']\" mode=\"update_xml_step1\" priority=\"5\"> \n" +
                "  <xsl:copy>\n" +
                "    <xsl:apply-templates select=\"@*|node()\"/><xsl:if test=\"normalize-space() != ''\">:<tab /></xsl:if>\n" +
                "  </xsl:copy>\n" +
                "</xsl:template>\n" +
                "\n";

        assertTrue(strProcessXSLT.equals(strProcessXSLTEtalon));
    }
}
