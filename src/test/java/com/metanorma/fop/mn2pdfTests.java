package com.metanorma.fop;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.ParseException;

import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;

public class mn2pdfTests {

    @Rule
    public final ExpectedSystemExit exitRule = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

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
    
}
