package com.metanorma.fop;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.fop.fonts.autodetect.FontFileFinder;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 *
 * @author Alexander Dyuzhev
 */
class fontConfig {
    static final String ENV_FONT_PATH = "MN_PDF_FONT_PATH";
    static final String WARNING_FONT = "WARNING: Font file '%s' (font style '%s', font weight '%s') doesn't exist. Replaced by '%s'.";
    private final String CONFIG_NAME = "pdf_fonts_config.xml";
    private final String CONFIG_NAME_UPDATED = CONFIG_NAME + ".out";
    private final String FONT_PREFIX = "Source";
    private final String FONT_SUFFIX = "Pro";
    private final Document configXML;
    private final Document srcXML;
    private File updatedConfig;
    private String fontPath;
    private ArrayList<String> messages;
    private final ArrayList<String> defaultFontList = new ArrayList<String>() { 
        { 
            // Example
            // add("SourceSansPro-Regular.ttf");
            Stream.of("Sans", "Serif", "Code").forEach(
                    prefix -> Stream.of("Regular", "Bold", "It", "BoldIt").forEach(
                            suffix -> add(FONT_PREFIX + prefix + FONT_SUFFIX + "-" + suffix + ".ttf"))
            );
            Stream.of("Sans").forEach(
                    prefix -> Stream.of("Light", "LightIt").forEach(
                            suffix -> add(FONT_PREFIX + prefix + FONT_SUFFIX + "-" + suffix + ".ttf"))
            );
            // add("SourceHanSans-Normal.ttc");
            Stream.of("Normal", "Bold").forEach(
                suffix -> add(FONT_PREFIX + "HanSans" + "-" + suffix + ".ttc"));
            
            add("STIX2Math.otf");
        } 
    };
    

    public fontConfig(String xmlFO, String fontPath) throws SAXException, ParserConfigurationException, IOException, Exception {
        messages = new ArrayList<>();
        this.fontPath = fontPath;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputStream config = getStreamFromResources(CONFIG_NAME);
        
	this.configXML = dBuilder.parse(config);
        
        InputSource is = new InputSource(new StringReader(xmlFO));
        this.srcXML = dBuilder.parse(is);
        
        
        
        //extract all .ttf files from resources into fontPath folder
        prepareFonts();
        // replace missing font in fonts/substitutions sections
        substFonts();
        //write updated FOP config file
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
        
        ArrayList<String> fontstocopy = new ArrayList<>();
        for (String fontfilename: defaultFontList) {
            final Path destPath = Paths.get(fontPath, fontfilename);
            if (!destPath.toFile().exists()) {
                fontstocopy.add(fontfilename);
            }            
            //InputStream fontfilestream = getStreamFromResources("fonts/" + fontfilename);            
            //Files.copy(fontfilestream, destPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith(FONT_PREFIX))) {
            String url = getFontsURL("URL.sourcefonts");
            int remotefilesize = Util.getFileSize(new URL(url));
            final Path destZipPath = Paths.get(fontPath, "source-fonts.zip");
            if (!destZipPath.toFile().exists() || Files.size(destZipPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destZipPath);
            }
            // unzip file to fontPath
            Util.unzipFile(destZipPath, fontPath, defaultFontList);
            // check existing files
            for (String fontfilename: defaultFontList) {
                final Path destPath = Paths.get(fontPath, fontfilename);
                if (!destPath.toFile().exists()) {
                    System.out.println("Can't find a font file: " + destPath.toString());
                }
            }
        }
        if (!fontstocopy.isEmpty() && fontstocopy.stream().anyMatch(s -> s.startsWith("STIX"))) {
            String url = getFontsURL("URL.STIX2Mathfont");
            int remotefilesize = Util.getFileSize(new URL(url));
            final Path destPath = Paths.get(fontPath, "STIX2Math.otf");
            if (!destPath.toFile().exists() || Files.size(destPath) != remotefilesize) {
                // download a file
                Util.downloadFile(url, destPath);
            }
        }
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
    
    private void substFonts() throws IOException, URISyntaxException {
        
        List<String> machineFontList = getMachineFonts();
        
        List<String> srcDocumentFontList = getDocumentFonts();
        
        NodeList fonts = configXML.getElementsByTagName("font");
        
        //iterate each font from FOP config
        for (int i = 0; i < fonts.getLength(); i++) {
            Node font = fonts.item(i);
            Node attr_embed_url = font.getAttributes().getNamedItem("embed-url");
            Node attr_sub_font = font.getAttributes().getNamedItem("sub-font");
            Node attr_simulate_style = font.getAttributes().getNamedItem("simulate-style");
            NodeList fonttriplets = ((Element)font).getElementsByTagName("font-triplet");
            if (attr_embed_url != null) {
                String msg = "";
                //String embed_url = attr_embed_url.getTextContent()
                //                    .replace("${" + ENV_FONT_PATH + "}", fontPath);
                String embed_url = Paths.get(fontPath, attr_embed_url.getTextContent()).toString();
                // add file:/..
                attr_embed_url.setNodeValue(new File(embed_url).toURI().toURL().toString());                
                
                boolean isUsedFont = true;
                try {
                    String fonttriplet_name = fonttriplets.item(0).getAttributes().getNamedItem("name").getTextContent();
                    isUsedFont = srcDocumentFontList.contains(fonttriplet_name);
                } catch (Exception ex) {
                    //skip
                };
                
                //try to find font if it uses only (skip unused font processing)
                if (isUsedFont) {
                
                    File file_embed_url = new File (embed_url);
                    if (!file_embed_url.exists()) {
                        msg = "WARNING: Font file '" + embed_url + "'";
                        //try to find system font (example for Windows - C:/Windows/fonts/)
                        String fontfilename = file_embed_url.getName();
                        String font_replacementpath = null;
                        for (String url: machineFontList) {
                            if (url.toLowerCase().endsWith(fontfilename.toLowerCase())) {
                                font_replacementpath = url;
                                break;
                            }
                        }
                        // if there isn't system font, then try to find font with alternate name
                        // Example: timesbd.ttf -> Times New Roman Bold.ttf
                        if (font_replacementpath == null) {

                            NodeList fontsalternate = ((Element)font).getElementsByTagName("alternate");

                            //iterate each font-triplet
                            for (int j = 0; j < fontsalternate.getLength(); j++) {
                                if (font_replacementpath != null) {
                                    break;
                                }
                                Node fontalternate = fontsalternate.item(j);
                                String fontalternateURL = fontalternate.getAttributes().getNamedItem("embed-url").getTextContent();
                                for (String url: machineFontList) {
                                    if (url.toLowerCase().endsWith(fontalternateURL.toLowerCase())) {
                                        font_replacementpath = url;
                                        Node attr_sub_font_alternate = fontalternate.getAttributes().getNamedItem("sub-font");
                                        // if alternate font doesn't have sub-font attribute, then delete it
                                        if (attr_sub_font_alternate == null && attr_sub_font != null) {
                                            font.getAttributes().removeNamedItem("sub-font");
                                        } else if (attr_sub_font_alternate != null) {                                        
                                            // if exist, then change it, else add it
                                            String attr_sub_font_alternate_value = attr_sub_font_alternate.getTextContent();
                                            if (attr_sub_font != null) {
                                                attr_sub_font.setNodeValue(attr_sub_font_alternate_value);
                                            } else {
                                                ((Element)font).setAttribute("sub-font", attr_sub_font_alternate_value);
                                            }
                                        }
                                        Node attr_simulate_style_alternate = fontalternate.getAttributes().getNamedItem("simulate-style");
                                        // if alternate font doesn't have simulate-style attribute, then delete it
                                        if (attr_simulate_style_alternate == null && attr_simulate_style != null) {
                                            font.getAttributes().removeNamedItem("simulate-style");
                                        } else if (attr_simulate_style_alternate != null) { 
                                            String attr_simulate_style_alternate_value = attr_simulate_style_alternate.getTextContent();
                                            // if exist, then change it, else add it
                                            if (attr_simulate_style != null) {
                                                attr_simulate_style.setNodeValue(attr_simulate_style_alternate_value);
                                            } else {
                                                ((Element)font).setAttribute("simulate-style",attr_simulate_style_alternate_value);
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        if (font_replacementpath == null) {                        
                            //iterate each font-triplet
                            for (int j = 0; j < fonttriplets.getLength(); j++) {
                                Node fonttriplet = fonttriplets.item(j);

                                String fontname = fonttriplet.getAttributes().getNamedItem("name").getTextContent();
                                String fontstyle = fonttriplet.getAttributes().getNamedItem("style").getTextContent();
                                String fontweight = fonttriplet.getAttributes().getNamedItem("weight").getTextContent();

                                String substprefix = getSubstFontPrefix(fontname);
                                String substsuffix = getSubstFontSuffix(fontname, fontweight, fontstyle);
                                String fontFamilySubst = FONT_PREFIX + substprefix + FONT_SUFFIX + "-" + substsuffix;

                                font_replacementpath = Paths.get(fontPath, fontFamilySubst + ".ttf").toString();
                                
                                //printMessage(msg + " (font style '" + fontstyle + "', font weight '" + fontweight + "') doesn't exist. Replaced by '" + font_replacementpath + "'.");
                                printMessage(String.format(WARNING_FONT, embed_url, fontstyle, fontweight, font_replacementpath));

                                font_replacementpath = new File(font_replacementpath).toURI().toURL().toString();
                            }
                        }
                        if (font_replacementpath != null) {
                            attr_embed_url.setNodeValue(font_replacementpath);
                            //if (font.getAttributes().getNamedItem("sub-font") != null && !(font.getAttributes().getNamedItem("embed-url").getNodeValue().contains(".ttc"))) {
                            //    font.getAttributes().removeNamedItem("sub-font");
                            //}
                            //if (font.getAttributes().getNamedItem("simulate-style") != null) {
                            //    font.getAttributes().removeNamedItem("simulate-style");
                            //}
                        }
                    }
                }
            }
        }
    }

    private List<String> getMachineFonts() throws IOException{
        List<URL> systemFontListURL;
        List<URL> userFontListURL;
        
        FontFileFinder fontFileFinder = new FontFileFinder(null);
        
        userFontListURL =  fontFileFinder.find(fontPath);
        systemFontListURL = fontFileFinder.find();
        
        userFontListURL.addAll(systemFontListURL);
        
        // %20 to space character replacement
        List<String> machineFontList =  new ArrayList<>();
        for(URL url: userFontListURL){
            machineFontList.add(URLDecoder.decode(url.toString(), StandardCharsets.UTF_8.name()));
        }
        return machineFontList;
    }
    
    private List<String> getDocumentFonts() {
        List<String> documentFontList = new ArrayList<>();
        
        XPath xPath = XPathFactory.newInstance().newXPath();
        
        XPathExpression query;
        try {
            query = xPath.compile("//*/@font-family");
            NodeList nList = (NodeList)query.evaluate(srcXML, XPathConstants.NODESET);
            
            for (int i = 0; i < nList.getLength(); i++) {
                try {
                    Attr attr = (Attr) nList.item(i);
                    for (String fname: attr.getNodeValue().split(",")) {
                        fname = fname.trim();
                        if (!documentFontList.contains(fname)) {
                            documentFontList.add(fname);
                        }
                    }                    
                } catch (Exception ex) {}
            }
            
        } catch (XPathExpressionException ex) {
            System.out.println(ex.toString());
        }
        
        return documentFontList;
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
            Path updateConfigPath = Paths.get(this.fontPath, CONFIG_NAME_UPDATED);
            updatedConfig = updateConfigPath.toFile();
            try (BufferedWriter bw = Files.newBufferedWriter(updateConfigPath)) 
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

    public void setPDFUAmode(String mode) throws SAXException, IOException, ParserConfigurationException{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();        
        Document configXML = dBuilder.parse(updatedConfig);
        NodeList pdfuamodelist = configXML.getElementsByTagName("pdf-ua-mode");
        if (pdfuamodelist != null) {
            Node pdfuamode = pdfuamodelist.item(0);
            pdfuamode.setTextContent(mode);
        }
        writeXmlDocumentToXmlFile(configXML);
    }
    
    private String getSubstFontPrefix (String fontname) {
        String substprefix = "Sans";
        if (fontname.toLowerCase().contains("arial")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("times")) {
            substprefix = "Sans";//"Serif";
        } else if (fontname.toLowerCase().contains("cambria")) {
            substprefix = "Sans";//"Serif";
        } else if (fontname.toLowerCase().contains("calibri")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("cour")) {
            substprefix = "Code";
        } else if (fontname.toLowerCase().contains("sans")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("serif")) {
            substprefix = "Sans";//"Serif";
        }
        return substprefix;
    }

    private String getSubstFontSuffix(String fontname, String fontweight, String fontstyle) {
        String substsuffix = "Regular";
        String pfx = "";
        if (fontname.contains("Light")) {
            pfx = "Light";
            substsuffix = "Light";
        }
        if (fontstyle.equals("italic")) {
            if (fontweight.equals("bold")) {
                substsuffix = "BoldIt";
            } else {
                substsuffix = pfx + "It";
            }
        }
        if (fontweight.equals("bold")) {
            if (fontstyle.equals("italic")) {
                substsuffix = "BoldIt";
            } else {
                substsuffix = "Bold";
            }
        }
        return substsuffix;
    }
    
    private String getFontsURL(String property) throws Exception {
        Properties appProps = new Properties();
        appProps.load(getStreamFromResources("app.properties"));
        return appProps.getProperty(property);
    }
    
    private void printMessage(String msg) {
        System.out.println(msg);
        messages.add(msg);
    }

    public ArrayList<String> getMessages() {
        return messages;
    }
    
}
