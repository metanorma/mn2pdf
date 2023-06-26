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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

/*
 * This class is intended for removing the semantic part from Metanorma XML
 */

public class FOPXMLPresentationHandler extends DefaultHandler {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private final String XMLHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final Character SIGN_GREATER = '>';

    private StringBuilder sbResult = new StringBuilder();

    Stack<Character> stackChar = new Stack<>();

    Stack<Boolean> skipElements = new Stack<>();

    @Override
    public void startDocument() {
        sbResult.append(XMLHEADER);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {

        if (qName.startsWith("semantic__")) {
            // skip
            skipElements.push(true);
        } else {
            skipElements.push(false);
            if (skipElements.contains(true)) {
                // skip
            } else {
                copyStartElement(qName, attr);
            }
        }
    }

    private void copyStartElement(String qName, Attributes attr) {
        StringBuilder sbTmp = new StringBuilder();

        updateStackChar(sbTmp);

        sbTmp.append("<");
        sbTmp.append(qName);
        sbTmp.append(copyAttributes(attr));

        stackChar.push(SIGN_GREATER);

        sbResult.append(sbTmp.toString());
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

        if (skipElements.contains(true)) {
            // skip
        } else {
            copyEndElement(qName);
        }
        skipElements.pop();
    }

    private void copyEndElement(String qName) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            sbResult.append("/>");
        } else {
            sbResult.append("</");
            sbResult.append(qName);
            sbResult.append(">");
        }
        stackChar.pop();
    }

    @Override
    public void characters(char character[], int start, int length) throws SAXException {
        if (skipElements.contains(true)) {
            // skip
        } else {
            String str = new String(character, start, length);
            str = StringEscapeUtils.escapeXml(str);
            if (!str.isEmpty()) {
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

    public StringBuilder getResultedXML() {
        return sbResult;
    }
}
