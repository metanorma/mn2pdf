
package com.metanorma.fop.fonts;

import com.metanorma.fop.Util;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 *
 * @author Alexander Dyuzhev
 */
public class DefaultFonts {
    
    public static final String DEFAULTFONT_PREFIX = "Source";
    public static final String DEFAULTFONT_SUFFIX = "Pro";
    
    private final List<String> defaultFontList = new ArrayList<String>() { 
       { 
           // Example
           // add("SourceSansPro-Regular.ttf");
           Stream.of("Sans", "Serif", "Code").forEach(
                   prefix -> Stream.of("Regular", "Bold", "It", "BoldIt").forEach(
                           suffix -> add(DEFAULTFONT_PREFIX + prefix + DEFAULTFONT_SUFFIX + "-" + suffix + ".ttf"))
           );
           Stream.of("Sans").forEach(
                   prefix -> Stream.of("Light", "LightIt").forEach(
                           suffix -> add(DEFAULTFONT_PREFIX + prefix + DEFAULTFONT_SUFFIX + "-" + suffix + ".ttf"))
           );
           // add("SourceHanSans-Normal.ttc");
           Stream.of("Normal", "Bold").forEach(
               suffix -> add(DEFAULTFONT_PREFIX + "HanSans" + "-" + suffix + ".ttc"));

           add("STIX2Math.otf");
       } 
    };
    
    public DefaultFonts() {
        
    }
     
    public List<String> getDefaultFonts () {
        return defaultFontList;
    }
    
    //download Source family fonts and STIX2Math into fontPath
    public void downloadDefaultFonts(String fontDestinationPath) throws IOException, Exception {

        //fontDestinationPath = fontDestinationPath.replace("~", System.getProperty("user.home"));
        fontDestinationPath = Util.fixFontPath(fontDestinationPath);
        new File(fontDestinationPath).mkdirs();
        
        ArrayList<String> fontstocopy = new ArrayList<>();
        // check if default font exist in fontPath
        for (String fontfilename: defaultFontList) {
            final Path destPath = Paths.get(fontDestinationPath, fontfilename);
            if (!destPath.toFile().exists()) {
                fontstocopy.add(fontfilename);
            }            
            //InputStream fontfilestream = getStreamFromResources("fonts/" + fontfilename);            
            //Files.copy(fontfilestream, destPath, StandardCopyOption.REPLACE_EXISTING);
        }
        // download Source family fonts
        if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith(DefaultFonts.DEFAULTFONT_PREFIX))) {
            String url = getFontsURL("URL.sourcefonts");
            int remotefilesize = Util.getFileSize(new URL(url));
            final Path destZipPath = Paths.get(fontDestinationPath, "source-fonts.zip");
            if (!destZipPath.toFile().exists() || Files.size(destZipPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destZipPath);
            }
            // unzip file to fontPath
            Util.unzipFile(destZipPath, fontDestinationPath, defaultFontList);
            // check existing files
            for (String fontfilename: defaultFontList) {
                final Path destPath = Paths.get(fontDestinationPath, fontfilename);
                if (!destPath.toFile().exists()) {
                    System.out.println("Can't find a font file: " + destPath.toString());
                }
            }
        }
        if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith("STIX"))) {
            String url = getFontsURL("URL.STIX2Mathfont");
            int remotefilesize = Util.getFileSize(new URL(url));
            final Path destPath = Paths.get(fontDestinationPath, "STIX2Math.otf");
            if (!destPath.toFile().exists() || Files.size(destPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destPath);
            }
        }
    }
    
    private String getFontsURL(String property) throws Exception {
        Properties appProps = new Properties();
        appProps.load(Util.getStreamFromResources(getClass().getClassLoader(), "app.properties"));
        return appProps.getProperty(property);
    }
    

}
