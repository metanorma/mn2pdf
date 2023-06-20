package org.metanorma.fop.ifhandler;

import org.apache.commons.lang3.StringEscapeUtils;
import org.metanorma.fop.Util;
import org.metanorma.utils.LoggerHelper;
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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Logger;

/*
 * This class is replacement of add_hidden_math.xsl for fast Apache IF XML processing (adding hidden Math text)
 */

public class FOPIFHiddenMathHandler extends DefaultHandler {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private final String XMLHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final Character SIGN_GREATER = '>';

    private StringBuilder sbResult = new StringBuilder();

    private String rootXMLNS = "";

    private String previousElement = "";

    Map<String,InstreamForeignObject> mapInstreamForeignObjects = new HashMap<>();

    String strPrecedingInlineTextStructId = "";

    private String imageRef = "";

    boolean isViewportProcessing = false;

    StringBuilder sbViewport = new StringBuilder();

    Stack<String> stackElements = new Stack<>();

    Stack<Character> stackChar = new Stack<>();

    @Override
    public void startDocument() {
        sbResult.append(XMLHEADER);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {

        stackElements.push(qName);

        if (rootXMLNS.isEmpty()) {
            rootXMLNS = copyAttributes(attr);
        }

        switch (qName) {
            case "fo:inline":
                previousElement = "fo:inline";
                copyStartElement(qName, attr);
                break;
            case "marked-content":
                if (previousElement.equals("fo:inline")) {
                    strPrecedingInlineTextStructId = attr.getValue("foi:struct-id");
                }
                copyStartElement(qName, attr);
                break;
            case "fo:instream-foreign-object":
                String alt_text = StringEscapeUtils.escapeXml(attr.getValue("fox:alt-text"));
                String struct_id = attr.getValue("foi:struct-id");
                if (alt_text != null && !alt_text.isEmpty()) { // && previousElement.equals("fo:inline")
                    mapInstreamForeignObjects.put(struct_id, new InstreamForeignObject(struct_id, strPrecedingInlineTextStructId, alt_text));
                }
                copyStartElement(qName, attr);
                break;
            case "viewport":
                isViewportProcessing = true;
                copyStartElement(qName, attr);
                break;
            default:
                copyStartElement(qName, attr);
                break;
        }
        previousElement = qName;
    }

    private void copyStartElement(String qName, Attributes attr) {
        StringBuilder sbTmp = new StringBuilder();

        updateStackChar(sbTmp);

        sbTmp.append("<");
        sbTmp.append(qName);
        sbTmp.append(copyAttributes(attr));

        stackChar.push(SIGN_GREATER);

        if (isViewportProcessing) {
            sbViewport.append(sbTmp.toString());
        } else {
            sbResult.append(sbTmp.toString());
        }
    }

    private String copyAttributes(Attributes attr) {
        StringBuilder sbTmp = new StringBuilder();
        for (int i = 0; i < attr.getLength(); i++) {
            sbTmp.append(" ");
            sbTmp.append(attr.getLocalName(i));
            sbTmp.append("=\"");
            String value = StringEscapeUtils.escapeXml(attr.getValue(i));
            sbTmp.append(value);
            sbTmp.append("\"");
        }
        return sbTmp.toString();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        stackElements.pop();

        if (isViewportProcessing) {
            copyEndElement(qName, sbViewport);

            if (qName.equals("viewport") && !stackElements.contains("viewport")) { // for top-level viewport
                isViewportProcessing = false;

                String strViewport =  sbViewport.toString();
                if (strViewport.contains("<math")) {
                    //System.out.println("Contains Math");

                    try {
                        Source srcXSL = new StreamSource(Util.getStreamFromResources(getClass().getClassLoader(), "add_hidden_math_partial.xsl"));
                        TransformerFactory factory = TransformerFactory.newInstance();
                        Transformer transformer = factory.newTransformer(srcXSL);
                        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");

                        String strViewportXML = sbViewport.insert(0, XMLHEADER + "<envelope " + rootXMLNS + ">").append("</envelope>").toString();

                        StreamSource sourceXML =  new StreamSource(new StringReader(strViewportXML));

                        StringBuilder sbInstreamForeignObjects = new StringBuilder();
                        sbInstreamForeignObjects.append(XMLHEADER);
                        sbInstreamForeignObjects.append("<data>\n");
                        for (Map.Entry<String, InstreamForeignObject> entry : mapInstreamForeignObjects.entrySet()) {
                            String key = entry.getKey();
                            InstreamForeignObject value = entry.getValue();
                            sbInstreamForeignObjects.append(value.toString());
                        }
                        sbInstreamForeignObjects.append("</data>");

                        InputSource xmlIF_IFO = new InputSource(new StringReader(sbInstreamForeignObjects.toString()));
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document xmlIFDocument = dBuilder.parse(xmlIF_IFO);
                        NodeList xmlIFDocumentNodeList = xmlIFDocument.getDocumentElement().getChildNodes();
                        transformer.setParameter("ifo", xmlIFDocumentNodeList);

                        StringWriter resultWriter = new StringWriter();
                        StreamResult sr = new StreamResult(resultWriter);
                        transformer.transform(sourceXML, sr);
                        String xmlResult = resultWriter.toString();

                        sbResult.append(xmlResult);
                    } catch (Exception ex) {
                        logger.severe(ex.toString());
                        sbResult.append(strViewport);
                    }

                } else {
                    sbResult.append(strViewport);
                }
                sbViewport.setLength(0);
            }
        } else {
            if (qName.equals("page-sequence")) {
                mapInstreamForeignObjects.clear();
            }
            copyEndElement(qName, sbResult);
        }
        previousElement = qName;
    }

    private void copyEndElement(String qName, StringBuilder sb) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            sb.append("/>");
        } else {
            sb.append("</");
            sb.append(qName);
            sb.append(">");
        }
        stackChar.pop();
    }

    @Override
    public void characters(char character[], int start, int length) throws SAXException {

        String str = new String(character, start, length);
        str = StringEscapeUtils.escapeXml(str);
        if (!str.isEmpty()) {
            if (isViewportProcessing) {
                updateStackChar(sbViewport);
                sbViewport.append(str);
            } else {
                updateStackChar(sbResult);
                sbResult.append(str);
            }
        }
    }

    private void updateStackChar(StringBuilder sb) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            sb.append(stackChar.pop());
            stackChar.push(Character.MIN_VALUE);
        }
    }

    public StringBuilder getResultedXML() throws IOException {
        //return sbResult.toString(); // https://github.com/metanorma/mn2pdf/issues/214#issuecomment-1599200350
        return sbResult;
    }

}
