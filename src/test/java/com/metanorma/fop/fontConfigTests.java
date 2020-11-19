package com.metanorma.fop;

import com.metanorma.fop.fonts.FOPFont;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.cli.ParseException;

import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.SystemErrRule;

public class fontConfigTests {

    @Rule
    public final ExpectedSystemExit exitRule = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();
    
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final EnvironmentVariables envVarRule = new EnvironmentVariables();

    
    @Test
    public void testUsedFontList() throws ParseException, ParserConfigurationException, IOException, Exception {
        
        ClassLoader classLoader = getClass().getClassLoader();
        String xml = classLoader.getResource("G.191.xml").getFile();
        SourceXMLDocument sourceXMLDocument = new SourceXMLDocument(new File(xml));
        
        String xsl = classLoader.getResource("itu.recommendation.xsl").getFile();        
        XSLTconverter xsltConverter = new XSLTconverter(new File(xsl));
        xsltConverter.transform(sourceXMLDocument);
        
        fontConfig fontcfg = new fontConfig();        
        fontcfg.setSourceDocumentFontList(sourceXMLDocument.getDocumentFonts());
        List<FOPFont> fonts = fontcfg.getUsedFonts();
        
        assertTrue(!fonts.isEmpty());
        assertTrue(fonts.size() == 13);
    }
    
    
}
