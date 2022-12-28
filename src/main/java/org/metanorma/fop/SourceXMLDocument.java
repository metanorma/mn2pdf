package org.metanorma.fop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
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
import static org.metanorma.fop.PDFGenerator.logger;
import static org.metanorma.fop.Util.getStreamFromResources;

import org.metanorma.utils.LoggerHelper;
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
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    Document sourceXML;

    String sourceXMLstr = "";
    
    static final String TMPDIR = System.getProperty("java.io.tmpdir");
    static final Path tmpfilepath  = Paths.get(TMPDIR, UUID.randomUUID().toString());
    
    String documentFilePath;
    
    File fXML;
    
    String xmlFO = "";
    
    public SourceXMLDocument(File fXML) {

        this.fXML = fXML;
        this.documentFilePath = this.fXML.getParent();
        if (this.documentFilePath == null) {
            this.documentFilePath = System.getProperty("user.dir");
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream xmlstream = new FileInputStream(fXML);
            sourceXML = dBuilder.parse(xmlstream);
        } catch (Exception ex) {
            logger.severe("Can't read source XML.");
            ex.printStackTrace();
        }
    }
    
    public SourceXMLDocument(String strXML) {

        try {
            this.sourceXMLstr = strXML;
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource xmlIFIS = new InputSource(new StringReader(strXML));
            sourceXML = dBuilder.parse(xmlIFIS);
        } catch (Exception ex) {
            logger.severe("Can't parse source XML.");
            ex.printStackTrace();
        }
    }
    
    public StreamSource getStreamSource() {
        if (sourceXMLstr.isEmpty()) {
            try {
                sourceXMLstr = Util.readFile(fXML.getAbsolutePath());
            } catch (IOException ex) {
                logger.severe("Can't read source XML.");
                ex.printStackTrace();
            }
        }
        //return new StreamSource(this.fXML); // issue with colon in path /Users/username/share/src/test:/main.presentation.xml
        return new StreamSource(new StringReader(sourceXMLstr));
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
                    logger.info(ex.toString());
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                logger.info(e.toString());
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
                            logger.log(Level.SEVERE, "Can't save svg file into a temporary directory {0}", tmpfilepath.toString());
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
            logger.severe("Can't save images.xml into temporary folder");
            ex.printStackTrace();
        }
        return "";
    } 
    
    public String getIndexFilePath() {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression query;
            try {
                //query = xPath.compile("//clause[@type = 'index']");
                query = xPath.compile("//indexsect");
                NodeList nList = (NodeList)query.evaluate(sourceXML, XPathConstants.NODESET);
                if (nList.getLength() != 0) {
                    try {
                        Files.createDirectories(tmpfilepath);
                        Path outputPath = Paths.get(tmpfilepath.toString(), "index.xml");
                        return outputPath.toString();
                    } catch (IOException ex) {
                        logger.severe("Can't create a temporary directory " + tmpfilepath.toString());
                        ex.printStackTrace();;
                    }
                }
            } catch (XPathExpressionException ex) {
                logger.info(ex.toString());
            }
            
        } catch (Exception ex) {
            logger.severe("Can't save index.xml into temporary folder");
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
            logger.severe("Can't read language list from source XML.");
            ex.printStackTrace();
        }

        return languagesList;
    }

    public String getPreprocessXSLT() {

        StringBuilder inlineXSLT = new StringBuilder();
        try {

            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression query = xPath.compile("//*[contains(local-name(),'ogc-standard')]/*[local-name()='render']/*[local-name()='preprocess-xslt']/*[local-name()='stylesheet']");
            NodeList nList = (NodeList)query.evaluate(sourceXML, XPathConstants.NODESET);

            for (int i = 0; i < nList.getLength(); i++) {
                Node nodeXSLT = nList.item(i);

                // convert Node to Document
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document documentXSLT = builder.newDocument();
                Node importedNode = documentXSLT.importNode(nodeXSLT, true);
                documentXSLT.appendChild(importedNode);

                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(documentXSLT), new StreamResult(writer));
                String xslt = writer.getBuffer().toString();

                documentXSLT = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xslt)));
                String xsltUpdated = updatePreprocessXSLT(documentXSLT);
                documentXSLT = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xsltUpdated)));

                XPath xPath2 = XPathFactory.newInstance().newXPath();
                XPathExpression query2 = xPath2.compile("/*"); // select all nodes inside xsl:stylesheet
                NodeList nList2 = (NodeList)query2.evaluate(documentXSLT, XPathConstants.NODESET);

                for (int j = 0; j < nList2.getLength(); j++) {
                    Node nodeXSLT2 = nList2.item(j);
                    inlineXSLT.append(Util.innerXml(nodeXSLT2));
                }
            }

        } catch (Exception ex) {
            logger.severe("Can't read pre-process XSLT from source XML.");
            ex.printStackTrace();
        }
        return inlineXSLT.toString().replaceAll("xmlns=\"\"", "");
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

    public String getDocumentFilePath() {
        return documentFilePath;
    }


    private String updatePreprocessXSLT(Document docXML) throws Exception {

        Source srcXSL =  new StreamSource(getStreamFromResources(getClass().getClassLoader(), "update_preprocess_xslt.xsl"));
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(srcXSL);

        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        transformer.transform(new DOMSource(docXML), sr);
        String xmlResult = resultWriter.toString();

        return xmlResult;
    }

    private String readValue(String xpath) {
        String value = "";
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression query = xPath.compile(xpath);
            Node textElement = (Node)query.evaluate(sourceXML, XPathConstants.NODE);
            if(textElement != null) {
                value = textElement.getTextContent();
            }
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
        return value;
    }

    public boolean hasAnnotations() {
        String element_review =  readValue("//*[local-name() = 'review'][1]");
        return element_review.length() != 0;
    }

    // find tag 'table' or 'dl'
    public boolean hasTables() {
        String element_table = readValue("//*[local-name() = 'table' or local-name() = 'dl'][1]");
        return element_table.length() != 0;
    }

    public boolean hasMath() {
        String element_math = readValue("//*[local-name() = 'math'][1]");
        return element_math.length() != 0;
    }
}
