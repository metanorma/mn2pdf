package org.metanorma.fop.ifhandler;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.metanorma.fop.form.FormItem;
import org.metanorma.utils.LoggerHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;
import java.util.logging.Logger;

public class FOPIFFormsHandler extends DefaultHandler {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private final String XMLHEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final String METANORMA_FORM_START_PREFIX = "_metanorma_form_start";
    private final String METANORMA_FORM_ITEM_PREFIX = "_metanorma_form_item_";
    private final String FIELD_NAME_PREFIX = "___name_";
    private final Character SIGN_GREATER = '>';

    private List<String> listResult = new ArrayList<>();

    private String rootXMLNS = "";

    private String previousElement = "";

    boolean isBorderAroundElement = false;

    private int pageHeight = 0;
    Stack<Viewport> stackViewports = new Stack<>();

    private int page;

    private String formName;

    private int fontSize = 11;

    private String fontColor = "#000000";

    FormItem currFormItem;
    List<FormItem> formItems = new ArrayList<>();

    boolean isViewportProcessing = false;

    StringBuilder sbViewport = new StringBuilder();

    Stack<String> stackElements = new Stack<>();

    Stack<Character> stackChar = new Stack<>();

    Stack<Boolean> skipElements = new Stack<>();

    @Override
    public void startDocument() {
        //sbResult.append(XMLHEADER);
        listResult.add(XMLHEADER);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {

        stackElements.push(qName);

        if (rootXMLNS.isEmpty()) {
            rootXMLNS = copyAttributes(attr);
        }

        switch (qName) {
            case "page":
                page++;
                String attr_height = attr.getValue("height");
                if (attr_height != null) {
                    pageHeight = Integer.valueOf(attr_height);
                }
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
                    String translate_value = attr_transform.substring(attr_transform.indexOf("(") + 1, attr_transform.indexOf(")"));

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
        }


        // From common.form.xsl:
        // _metanorma_form_start - fixed prefix, start of the form
        if (qName.equals("id") && attr.getValue("name").startsWith(METANORMA_FORM_START_PREFIX)) {
            String value = attr.getValue("name");
            String[] values = value.split("___");
            if (values.length == 3 && !values[2].isEmpty()) {
                formName = values[2];
            } else if (values.length == 2 && !values[1].isEmpty()) {
                formName = values[1];
            } else {
                formName = value;
            }
            skipElements.push(false);
            copyStartElement(qName, attr);
            return;
        }
        
        // From common.form.xsl:
        // _metanorma_form_item - fixed prefix
        // _border - for determine the size of the form element
        // _textfield_ - form element type
        // fname - form element id
        // ___name_ - prefix before form element name
        // fname - form element name
        /* Example:
        <id name="_metanorma_form_item_border_textfield_fname___name_fname"/>
        <text x="0" y="355376" foi:struct-ref="53"> </text>
        <border-rect x="605" y="345829" width="45120" height="12989" top="(solid,#000000,1000)" bottom="(solid,#000000,1000)" left="(solid,#000000,1000)" right="(solid,#000000,1000)" inner-background-color="#ffffff"/>
        */

        // if next item is border around the form element
        if (qName.equals("id") && attr.getValue("name").startsWith(METANORMA_FORM_ITEM_PREFIX + "border_")) {
            // example: <id name="_metanorma_form_item_border_textfield_birthday"/>
            isBorderAroundElement = true;
            // skip
            skipElements.push(true);
            return;
        }
        // text element with hair space after <id name="_metanorma_form_item_border_..." -->
        if (qName.equals("text") && isBorderAroundElement && !previousElement.equals("form_item_id")) {
            // example: <text x="0" y="525992" foi:struct-ref="6f"> </text>
            skipElements.push(true);
            return;
        }
        if (qName.equals("border-rect") && isBorderAroundElement) {
            // example: <border-rect x="913" y="517369" width="59750" height="11988" top="(solid,#000000,1000)" bottom="(solid,#000000,1000)" left="(solid,#000000,1000)" right="(solid,#000000,1000)" inner-background-color="#ffffff"/>
            float border_x1 = stackViewports.peek().getX() + Integer.parseInt(attr.getValue("x"));
            float border_y1 = stackViewports.peek().getY() + Integer.parseInt(attr.getValue("y"));
            float border_x2 = border_x1 + Integer.parseInt(attr.getValue("width"));
            float border_y2 = border_y1 + Integer.parseInt(attr.getValue("height"));
            PDRectangle pdRectangle = new PDRectangle();
            pdRectangle.setLowerLeftX(border_x1 / 1000);
            pdRectangle.setLowerLeftY((pageHeight - border_y1) / 1000);
            pdRectangle.setUpperRightX(border_x2 / 1000);
            pdRectangle.setUpperRightY((pageHeight - border_y2) / 1000) ;
            currFormItem = new FormItem(pdRectangle, page);
            skipElements.push(true);
            return;
        }
        if (qName.equals("id") && attr.getValue("name").startsWith("_metanorma_form_item_")) {
            // example: <id name="_metanorma_form_item_textfield_birthday"/>

            String attname =  attr.getValue("name");


            /// _metanorma_form_item_border____form_item_type___id___name___value -->
			// split by '___': [1] - form_item_type, [2] - id, [3] - name, [4] - value -->
            String[] values = attname.split("___");


            //determine field type
            //String value = attname.substring(METANORMA_FORM_ITEM_PREFIX.length());
            //String field_type = value.substring(0, value.indexOf("_"));
            String field_type = values[1].replace("_", "");
            currFormItem.setType(field_type);

            //determine field name
            //String field_name = attname.substring((METANORMA_FORM_ITEM_PREFIX + field_type + "_").length());
            //field_name = field_name.substring(field_name.indexOf(FIELD_NAME_PREFIX) + FIELD_NAME_PREFIX.length());
            String field_name = values[3];
            String field_name_unique = field_name;
            if (!field_type.equals("radiobutton")) {
                int iter = 1;
                boolean is_field_name_unique = false;
                while (!is_field_name_unique) {

                    final String field_name_new;
                    if (iter == 1) {
                        field_name_new = field_name;
                    } else if (iter == 2) {
                        field_name_new = formName + "_" + field_name;
                    } else {
                        field_name_new = formName + "_" + field_name + iter;
                    }

                    FormItem foundItem = formItems.stream()
                            .filter(item -> item.getName().equals(field_name_new))
                            .findFirst()
                            .orElse(null); // Returns null if no match is found
                    if (foundItem != null) {
                        iter ++;
                    } else {
                        is_field_name_unique = true;
                        field_name_unique = field_name_new;
                    }
                }

                if (!field_name.equals(field_name_unique)) {
                    logger.warning("Form element " + field_type + " with name '" + field_name +  "' exists already in the document. For uniqueness, the name changed to '" + field_name_unique + "'.");
                }
            }

            currFormItem.setName(field_name_unique);
            currFormItem.setFontSize(fontSize / 1000.0);
            currFormItem.setFontColor(fontColor);
            formItems.add(currFormItem);

            previousElement = "form_item_id";
            skipElements.push(false);
            copyStartElement(qName, attr);
            return;
        }

        if (qName.equals("text") && isBorderAroundElement && previousElement.equals("form_item_id")) {
            // example: <text x="59542" y="539192" foi:struct-ref="75">__________ </text>
            //skipElements.push(true);
            // Todo: replace to hair space - for previous ID work
            skipElements.push(false);
            copyStartElement(qName, attr);
            return;
        }

        if (qName.equals("font")) {
            String attr_font = attr.getValue("size");
            if (attr_font != null) {
                // Example: 10500
                fontSize = Integer.valueOf(attr_font);
            }
            String attr_color = attr.getValue("color");
            if (attr_color != null) {
                // Example "#000000"
                fontColor = attr_color;
            }
        }

        isBorderAroundElement = false;

        skipElements.push(false);

        copyStartElement(qName, attr);

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
            //sbResult.append(sbTmp.toString());
            listResult.add(sbTmp.toString());
        }
    }

    private String copyAttributes(Attributes attr) {
        StringBuilder sbTmp = new StringBuilder();
        for (int i = 0; i < attr.getLength(); i++) {
            sbTmp.append(" ");
            String attName = attr.getLocalName(i);
            sbTmp.append(attName);
            sbTmp.append("=\"");
            String value = StringEscapeUtils.escapeXml(attr.getValue(i));
            if (attName.equals("name") && value.startsWith(METANORMA_FORM_START_PREFIX)) {
                // restore id from string starts with '_metanorma_form_start_id'
                String[] values = value.split("___");
                value = values[1];
            }  else if (attName.equals("name") && value.startsWith(METANORMA_FORM_ITEM_PREFIX)) {
                // restore id from string _metanorma_form_item_border____form_item_type___id___name___value
                // split string  by '___'
                String[] values = value.split("___");
                try {
                    value = values[2];
                } catch (Exception ex) {
                    value = value.substring(METANORMA_FORM_ITEM_PREFIX.length() + 1);
                }
            }
            sbTmp.append(value);
            sbTmp.append("\"");
        }
        return sbTmp.toString();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        stackElements.pop();

        if (skipElements.contains(true)) {
            // skip
        } else {

            switch (qName) {
                case "viewport":
                case "g":
                    stackViewports.pop();
                    break;
                default:
                    break;
            }

            copyEndElement(qName, listResult);

        }
        //previousElement = qName;
        skipElements.pop();
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
    private void copyEndElement(String qName, List<String> list) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            list.add("/>");
        } else {
            list.add("</");
            list.add(qName);
            list.add(">");
        }
        stackChar.pop();
    }

    @Override
    public void characters(char character[], int start, int length) throws SAXException {
        if (skipElements.contains(true)) {
            return;
        }
        String str = new String(character, start, length);
        str = StringEscapeUtils.escapeXml(str);
        if (!str.isEmpty()) {
            if (isViewportProcessing) {
                updateStackChar(sbViewport);
                sbViewport.append(str);
            } else {
                updateStackChar(listResult);
                listResult.add(str);
            }
        }
    }

    private void updateStackChar(StringBuilder sb) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            sb.append(stackChar.pop());
            stackChar.push(Character.MIN_VALUE);
        }
    }

    private void updateStackChar(List<String> list) {
        if (!stackChar.isEmpty() && stackChar.peek().compareTo(SIGN_GREATER) == 0) {
            list.add(stackChar.pop().toString());
            stackChar.push(Character.MIN_VALUE);
        }
    }

    public String getResultedXML() {
        if (listResult.size() == 0) {
            return "";
        }
        int size = 0;
        for (String item: listResult) {
            size +=  item.length();
        }
        StringBuilder sbResult = new StringBuilder(size);
        for (String item: listResult) {
            sbResult.append(item);
        }
        listResult.clear();
        return sbResult.toString();
    }

    public List<FormItem> getFormsItems() {
        return formItems;
    }

}
