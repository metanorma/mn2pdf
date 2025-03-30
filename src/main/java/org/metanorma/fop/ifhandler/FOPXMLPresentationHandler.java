package org.metanorma.fop.ifhandler;

import org.apache.commons.lang3.StringEscapeUtils;
import org.metanorma.fop.PDFResult;
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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/*
 * This class is intended for:
 * - removing the semantic part from Metanorma XML
 * - extract embedded images in base64 to binary format into temporary folder on disk
 */

public class FOPXMLPresentationHandler extends DefaultHandler {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private final String XMLHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final Character SIGN_GREATER = '>';

    private StringBuilder sbResult = new StringBuilder();

    private String currentElement;

    Stack<Character> stackChar = new Stack<>();

    Stack<Boolean> skipElements = new Stack<>();

    @Override
    public void startDocument() {
        sbResult.append(XMLHEADER);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {

        currentElement = qName;

        if (qName.startsWith("semantic__") || qName.equals("emf") || qName.equals("stem")) {
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

        if (qName.equals("semx")) {
            // skip element name `semx`
        } else {

            updateStackChar(sbTmp);

            sbTmp.append("<");
            sbTmp.append(qName);
            sbTmp.append(copyAttributes(attr));

            stackChar.push(SIGN_GREATER);
        }

        sbResult.append(sbTmp.toString());
    }

    private String copyAttributes(Attributes attr) {
        StringBuilder sbTmp = new StringBuilder();
        for (int i = 0; i < attr.getLength(); i++) {
            sbTmp.append(" ");
            String attrName = attr.getLocalName(i);
            String attrValue = attr.getValue(i);
            sbTmp.append(attrName);
            sbTmp.append("=\"");

            String value = StringEscapeUtils.escapeXml(attrValue);;

            boolean isExtractedImage = false;

            if (currentElement.equals("image") && attrName.equals("src") &&
                    (attrValue.startsWith("data:image/") || attrValue.startsWith("data:application/")) &&
                    !(attrValue.startsWith("data:image/svg+xml;"))) {
                String dataPrefix = "data:image/";
                if (attrValue.startsWith("data:application/")) {
                    dataPrefix = "data:application/";
                }
                // extract embedded images in base64 to binary format into temporary folder on disk
                int startPos = attrValue.indexOf(";base64,") + 8;
                String base64data = attrValue.substring(startPos);
                byte[] decodedBytes = Base64.getDecoder().decode(base64data);

                String imageFormat = attrValue.substring(attrValue.indexOf(dataPrefix) + dataPrefix.length(), attrValue.indexOf(";base64,"));
                PDFResult pdfResult = PDFResult.PDFResult(null);
                String imageTmpName = UUID.randomUUID().toString() + "." + imageFormat;
                Path imagePath = Paths.get(pdfResult.getOutTmpImagesPath().toString(), imageTmpName);
                try {
                    Files.createDirectories(pdfResult.getOutTmpImagesPath());
                    Files.write(imagePath, decodedBytes);
                    // relative path to PDF out file
                    //File imageFile = new File(imagePath.toString());
                    //String imageFileParentFolder = imageFile.getParentFile().getName();
                    //value = Paths.get(imageFileParentFolder, imageTmpName).toString();
                    // absolutepath
                    value = imagePath.toAbsolutePath().toString();
                    isExtractedImage = true;
                } catch (IOException ex) {
                    logger.severe("Can't save the image on disk '" + imagePath.toString() + "':");
                    logger.severe(ex.getMessage());
                    ex.printStackTrace();
                }
            }
            sbTmp.append(value);
            sbTmp.append("\"");

            if (isExtractedImage) {
                sbTmp.append(" extracted=\"true\"");
            }
        }
        return sbTmp.toString();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (skipElements.contains(true)) {
            // skip
        } else {
            if (qName.equals("semx")) {
                // skip element name `semx`
            } else {
                copyEndElement(qName);
            }

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
