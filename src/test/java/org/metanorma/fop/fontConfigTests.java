package org.metanorma.fop;

import org.metanorma.fop.fonts.FOPFont;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
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
        assertTrue(fonts.size() == 59);
    }

    @Test
    public void TestGetFontStyles() {
        fontConfig fontcfg = new fontConfig();
        Map<String,String> tc1 = fontcfg.getFontStyles("Regular");
        assertTrue(tc1.get("weight").equals("normal"));
        assertTrue(tc1.get("style").equals("normal"));
        
        Map<String,String> tc2 = fontcfg.getFontStyles("Bold");
        assertTrue(tc2.get("weight").equals("bold"));
        assertTrue(tc2.get("style").equals("normal"));
        
        Map<String,String> tc3 = fontcfg.getFontStyles("Bold Italic");
        assertTrue(tc3.get("weight").equals("bold"));
        assertTrue(tc3.get("style").equals("italic"));
        
        Map<String,String> tc4 = fontcfg.getFontStyles("Thin");
        assertTrue(tc4.get("weight").equals("100"));
        assertTrue(tc4.get("style").equals("normal"));
        
        Map<String,String> tc5 = fontcfg.getFontStyles("SemiBold Italic");
        assertTrue(tc5.get("weight").equals("600"));
        assertTrue(tc5.get("style").equals("italic"));   
    }
    
}
