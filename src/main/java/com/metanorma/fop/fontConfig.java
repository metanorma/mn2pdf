package com.metanorma.fop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.fop.fonts.autodetect.FontFileFinder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Alexander Dyuzhev
 */
class fontConfig {
    static final String ENV_FONT_PATH = "MN_PDF_FONT_PATH";
    private final String CONFIG_NAME = "pdf_fonts_config.xml";
    private final String FONT_PREFIX = "Source";
    private final String FONT_SUFFIX = "Pro";
    private final Document configXML;
    private File updatedConfig;
    private String fontPath;
    private final ArrayList<String> fontList = new ArrayList<String>() { 
        { 
            // Example
            // add("SourceSansPro-Regular.ttf");
            Stream.of("Sans", "Serif", "Code").forEach(
                    prefix -> Stream.of("Regular", "Bold", "It", "BoldIt").forEach(
                            suffix -> add(FONT_PREFIX + prefix + FONT_SUFFIX + "-" + suffix + ".ttf"))
            );
        } 
    };
    
    private Map<String, List<String>> fontalternatemap;
    
    public fontConfig(File fontPath) throws SAXException, ParserConfigurationException, IOException, Exception {
        this.fontPath = fontPath.getAbsolutePath();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputStream config = getStreamFromResources(CONFIG_NAME);
	this.configXML = dBuilder.parse(config);
        
        //extract all .ttf files from resources into fontPath folder
        prepareFonts();
        // replace missing font in fonts/substitutions sections
        substFonts();
        writeXmlDocumentToXmlFile(configXML);
    }
    
    //extract all .ttf files from resources into fontPath folder
    private void prepareFonts() throws IOException, Exception {
        
        //fontPath = System.getenv(ENV_FONT_PATH);
        //if (fontPath == null) {
            //fontPath = System.getProperty("user.dir") + File.separator + ".fonts";
        //    System.out.println("Environment variable MN_PDF_FONT_PATH doesn't set.");
        //    System.exit(-1);
        //}
        fontPath = fontPath.replace("~", System.getProperty("user.home"));
        new File(fontPath).mkdirs();
        
        for (String fontfilename: fontList) {
            InputStream fontfilestream = getStreamFromResources("fonts/" + fontfilename);
            final String destPath = fontPath + File.separator + fontfilename;
            Files.copy(fontfilestream, new File(destPath).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        fontalternatemap = fillAlternateMap();

    }
    
    private Map<String, List<String>> fillAlternateMap() throws IOException {
        Yaml yaml = new Yaml();
        Map<String, List<String>> map = null;
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("fontmap.yml")) {
            Iterable<Object> itr = yaml.loadAll(inputStream);
            for (Object object : itr) {
                Map<String, List<String>> tmpmap = (Map<String, List<String>>) object;
                map = tmpmap.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), entry -> entry.getValue()));
            }
        }
        return map;
    }
    
    // get file from classpath, resources folder
    private InputStream getStreamFromResources(String fileName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream stream = classLoader.getResourceAsStream(fileName);
        if(stream == null) {
            throw new Exception("Cannot get resource \"" + fileName + "\" from Jar file.");
        }
        return stream;
    }
    
    private void substFonts() throws IOException {
        
        List<String> systemFontList = getSystemFonts();
        
        // select substitutions element
        Node nodesubstitutions = configXML.getElementsByTagName("substitutions").item(0);
        
        NodeList fonts = configXML.getElementsByTagName("font");
        
        //iterate each font
        for (int i = 0; i < fonts.getLength(); i++) {
            Node font = fonts.item(i);
            Node attr_embed_url = font.getAttributes().getNamedItem("embed-url");
            if (attr_embed_url != null) {
                String embed_url = attr_embed_url.getTextContent()
                                    .replace("${" + ENV_FONT_PATH + "}", fontPath);
                
                attr_embed_url.setNodeValue(embed_url);
                embed_url = embed_url.replace("file://", "");
                File file_embed_url = new File (embed_url);
                if (!file_embed_url.exists()) {
                    System.out.print("WARNING: Font file '" + embed_url + "' doesn't exist. ");
                    
                    //try to find system font (example for Windows - C:/Windows/fonts/)
                    String fontfilename = file_embed_url.getName();
                    String font_replacementpath = null;
                    for (String url: systemFontList) {
                        if (url.toLowerCase().endsWith(fontfilename.toLowerCase())) {
                            font_replacementpath = url;
                            break;
                        }
                    }
                    // if there isn't system font, then try to find font with alternate name
                    // Example: timesbd.ttf -> Times New Roman Bold.ttf
                    if (font_replacementpath == null) {
                        List<String> fontalternatelist = fontalternatemap.get(fontfilename.toLowerCase());
                        if (fontalternatelist != null) {
                            for (String fontalternatename: fontalternatelist) {
                                if (font_replacementpath != null) {
                                    break;
                                }
                                for (String url: systemFontList) {
                                    if (url.toLowerCase().endsWith(fontalternatename.toLowerCase())) {
                                        font_replacementpath = url;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (font_replacementpath != null) {
                        attr_embed_url.setTextContent(font_replacementpath);
                        System.out.println("Font '" + font_replacementpath + "' will be used.");
                    } else {
                        NodeList fonttriplets = ((Element)font).getElementsByTagName("font-triplet");
                        //iterate each font-triplet
                        for (int j = 0; j < fonttriplets.getLength(); j++) {
                            Node fonttriplet = fonttriplets.item(j);
                            //Node fonttriplet = ((Element)font).getElementsByTagName("font-triplet").item(0);
                            String fontname = fonttriplet.getAttributes().getNamedItem("name").getTextContent();
                            String fontstyle = fonttriplet.getAttributes().getNamedItem("style").getTextContent();
                            String fontweight = fonttriplet.getAttributes().getNamedItem("weight").getTextContent();
                            String substprefix = "Sans";
                            if (fontname.toLowerCase().contains("arial")) {
                                substprefix = "Sans";
                            } else if (fontname.toLowerCase().contains("times")) {
                                substprefix = "Serif";
                            } else if (fontname.toLowerCase().contains("cambria")) {
                                substprefix = "Serif";
                            } else if (fontname.toLowerCase().contains("calibri")) {
                                substprefix = "Sans";
                            } else if (fontname.toLowerCase().contains("cour")) {
                                substprefix = "Code";
                            } else if (fontname.toLowerCase().contains("sans")) {
                                substprefix = "Sans";
                            } else if (fontname.toLowerCase().contains("serif")) {
                                substprefix = "Serif";
                            }

                            // append a new node to substitutions
                            Element substitution = configXML.createElement("substitution");
                            Element from = configXML.createElement("from");
                            from.setAttribute("font-family", fontname);
                            from.setAttribute("font-style", fontstyle);
                            from.setAttribute("font-weight", fontweight);
                            substitution.appendChild(from);

                            Element to = configXML.createElement("to");
                            String substsuffix = "Regular";
                            if (fontstyle.equals("italic")) {
                                if (fontweight.equals("bold")) {
                                    substsuffix = "BoldIt";
                                } else {
                                    substsuffix = "It";
                                }
                            }
                            if (fontweight.equals("bold")) {

                                if (fontstyle.equals("italic")) {
                                    substsuffix = "BoldIt";
                                } else {
                                    substsuffix = "Bold";
                                }
                            }
                            to.setAttribute("font-style", fontstyle);
                            to.setAttribute("font-weight", fontweight);
                            String fontFamilySubst = FONT_PREFIX + substprefix + FONT_SUFFIX + "-" + substsuffix;
                            to.setAttribute("font-family", fontFamilySubst);
                            System.out.println("Font '" + fontPath + File.separator + fontFamilySubst + ".ttf' will be used.");
                            substitution.appendChild(to);

                            nodesubstitutions.appendChild(substitution);
                        
                        }
                    }
                }
            }
        }
    }

    private List<String> getSystemFonts() throws IOException{
        List<URL> systemFontListURL;
        FontFileFinder fontFileFinder = new FontFileFinder(null);
        systemFontListURL = fontFileFinder.find();
        // %20 to space character replacement
        List<String> systemFontList =  new ArrayList<>();
        for(URL url: systemFontListURL){
            systemFontList.add(URLDecoder.decode(url.toString(), StandardCharsets.UTF_8.name()));
        }
        
        return systemFontList;
    }
    
    private void writeXmlDocumentToXmlFile(Document xmlDocument) throws IOException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();

            StringWriter writer = new StringWriter();

            //transform document to string 
            transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));

            String xmlString = writer.getBuffer().toString();   
            
            //System.out.println(xmlString);
            String updateConfigPath = this.fontPath + File.separator + CONFIG_NAME + ".out";
            updatedConfig = new File (updateConfigPath);
            Path path = Paths.get(updateConfigPath);
            try (BufferedWriter bw = Files.newBufferedWriter(path)) 
            {
                bw.write(xmlString);
            }
        } 
        catch (TransformerException e) 
        {
            e.printStackTrace();
        }        
    }

    // get updated config file name
    public File getUpdatedConfig() {
        return updatedConfig;
    }

}
