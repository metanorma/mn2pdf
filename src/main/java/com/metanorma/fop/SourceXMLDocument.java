package com.metanorma.fop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Alexander Dyuzhev
 */
public class SourceXMLDocument {
    
    Document sourceXML;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");
    final Path tmpfilepath  = Paths.get(TMPDIR, UUID.randomUUID().toString());
    
    File fXML;
    
    String xmlFO = "";
    
    public SourceXMLDocument(File fXML) {
        this.fXML = fXML;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream xmlstream = new FileInputStream(fXML);
            sourceXML = dBuilder.parse(xmlstream);
        } catch (Exception ex) {
            System.err.println("Can't read source XML.");
            ex.printStackTrace();
        }
    }
    
    public StreamSource getStreamSource() {
        return new StreamSource(this.fXML);
    }
    
    public String getXMLFO() {
        return xmlFO;
    }
    
    public void setXMLFO(String xmlFO) {
        this.xmlFO = xmlFO;
    }
    
    public List<String> getDocumentFonts() {        
        List<String> documentFontList = new ArrayList<>();
        
        if(!xmlFO.isEmpty()) {
            try {
                InputSource is = new InputSource(new StringReader(xmlFO));
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document srcXML = dBuilder.parse(is);

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
            } catch (ParserConfigurationException | SAXException | IOException e) {
                System.out.println(e.toString());
            }
        }
        
        return documentFontList;
    }
    
    public String getImageFilePath()  {
        try {
            
            NodeList images = sourceXML.getElementsByTagName("image");

            HashMap<String, String> svgmap = new HashMap<>();
            for (int i = 0; i < images.getLength(); i++) {
                Node image = images.item(i);
                Node mimetype = image.getAttributes().getNamedItem("mimetype");
                if (mimetype != null && mimetype.getTextContent().equals("image/svg+xml")) {
                    // decode base64 svg into external tmp file
                    Node svg_src = image.getAttributes().getNamedItem("src");
                    Node svg_id = image.getAttributes().getNamedItem("id");
                    if (svg_src != null && svg_id != null && svg_src.getTextContent().startsWith("data:image")) {
                        String base64svg = svg_src.getTextContent().substring(svg_src.getTextContent().indexOf("base64,")+7);
                        String xmlsvg = Util.getDecodedBase64SVGnode(base64svg);
                        try {
                            Files.createDirectories(tmpfilepath);
                            String id = svg_id.getTextContent();
                            Path svgpath = Paths.get(tmpfilepath.toString(), id + ".svg");
                            try (BufferedWriter bw = Files.newBufferedWriter(svgpath)) {
                                bw.write(xmlsvg);
                            }
                            svgmap.put(id, svgpath.toFile().toURI().toURL().toString());
                        } catch (IOException ex) {
                            System.err.println("Can't save svg file into a temporary directory " + tmpfilepath.toString());
                            ex.printStackTrace();;
                        }
                    }
                }
            }
            if (!svgmap.isEmpty()) {
                // crate map file for svg images
                DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
                Document document = documentBuilder.newDocument();
                Element root = document.createElement("images");
                document.appendChild(root);
                for (Map.Entry<String, String> item : svgmap.entrySet()) {
                    Element image = document.createElement("image");
                    root.appendChild(image);
                    Attr attr_id = document.createAttribute("id");
                    attr_id.setValue(item.getKey());
                    image.setAttributeNode(attr_id);
                    Attr attr_path = document.createAttribute("src");
                    attr_path.setValue(item.getValue());
                    image.setAttributeNode(attr_path);
                }
                // save xml 'images.xml' with svg links to temporary folder
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource domSource = new DOMSource(document);
                Path outputPath = Paths.get(tmpfilepath.toString(), "images.xml");
                StreamResult streamResult = new StreamResult(outputPath.toFile());
                transformer.transform(domSource, streamResult);

                return outputPath.toString();
            }
        } catch (Exception ex) {
            System.err.println("Can't save images.xml into temporary folder");
            ex.printStackTrace();
        }
        return "";
    } 
    
    public String getIndexFilePath() {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression query;
            try {
                query = xPath.compile("//clause[@type = 'index']");
                NodeList nList = (NodeList)query.evaluate(sourceXML, XPathConstants.NODESET);
                if (nList.getLength() != 0) {
                    try {
                        Files.createDirectories(tmpfilepath);
                        Path outputPath = Paths.get(tmpfilepath.toString(), "index.xml");
                        return outputPath.toString();
                    } catch (IOException ex) {
                        System.err.println("Can't create a temporary directory " + tmpfilepath.toString());
                        ex.printStackTrace();;
                    }
                }
            } catch (XPathExpressionException ex) {
                System.out.println(ex.toString());
            }
            
        } catch (Exception ex) {
            System.err.println("Can't save index.xml into temporary folder");
            ex.printStackTrace();
        }
        return "";
    }
            
    public ArrayList<String> getLanguagesList () {
        ArrayList<String> languagesList = new ArrayList<>();
        try {            
            // open XML and find all <language> tags
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream xmlstream = new FileInputStream(fXML);
            Document sourceXML = dBuilder.parse(xmlstream);
            //NodeList languages = sourceXML.getElementsByTagName("language");

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//*[contains(local-name(),'-standard')]/*[local-name()='bibdata']//*[local-name()='language']");
            NodeList languages = (NodeList) expr.evaluate(sourceXML, XPathConstants.NODESET);
            
            for (int i = 0; i < languages.getLength(); i++) {
                String language = languages.item(i).getTextContent();                 
                if (!languagesList.contains(language)) {
                    languagesList.add(language);
                }
            }

        } catch (Exception ex) {
            System.err.println("Can't read language list from source XML.");
            ex.printStackTrace();
        }
        
        return languagesList;
    }
    
    public void flushTempPath() {
        if (Files.exists(tmpfilepath)) {

            //Files.deleteIfExists(tmpfilepath);
            try {
                Files.walk(tmpfilepath)
                    .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                            .forEach(File::delete);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public String getTempPath() {
        return tmpfilepath.toString();
    }
}
