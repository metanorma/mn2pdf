package com.metanorma.fop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.cos.COSName;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;

import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.SystemErrRule;

public class mn2pdfTests {

    @Rule
    public final ExpectedSystemExit exitRule = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();
    
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final EnvironmentVariables envVarRule = new EnvironmentVariables();

    @Test
    public void notEnoughArguments() throws ParseException {
        exitRule.expectSystemExitWithStatus(-1);
        String[] args = new String[]{"1", "2", "3"};
        mn2pdf.main(args);

        assertTrue(systemOutRule.getLog().contains(mn2pdf.USAGE));
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
    public void xmlNotExists() throws ParseException {
        exitRule.expectSystemExitWithStatus(-1);

        String fontpath = System.getProperty("buildDirectory") + File.separator + ".." + File.separator + "fonts";

        String[] args = new String[]{"--xml-file", "1", "--xsl-file", "2", "--pdf-file", "3"};
        mn2pdf.main(args);

        assertTrue(systemOutRule.getLog().contains(
                String.format(mn2pdf.INPUT_NOT_FOUND, mn2pdf.XML_INPUT, args[1])));
    }

    @Test
    public void xslNotExists() throws ParseException {
        exitRule.expectSystemExitWithStatus(-1);

        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = System.getProperty("buildDirectory") + File.separator + ".." + File.separator + "fonts";
        String xml = classLoader.getResource("G.191.xml").getFile();

        String[] args = new String[]{"--xml-file", xml, "--xsl-file", "3", "--pdf-file", "4"};
        mn2pdf.main(args);

        assertTrue(systemOutRule.getLog().contains(
                String.format(mn2pdf.INPUT_NOT_FOUND, mn2pdf.XSL_INPUT, args[2])));
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
    
    @Test
    public void additionalXMLnotfound() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("iec-rice.xml").getFile();
        String xsl = classLoader.getResource("iec.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iec-rice.pdf");

        String additionalXMLs = "iec-rice.fr.xml";
        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--param", "additionalXMLs=" + additionalXMLs, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);        
        assertTrue(systemErrRule.getLog().contains(additionalXMLs + " (")); //"Can not load requested doc"
    }
    
    @Test
    public void successFontReplacement() throws ParseException {
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
        String newPath = Paths.get(fontpath, "SourceSansPro-Regular.ttf").toString();
        assertTrue(systemOutRule.getLog().contains(
            String.format(fontConfig.WARNING_FONT, embed_url, "normal", "normal", newPath)));
        assertTrue(Files.exists(pdf));
    }
    
    @Test
    public void successNonPDFUAmode() throws ParseException {
        ClassLoader classLoader = getClass().getClassLoader();
        String fontpath = Paths.get(System.getProperty("buildDirectory"), ".." , "fonts").toString();
        String xml = classLoader.getResource("rice-en.svgtest.xml").getFile();
        String xsl = classLoader.getResource("iso.international-standard.xsl").getFile();
        Path pdf = Paths.get(System.getProperty("buildDirectory"), "iso-rice.pdf");

        String[] args = new String[]{"--font-path", fontpath, "--xml-file",  xml, "--xsl-file", xsl, "--pdf-file", pdf.toAbsolutePath().toString()};
        mn2pdf.main(args);
        
        assertTrue(systemOutRule.getLog().contains(mn2pdf.WARNING_NONPDFUA));
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
        assertTrue(PDFsubject.length() != 0);
        assertTrue(PDFkeywords.length() != 0);
        assertTrue(allEmbedded);
        
    }

}
