package org.metanorma.fop.ifhandler;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class FOPIFHiddenMathHandler extends DefaultHandler {

    private final String XMLHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private StringBuilder result = new StringBuilder();

    private List<String> xmlItems = new ArrayList<>();

    private String id_name;

    private StringBuilder charBuffer = new StringBuilder();

    private String previousElement = "";
    Map<String,InstreamForeignObject> mapInstreamForeignObjects = new HashMap<>();

    String strPrecedingInlineTextStructId = "";

    String strFontFamily = "";

    private String imageRef = "";
    private int imageWidth;
    private int imageHeight;

    boolean isViewportProcessing = false;
    StringBuilder sbViewport = new StringBuilder();

    Deque<String> stackElements = new ArrayDeque<>();

    Deque<String> stackGreaterSign = new ArrayDeque<>();

    @Override
    public void startDocument() {
        result.append(XMLHEADER);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {
        //charBuffer.setLength(0);
        stackElements.add(qName);
        switch (qName) {
            case "fo:inline":
                previousElement = "fo:inline";
                copyElement(qName, attr);
                break;
            case "marked-content":
                if (previousElement.equals("fo:inline")) {
                    strPrecedingInlineTextStructId = attr.getValue("foi:struct-id");
                }
                copyElement(qName, attr);
                break;
            case "fo:instream-foreign-object":
                String alt_text = attr.getValue("fox:alt-text");
                if (alt_text != null && !alt_text.isEmpty() && previousElement.equals("fo:inline")) {
                    mapInstreamForeignObjects.put(attr.getValue("foi:struct-id"), new InstreamForeignObject(strPrecedingInlineTextStructId, alt_text));
                }
                copyElement(qName, attr);
                break;
            case "viewport":
                isViewportProcessing = true;
                copyElement(qName, attr);
                break;
           /*case "font":
                strFontFamily = attr.getValue("font-family");
                copyElement(qName, attr);
                break;
            case "image":
                imageRef = attr.getValue("foi:struct-ref");
                imageWidth = Integer.valueOf(attr.getValue("width"));
                imageHeight = Integer.valueOf(attr.getValue("height"));
                copyElement(qName, attr);
                break;*/
            default:
                copyElement(qName, attr);
                break;
        }
        previousElement = qName;
    }

    private void copyElement(String qName, Attributes attr) {
        StringBuilder sbTmp = new StringBuilder();
        sbTmp.append("<");
        sbTmp.append(qName);
        for (int i = 0; i < attr.getLength(); i++) {
            sbTmp.append(" ");
            sbTmp.append(attr.getLocalName(i));
            sbTmp.append("=\"");
            String value = attr.getValue(i).replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"", "&quot;");
            sbTmp.append(value);
            sbTmp.append("\"");
        }
        sbTmp.append(">");

        if (isViewportProcessing) {
            sbViewport.append(sbTmp.toString());
        } else {
            result.append(sbTmp.toString());
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        stackElements.remove();

        if (isViewportProcessing) {
            sbViewport.append("</");
            sbViewport.append(qName);
            sbViewport.append(">");

            if (qName.equals("viewport") && !stackElements.contains("viewport")) { // for top-level viewport
                isViewportProcessing = false;
                String strViewport = sbViewport.toString();
                if (strViewport.contains("<math")) {
                    System.out.println("Contains Math");

                    try {
                        Source srcXSL = new StreamSource(getStreamFromResources(getClass().getClassLoader(), "add_hidden_math_partial.xsl"));
                        TransformerFactory factory = TransformerFactory.newInstance();
                        Transformer transformer = factory.newTransformer(srcXSL);
                        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");

                        StreamSource sourceXML =  new StreamSource(new StringReader(strViewport));

                        StringBuilder sbInstreamForeignObjects = new StringBuilder();
                        sbInstreamForeignObjects.append(XMLHEADER);
                        sbInstreamForeignObjects.append("<data xmlns:fo=\"http://www.w3.org/1999/XSL/Format\"");
                        sbInstreamForeignObjects.append(" xmlns:foi=\"http://xmlgraphics.apache.org/fop/internal\"");
                        sbInstreamForeignObjects.append(" xmlns:fox=\"http://xmlgraphics.apache.org/fop/extensions\">\n");

                        for (Map.Entry<String, InstreamForeignObject> entry : mapInstreamForeignObjects.entrySet()) {
                            String key = entry.getKey();
                            InstreamForeignObject value = entry.getValue();
                            // <fo:instream-foreign-object fox:alt-text="H" foi:struct-id="a93" preceding_inline_text_struct_id="a92"/>
                            sbInstreamForeignObjects.append("<fo:instream-foreign-object fox:alt-text=\"");
                            sbInstreamForeignObjects.append(value.get_alt_text());
                            sbInstreamForeignObjects.append("\" foi:struct-id=\"");
                            sbInstreamForeignObjects.append(key);
                            sbInstreamForeignObjects.append("\" preceding_inline_text_struct_id=\"");
                            sbInstreamForeignObjects.append(value.get_preceding_inline_text_struct_id());
                            sbInstreamForeignObjects.append("\"/>\n");
                        }

                        sbInstreamForeignObjects.append("</data>");

                        InputSource xmlIFIS = new InputSource(new StringReader(sbInstreamForeignObjects.toString()));
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document xmlIFDocument = dBuilder.parse(xmlIFIS);
                        NodeList xmlIFDocumentNodeList = xmlIFDocument.getDocumentElement().getChildNodes();
                        transformer.setParameter("ifo", xmlIFDocumentNodeList);

                        StringWriter resultWriter = new StringWriter();
                        StreamResult sr = new StreamResult(resultWriter);
                        transformer.transform(sourceXML, sr);
                        String xmlResult = resultWriter.toString();

                        result.append(xmlResult);
                    } catch (Exception ex) {
                        result.append(strViewport);
                    }

                } else {
                    result.append(strViewport);
                }
                sbViewport.setLength(0);
            }
        } else {


            switch (qName) {
                case "id2":
                    break;
                default:
                    /*if (charBuffer.toString().isEmpty()) {
                        result.append("/>");
                    } else {*/
                        result.append("</");
                        result.append(qName);
                        result.append(">");
                    //}
                    break;
            }
        //charBuffer.setLength(0);
        }
        previousElement = qName;
    }


    @Override
    public void characters(char character[], int start, int length) throws SAXException {
        String str = new String(character, start, length);
        str = str.replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll("\"", "&quot;");
        //charBuffer.append(str);
        if (!str.isEmpty()) {
            if (isViewportProcessing) {
                sbViewport.append(str);
            } else {
                result.append(str);
            }
        }
    }

    public String getResultedXML() {
        return result.toString();
    }

    public static InputStream getStreamFromResources(ClassLoader classLoader, String fileName) throws Exception {
        InputStream stream = classLoader.getResourceAsStream(fileName);
        if(stream == null) {
            throw new Exception("Cannot get resource \"" + fileName + "\" from Jar file.");
        }
        return stream;
    }

}
