package com.metanorma.fop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
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
import java.util.stream.Stream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Alexander Dyuzhev
 */
public class fontConfig {
    
    private final String ENV_FONT_PATH = "MN_PDF_FONT_PATH";
    private final String FONT_PREFIX = "Source";
    private final String FONT_SUFFIX = "Pro";
    private final String configPath;
    private final Document configXML;
    private String fontPath;
    private File updatedConfig;
    
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
    
    public fontConfig(File config) throws SAXException, ParserConfigurationException, IOException, Exception {
        this.configPath = config.getAbsolutePath();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	this.configXML = dBuilder.parse(config);
        
        //extract all .ttf files from resources into fontPath folder
        prepareFonts();
        // replace missing font in fonts/substitutions sections
        substFonts();
        writeXmlDocumentToXmlFile(configXML);
    }
    
    //extract all .ttf files from resources into fontPath folder
    private void prepareFonts() throws IOException, Exception {
        
        fontPath = System.getenv(ENV_FONT_PATH);
        if (fontPath == null) {
            //fontPath = System.getProperty("user.dir") + File.separator + ".fonts";
            System.out.println("Environment variable MN_PDF_FONT_PATH doesn't set.");
            System.exit(-1);
        }
        new File(fontPath).mkdirs();
        
        for (String fontfilename: fontList) {
            InputStream fontfilestream = getStreamFromResources("fonts/" + fontfilename);
            Files.copy(fontfilestream, new File(fontPath + File.separator + fontfilename).toPath(), StandardCopyOption.REPLACE_EXISTING);
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
    
    private void substFonts() {
        
        // select substitutions element
        Node nodesubstitutions = configXML.getElementsByTagName("substitutions").item(0);
        
        NodeList fonts = configXML.getElementsByTagName("font");
        
        //iterate each font
        for (int i = 0; i < fonts.getLength(); i++) {
            Node font = fonts.item(i);
            if (font.getAttributes().getNamedItem("embed-url") != null) {
                String url = font.getAttributes().getNamedItem("embed-url").getTextContent().replace("file:///", "");
                if (!new File (url).exists()) {
                    System.out.print("WARNING: Font file '" + url + "' doesn't exist. ");
                    Node fonttriplet = ((Element)font).getElementsByTagName("font-triplet").item(0);
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
                    } else if (fontname.toLowerCase().contains("courier")) {
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
                    System.out.println("Font '" + fontFamilySubst + "' will be used.");
                    substitution.appendChild(to);

                    nodesubstitutions.appendChild(substitution);
                }
            }
        }
        
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
            String updateConfigPath = configPath + ".out";
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
