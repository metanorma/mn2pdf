package org.metanorma.fop.ifhandler;

import org.metanorma.fop.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class FOPIFIndexHandler extends DefaultHandler {

    private StringBuilder elementValue;

    private String id_name;

    private List<String> indexItems = new ArrayList<>();

    @Override
    public void startDocument() throws SAXException {
        indexItems.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index>");
    }


    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {
        switch (qName) {
            case "id":
                id_name = attr.getValue("name");
                break;
            case "text":
                elementValue = null;
                break;
            default:
                id_name = null;
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "id":
                break;
            case "text":
                if (elementValue.toString().matches("^[0-9]+$")) {
                    if (id_name != null ) { //&& !id_name.isEmpty()
                        indexItems.add("<item id=\"" + id_name + "\">" + elementValue + "</item>");
                    }
                }
                id_name = null;
                elementValue = null;
                break;
            default:
                id_name = null;
                elementValue = null;
                break;
        }
    }
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (elementValue == null) {
            elementValue = new StringBuilder();
        }
        String str = new String(ch, start, length);
        str = Util.escapeXMLEntities(str);
        elementValue.append(str);
    }

    @Override
    public void endDocument() throws SAXException {
        indexItems.add("</index>");
    }

    public String getIndexItems() {
        return String.join("\n", indexItems);
    }

}
