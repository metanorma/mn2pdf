package org.metanorma.fop;

import java.io.*;
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
import java.util.stream.Collectors;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.metanorma.fop.Util.getStreamFromResources;

import org.apache.fop.fonts.FontConfig;
import org.metanorma.fop.fonts.FOPFont;
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

    private boolean hasAnnotations = false;
    private boolean hasFileAttachmentAnnotations = false;
    private boolean hasTables = false;
    private boolean hasForms = false;

    private boolean isDebugMode = false;

    private Map<String, Integer> tablesCellsCountMap = new HashMap<>();
    private boolean hasMath = false;

    static final String TMPDIR = System.getProperty("java.io.tmpdir");
    static final Path tmpfilepath  = Paths.get(TMPDIR, UUID.randomUUID().toString());

    public static String mainFont = "";

    public static List<String> mainAdditionalFonts = new ArrayList<>();

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
            readMetaInformation();
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
            readMetaInformation();
        } catch (Exception ex) {
            logger.severe("Can't parse source XML.");
            ex.printStackTrace();
        }
    }

    private void readMetaInformation() {
        String element_review =  readValue("//*[local-name() = 'annotation-container' or local-name() = 'annotation'][1]");
        hasAnnotations = element_review.length() != 0;
        String element_link_to_attachment =  readValue("//*[local-name() = 'link' or local-name() = 'fmt-link'][@attachment = 'true'][1]");
        hasFileAttachmentAnnotations = element_link_to_attachment.length() != 0;
        String element_math = readValue("//*[local-name() = 'math'][1]");
        hasMath = element_math.length() != 0;
        //tables without colgroup/col (width) or dl
        //String element_table = readValue("//*[(local-name() = 'table' and not(*[local-name() = 'colgroup']/*[local-name() = 'col'])) or local-name() = 'dl'][1]");
        //hasTables = element_table.length() != 0;
        obtainTablesCellsCount();
        hasTables = !tablesCellsCountMap.isEmpty();
        String element_form = readValue("//*[local-name() = 'form'][1]");
        hasForms = element_form.length() != 0;
        String element_pdf_debug = readValue("//*[local-name() = 'presentation-metadata']/*[local-name() = 'pdf-debug'][1]");
        isDebugMode = element_pdf_debug.equalsIgnoreCase("true");
    }

    private void obtainTablesCellsCount() {
        try {
            XPath xPathAllTable = XPathFactory.newInstance().newXPath();
            // select all tables (without colgroup) and definitions lists (dl)
            XPathExpression queryAllTables = xPathAllTable.compile("//*[(local-name() = 'table' and not(*[local-name() = 'colgroup']/*[local-name() = 'col'])) or local-name() = 'dl']");
            NodeList nodesTables = (NodeList)queryAllTables.evaluate(sourceXML, XPathConstants.NODESET);
            for (int i = 0; i < nodesTables.getLength(); i++) {
                Node nodeTable = nodesTables.item(i);
                String tableId = "";
                Node nodeId = nodeTable.getAttributes().getNamedItem("id");
                if (nodeId != null) {
                    tableId =nodeId.getTextContent();
                }
                if (!tableId.isEmpty()) {
                    XPath xPathTableCountCells = XPathFactory.newInstance().newXPath();
                    XPathExpression queryTableCountCells = xPathTableCountCells.compile(".//*[local-name() = 'td' or local-name() = 'th' or local-name() = 'dt' or local-name() = 'dd']");
                    NodeList nodesCells = (NodeList) queryTableCountCells.evaluate(nodeTable, XPathConstants.NODESET);
                    int countCells = nodesCells.getLength();
                    tablesCellsCountMap.put(tableId, countCells);
                }
            }
        } catch (XPathExpressionException ex) {
            logger.severe(ex.toString());
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
        return getDocumentFonts(null);
    }

    public List<String> getDocumentFonts(fontConfig fontcfg) {

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
                                fname = fname.trim().replace("'","")
                                        .replace("\"","");
                                if (!documentFontList.contains(fname)) {
                                    documentFontList.add(fname);
                                }
                            }
                            if (attr.getOwnerElement().getNodeName().equals("fo:root")) {
                                // Get all fo:root fonts except the first one
                                mainAdditionalFonts = documentFontList.stream()
                                        .skip(1)
                                        .collect(Collectors.toList());
                            }
                        } catch (Exception ex) {}
                    }

                    query = xPath.compile("//*/@style[contains(., 'font-family')]");
                    nList = (NodeList)query.evaluate(srcXML, XPathConstants.NODESET);
                    for (int i = 0; i < nList.getLength(); i++) {
                        try {
                            Attr attr = (Attr) nList.item(i);
                            String attrText = attr.getNodeValue();
                            attrText = attrText.substring(attrText.indexOf("font-family"));
                            attrText = attrText.substring(attrText.indexOf(":") + 1);
                            if (attrText.indexOf(";") != -1) {
                                attrText = attrText.substring(0, attrText.indexOf(";")).trim();
                            }
                            for (String fname: attrText.split(",")) {
                                fname = fname.trim().replace("'","")
                                        .replace("\"","");
                                if (!documentFontList.contains(fname)) {
                                    documentFontList.add(fname);
                                }
                            }
                        } catch (Exception ex) {}
                    }

                    // [@type = 'text/css'] removed for https://github.com/metanorma/mn2pdf/issues/384#issuecomment-3803816045
                    query = xPath.compile("//*/*[local-name() = 'style'][contains(., 'font-family')]/text()");
                    // String textCSS = (String)query.evaluate(srcXML, XPathConstants.STRING);
                    nList = (NodeList)query.evaluate(srcXML, XPathConstants.NODESET);
                    for (int i = 0; i < nList.getLength(); i++) {
                        try {

                            String textCSS = nList.item(i).getTextContent();

                            boolean foundFontFamily = true;
                            while (foundFontFamily) {
                                int fontFamilyStart = textCSS.indexOf("font-family:");
                                foundFontFamily = fontFamilyStart != -1;
                                if (foundFontFamily) {
                                    textCSS = textCSS.substring(fontFamilyStart);
                                    textCSS = textCSS.substring(textCSS.indexOf(":") + 1);
                                    int pos_semicolon = textCSS.indexOf(";");
                                    int pos_curlybrace = textCSS.indexOf("}");
                                    if (pos_semicolon != -1 || pos_curlybrace != -1) {
                                        int pos_end = 0;
                                        if (pos_semicolon == -1) {
                                            pos_semicolon = 0;
                                            pos_end = pos_curlybrace;
                                        } else if (pos_curlybrace == -1) {
                                            pos_curlybrace = 0;
                                            pos_end = pos_semicolon;
                                        }
                                        pos_end = Math.min(pos_semicolon, pos_curlybrace);
                                        
                                        String attrText = textCSS.substring(0, pos_end).trim();
                                        for (String fname : attrText.split(",")) {
                                            fname = fname.trim().replace("'", "")
                                                    .replace("\"", "");

                                            if (!documentFontList.contains(fname)) {
                                                //if (fontcfg != null && fontcfg.hasFontFamily(fname)) {
                                                    documentFontList.add(fname);
                                                    //break;
                                                //}
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            //Experimental feature
                        }
                    }
                    if (!documentFontList.isEmpty()) {
                        mainFont = documentFontList.get(0);
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
            XPathExpression expr = xpath.compile("//*[local-name() = 'metanorma']/*[local-name()='bibdata']//*[local-name()='language']");
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
            XPathExpression query = xPath.compile("//*[local-name() = 'metanorma']/*[local-name()='render']/*[local-name()='preprocess-xslt']/*[local-name()='stylesheet']");
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

    public List<String> readElementsIds(String xpath) {
        List<String> values = new ArrayList<>();
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression query = xPath.compile(xpath);
            NodeList nodes = (NodeList)query.evaluate(sourceXML, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node_id = nodes.item(i).getAttributes().getNamedItem("id");
                if (node_id != null) {
                    String id = node_id.getTextContent();
                    values.add(id);
                }
            }
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
        return values;
    }

    public boolean hasAnnotations() {
        return hasAnnotations;
    }

    public boolean hasFileAttachmentAnnotations() {
        return hasFileAttachmentAnnotations;
    }


    // find tag 'table' or 'dl'
    public boolean hasTables() {
        return hasTables;
    }

    // find tag 'form'
    public boolean hasForms() {
        return hasForms;
    public boolean isDebugMode() {
        return isDebugMode;
    }

    public boolean hasMath() {
        return hasMath;
    }

    public int getCountTableCells() {
        int countTableCells = 0;
        try {
            countTableCells = tablesCellsCountMap.values().stream().mapToInt(Integer::intValue).sum();
        } catch (Exception ex) {
            logger.severe(ex.toString());
        };
        return countTableCells;
    }

    public void flushResources() {
        sourceXML = null;
        sourceXMLstr = "";
        xmlFO = null;
    }

    public Map<String, Integer> getTablesCellsCountMap() {
        return tablesCellsCountMap;
    }
}
