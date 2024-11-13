package org.metanorma.fop;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestName;
import org.metanorma.fop.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.Assert.assertTrue;

public class ImageUtilTests {

    @Rule
    public final ExpectedSystemExit exitRule = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public final EnvironmentVariables envVarRule = new EnvironmentVariables();

    @Rule public TestName name = new TestName();

    @Test
    public void testImageWebPtoPNG() throws IOException {
        System.out.println(name.getMethodName());
        ClassLoader classLoader = getClass().getClassLoader();
        String imageFilename = new File(classLoader.getResource("sample.webp").getFile()).getAbsolutePath();

        String imagepathPNG = ImageUtils.convertWebPtoPNG(imageFilename);
        assertTrue(Files.exists(Paths.get(imagepathPNG)));

        String imageScale = ImageUtils.getImageScale(imagepathPNG, "100", "200");
        assertTrue(imageScale.equals("37"));

        String imageWidth = ImageUtils.getImageWidth(imagepathPNG, "10", "20");
        assertTrue(imageWidth.equals("10.0"));
    }

    @Test
    public void testImageBase64WebPtoPNG() throws IOException {
        System.out.println(name.getMethodName());
        ClassLoader classLoader = getClass().getClassLoader();
        String imageFilename = classLoader.getResource("sample.webp").getFile();
        byte[] imageContent = Files.readAllBytes(new File(imageFilename).toPath());
        String imageContentBase64 = "data:image/webp;base64," + Base64.getEncoder().encodeToString(imageContent);
        String imagePNGBase64 = ImageUtils.convertWebPtoPNG(imageContentBase64);
        assertTrue(imagePNGBase64.startsWith("data:image/png;base64,"));
        String imageScale = ImageUtils.getImageScale(imagePNGBase64, "100", "200");
        assertTrue(imageScale.equals("37"));
    }

}
