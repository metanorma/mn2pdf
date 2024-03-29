
package org.metanorma.fop.fonts;

import org.metanorma.fop.Util;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.metanorma.utils.LoggerHelper;

/**
 *
 * @author Alexander Dyuzhev
 */
public class DefaultFonts {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    public static final String DEFAULTFONT_PREFIX = "Source";
    public static final String DEFAULTFONT_NOTO_PREFIX = "Noto";
    
    public static final String DEFAULTFONT_SUFFIX = "Pro";
    public static final String DEFAULTFONT_NOTO_SUFFIX = "";
    
    private final List<String> defaultFontList = new ArrayList<String>() { 
       { 
           // Example
           // add("SourceSansPro-Regular.ttf");
           //Stream.of("Sans", "Serif", "Code").forEach(
           //        prefix -> Stream.of("Regular", "Bold", "It", "BoldIt").forEach(
           //                suffix -> add(DEFAULTFONT_PREFIX + prefix + DEFAULTFONT_SUFFIX + "-" + suffix + ".ttf"))
           //);
           
           // Example
           // add("NotoSans-Regular.ttf");
           Stream.of("Sans", "Serif").forEach(
                   prefix -> Stream.of("Regular", "Bold", "Italic", "BoldItalic").forEach(
                           suffix -> add(DEFAULTFONT_NOTO_PREFIX + prefix + DEFAULTFONT_NOTO_SUFFIX + "-" + suffix + ".ttf"))
           );
           Stream.of("SansMono").forEach(
                   prefix -> Stream.of("Regular", "Bold").forEach(
                           suffix -> add(DEFAULTFONT_NOTO_PREFIX + prefix + DEFAULTFONT_NOTO_SUFFIX + "-" + suffix + ".ttf"))
           );
           
           
           //Stream.of("Sans").forEach(
           //        prefix -> Stream.of("Light", "LightIt").forEach(
           //                suffix -> add(DEFAULTFONT_PREFIX + prefix + DEFAULTFONT_SUFFIX + "-" + suffix + ".ttf"))
           //);
           
           // NotoSans-Light.ttf
           Stream.of("Sans").forEach(
                   prefix -> Stream.of("Light", "LightItalic").forEach(
                           suffix -> add(DEFAULTFONT_NOTO_PREFIX + prefix + DEFAULTFONT_NOTO_SUFFIX + "-" + suffix + ".ttf"))
           );
           
           // Examples: NotoSansHK-Regular.otf, NotoSerifJP-Bold.otf 
           Stream.of("Sans", "Serif").forEach(prefix
                   -> Stream.of("Regular", "Bold").forEach(suffix 
                   -> Stream.of("HK", "JP", "KR", "SC", "TC").forEach(lang
                   -> add(DEFAULTFONT_NOTO_PREFIX + prefix + lang + DEFAULTFONT_NOTO_SUFFIX + "-" + suffix + ".otf")))
           );
           
           // Example: NotoSansMonoCJKjp-Bold.otf 
           Stream.of("Sans").forEach(prefix
                   -> Stream.of("Regular", "Bold").forEach(suffix 
                   -> Stream.of("hk", "jp", "kr", "sc", "tc").forEach(lang
                   -> add(DEFAULTFONT_NOTO_PREFIX + prefix + "MonoCJK" + lang + DEFAULTFONT_NOTO_SUFFIX + "-" + suffix + ".otf")))
           );
           
           
           
           // add("SourceHanSans-Normal.ttc");
           //Stream.of("Normal", "Bold").forEach(
           //    suffix -> add(DEFAULTFONT_PREFIX + "HanSans" + "-" + suffix + ".ttc"));

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
        /*if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith(DefaultFonts.DEFAULTFONT_PREFIX))) {
            String url = getFontsURL("URL.sourcefonts");
            int remotefilesize = Util.getFileSize(new URL(url));
            final Path destZipPath = Paths.get(fontDestinationPath, "source-fonts.zip");
            if (!destZipPath.toFile().exists() || Files.size(destZipPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destZipPath);
            }
            // unzip file to fontPath
            Util.unzipFile(destZipPath, fontDestinationPath, defaultFontList, new ArrayList<>());
            // check existing files
            for (String fontfilename: defaultFontList) {
                final Path destPath = Paths.get(fontDestinationPath, fontfilename);
                if (!destPath.toFile().exists()) {
                    logger.info("Can't find a font file: " + destPath.toString());
                }
            }
        }*/
        
        List<String> fontsdownloaded = new ArrayList<>();
        
        // download fonts
        if (!fontstocopy.isEmpty()) {
            List<String> urls = getFontsURLs();
            logger.info("Fonts downloading...");
            for(String url: urls) {
                int remotefilesize = Util.getFileSize(new URL(url));
                final Path destZipPath = Paths.get(fontDestinationPath, getFilenameFromURL(url));
                if (!destZipPath.toFile().exists() || Files.size(destZipPath) != remotefilesize) {
                    // download a file
                    Util.downloadFile(url, destZipPath);
                }
                // unzip file to fontPath
                Util.unzipFile(destZipPath, fontDestinationPath, fontstocopy, fontsdownloaded);
                // check existing files
                
            }
            
        }
        
        
        
        // download Noto family fonts
        /*if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith(DefaultFonts.DEFAULTFONT_NOTO_PREFIX))) {
            String url = getFontsURL("URL.notofonts");
            int remotefilesize = Util.getFileSize(new URL(url));
            final Path destZipPath = Paths.get(fontDestinationPath, "noto-fonts.zip");
            if (!destZipPath.toFile().exists() || Files.size(destZipPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destZipPath);
            }
            // unzip file to fontPath
            Util.unzipFile(destZipPath, fontDestinationPath, defaultFontList, new ArrayList<String>());
            // check existing files
            for (String fontfilename: defaultFontList) {
                final Path destPath = Paths.get(fontDestinationPath, fontfilename);
                if (!destPath.toFile().exists()) {
                    logger.info("Can't find a font file: " + destPath.toString());
                }
            }
        }*/
        
        if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith("STIX"))) {
            String url = getFontsURL("URL.STIX2Mathfont");
            int remotefilesize = Util.getFileSize(new URL(url));
            String filename = getFilenameFromURL(url);
            final Path destPath = Paths.get(fontDestinationPath,filename ); //"STIX2Math.otf"
            if (!destPath.toFile().exists() || Files.size(destPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destPath);
                fontsdownloaded.add(filename);
            }
        }
        
        // remove downloaded fonts from defaultFontList
        fontsdownloaded.forEach(font -> {
            fontstocopy.remove(font);
        });
        
        for (String fontfilename: fontstocopy) {
            final Path destPath = Paths.get(fontDestinationPath, fontfilename);
            if (!destPath.toFile().exists()) {
                logger.log(Level.INFO, "Can''t find a font file: {0}", destPath.toString());
            }
        }
    }
    
    private String getFontsURL(String property) throws Exception {
        Properties appProps = new Properties();
        appProps.load(Util.getStreamFromResources(getClass().getClassLoader(), "app.properties"));
        return appProps.getProperty(property);
    }
    
    private List<String> getFontsURLs() throws Exception {
        Properties appProps = new Properties();
        appProps.load(Util.getStreamFromResources(getClass().getClassLoader(), "app.properties"));
        
        List<String> urls = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : appProps.entrySet()) {
            String k = entry.getKey().toString();
            if (k.startsWith("URL.") && k.contains("fonts")) {
                urls.add(entry.getValue().toString());
            }
        }
        return urls;
    }

    private String getFilenameFromURL(String url) {
        int slashIndex = url.lastIndexOf('/');
        return url.substring(slashIndex + 1);
    }
    

}
