package org.metanorma.fop.ifhandler;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Stack;

public class FOPIFFlatHandler extends DefaultHandler {

    private final String XMLHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final Character SIGN_GREATER = '>';

    private StringBuilder sbResult = new StringBuilder();

    Stack<Character> stackChar = new Stack<>();

    Stack<Viewport> stackViewports = new Stack<>();

    private String currElement = "";

    Stack<Boolean> skipElements = new Stack<>();

    @Override
    public void startDocument() throws SAXException {
        sbResult.append(XMLHEADER);
        sbResult.append("<document>\n");
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {

        currElement = qName;

        if (qName.equals("viewport") && attr.getValue("region-type") != null && (attr.getValue("region-type").equals("Footer") || attr.getValue("region-type").equals("Header"))) {
            // skip
            skipElements.push(true);
        } else if (qName.equals("svg")) {
            // skip svg element due text/@x @y attributes similar to IF attributes
            skipElements.push(true);
        } else {

            skipElements.push(false);

            if (skipElements.contains(true)) {
                // skip
            } else {

                switch (qName) {
                    case "viewport":
                    case "g":

                        int x = 0;
                        int y = 0;

                        if (!stackViewports.isEmpty()) {
                            x = stackViewports.peek().getX();
                            y = stackViewports.peek().getY();
                        }

                        int value_x = 0;
                        int value_y = 0;

                        String attr_transform = attr.getValue("transform");
                        if (attr_transform != null && attr_transform.startsWith("translate")) {
                            String translate_value = attr_transform.substring(attr_transform.indexOf("(")+1, attr_transform.indexOf(")"));

                            if (translate_value.contains(",")) { // Example: transform="translate(70866,36000)"
                                value_x = Integer.valueOf(translate_value.substring(0, translate_value.indexOf(",")));
                            } else { // Example: transform="translate(70866)"
                                value_x = Integer.valueOf(translate_value);
                            }

                            if (translate_value.contains(",")) { // Example: transform="translate(70866,36000)"
                                value_y = Integer.valueOf(translate_value.substring(translate_value.indexOf(",") + 1));
                            } else { // Example: transform="translate(70866)"
                                value_y = 0;
                            }
                        }

                        int new_x = x + value_x;
                        int new_y = y + value_y;

                        stackViewports.push(new Viewport(new_x, new_y));

                        break;
                    case "id":
                        copyStartElement(qName, attr);
                        copyEndElement(qName);
                        break;
                    case "page":
                        copyStartElement(qName, null); // null - no need attributes for <page
                        copyEndElement(qName);
                        break;
                    case "text":
                        float text_x = (stackViewports.peek().getX() + Float.parseFloat(attr.getValue("x"))) / 1000;
                        float text_y = (stackViewports.peek().getY() + Float.parseFloat(attr.getValue("y"))) / 1000;

                        copyStartElement(qName, null);

                        sbResult.append(" x=\"");
                        sbResult.append(String.valueOf(text_x));

                        sbResult.append("\" y=\"");
                        sbResult.append(String.valueOf(text_y));
                        sbResult.append("\"");

                        break;
                    default:

                        break;
                }
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
        if (attr != null) {
            for (int i = 0; i < attr.getLength(); i++) {
                if (attr.getLocalName(i).equals("x") || attr.getLocalName(i).equals("y") || attr.getLocalName(i).equals("name")) {
                    sbTmp.append(" ");
                    sbTmp.append(attr.getLocalName(i));
                    sbTmp.append("=\"");
                    String value = StringEscapeUtils.escapeXml(attr.getValue(i));
                    sbTmp.append(value);
                    sbTmp.append("\"");
                }
            }
        }
        return sbTmp.toString();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        currElement = "";
        if (skipElements.contains(true)) {
            // skip
        } else {
            switch (qName) {
                case "viewport":
                case "g":
                    stackViewports.pop();
                    break;
                case "text":
                    copyEndElement(qName);
                    break;
                default:
                    break;
            }
        }
        skipElements.pop();
    }

    private void copyEndElement(String qName) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            sbResult.append("/>\n");
        } else {
            sbResult.append("</");
            sbResult.append(qName);
            sbResult.append(">\n");
        }
        stackChar.pop();
    }

    @Override
    public void characters(char character[], int start, int length) throws SAXException {
        if (skipElements.contains(true)) {
            // skip
        } else {
            if (currElement.equals("text")) {
                String str = new String(character, start, length);
                str = StringEscapeUtils.escapeXml(str);
                if (!str.isEmpty()) {
                    updateStackChar(sbResult);
                    sbResult.append(str);
                }
            }
        }
    }

    @Override
    public void endDocument() throws SAXException {
        sbResult.append("</document>");
    }

    private void updateStackChar(StringBuilder sb) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            sb.append(stackChar.pop());
            stackChar.push(Character.MIN_VALUE);
        }
    }

    public String getResultedXML() {
        return sbResult.toString();
    }

}
