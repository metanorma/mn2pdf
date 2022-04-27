package org.metanorma.fop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.StringReader;

import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import static org.metanorma.fop.Util.logger;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author Alexander Dyuzhev
 */
public class PDFTextAnnotationStripper extends PDFTextStripper {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private boolean DEBUG = false;
    
    private String reviewer = "";
    
    private String annotation_text = "";
    
    private String criteria = "";
    
    private float x = 0f;
    private float y = 0f;
    
    private Calendar date = null;
    
    private boolean highlighted = false;
    
    public PDFTextAnnotationStripper(String author, Calendar date, String text, String criteria, float x, float y)  throws IOException {
        super();
        this.reviewer = author;
        this.date = date;
        this.annotation_text = text;
        this.criteria = criteria;
        this.x = x;
        this.y = y;
    }

    
    /**
     * Override the default functionality of PDFTextStripper.writeString()
     */

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        boolean isFound = false;
        float posXInit  = 0, 
              posXEnd   = 0, 
              posYInit  = 0,
              posYEnd   = 0,
              width     = 0, 
              height    = 0, 
              fontHeight = 0;
        //String[] criteria = {"proflie"}; //, "Word2", "Word3", ....

        /*for (int i = 0; i < criteria.length; i++) {
            if (string.contains(criteria[i])) {
                isFound = true;
            } 
        }*/
        
        boolean start_pos_found = false;
        boolean end_pos_found = false;
        
        if (!criteria.isEmpty()) {
        
            if (DEBUG) {
                System.out.println("Current string: '" + string + "'");
            }
            
            if ((criteria.equals('\u200a') && !string.contains(criteria)) ||
                (!highlighted && string.contains(criteria)) ) {

                if (criteria.equals('\u200a') && !string.contains(criteria)) {
                    posXInit = x;
                    posXEnd  = x+20;
                    posYInit = y;
                    posYEnd  = y+20;
                    width    = 20;
                    height   = 20;
                    highlighted = true;
                }
                if (!highlighted && !criteria.isEmpty() && string.contains(criteria))  {

                    if (DEBUG) {
                        System.out.println("Determine exactly start position:");
                    }
                    
                    int start_pos = 0;

                    if (DEBUG && string.contains("proflie") || string.contains("power system")) {
                        System.out.println("DEBUG found");
                    }
                    
                    for(int j = 0; j < textPositions.size(); j++) {
                        //System.out.println("Char: " + textPositions.get(j).getUnicode());
                        // for compare 100.12 (IF xml) and 10.1234 (PDFBox) 
                        if (Math.abs(textPositions.get(j).getXDirAdj() - x) < 0.01 && Math.abs(textPositions.get(j).getYDirAdj() - y) < 0.01) {
                            start_pos = j;
                            if (DEBUG) {
                                System.out.println("Start position: " + j);
                            }
                            start_pos_found = true;
                            break;
                        }
                    }

                    

                    if (DEBUG) {
                        System.out.println("Determine exactly end position:");
                    }
                    int end_pos = textPositions.size() - 1;

                    StringBuilder phrase = new StringBuilder();
                    for(int j = start_pos; j < textPositions.size(); j++) {
                        //System.out.println(textPositions.get(j).getUnicode());
                        phrase.append(textPositions.get(j).getUnicode());
                        if (DEBUG && string.contains("power ")) {
                            System.out.println(phrase.toString());
                        }
                        if (phrase.toString().equals(criteria)) {
                            end_pos = j;
                            if (DEBUG) {
                                System.out.println("End position: " + j);
                            }
                            end_pos_found = true;
                            break;
                        }
                    }

                    
                    
                    posXInit = textPositions.get(start_pos).getXDirAdj();
                    posXEnd  = textPositions.get(end_pos).getXDirAdj() + textPositions.get(end_pos).getWidth();
                    posYInit = textPositions.get(start_pos).getPageHeight() - textPositions.get(start_pos).getYDirAdj();
                    posYEnd  = textPositions.get(start_pos).getPageHeight() - textPositions.get(end_pos).getYDirAdj();
                    width    = textPositions.get(start_pos).getWidthDirAdj();
                    height   = textPositions.get(start_pos).getHeightDir();
                }


                //System.out.println(string + " X-Init = " + posXInit + "; Y-Init = " + posYInit + "; X-End = " + posXEnd + "; Y-End = " + posYEnd + "; Font-Height = " + fontHeight);

                /* numeration is index-based. Starts from 0 */

                float quadPoints[] = {posXInit, posYEnd + height + 2, posXEnd, posYEnd + height + 2, posXInit, posYInit - 2, posXEnd, posYEnd - 2};

                if (start_pos_found && end_pos_found) {
                
                    List<PDAnnotation> annotations = document.getPage(this.getCurrentPageNo() - 1).getAnnotations();
                    PDAnnotationTextMarkup highlight = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);

                    PDRectangle position = new PDRectangle();
                    position.setLowerLeftX(posXInit);
                    position.setLowerLeftY(posYEnd);
                    position.setUpperRightX(posXEnd);
                    position.setUpperRightY(posYEnd + height);

                    highlight.setRectangle(position);

                    // quadPoints is array of x,y coordinates in Z-like order (top-left, top-right, bottom-left,bottom-right) 
                    // of the area to be highlighted

                    highlight.setQuadPoints(quadPoints);

                    PDColor orange = new PDColor(new float[]{1, 195 / 255F, 51 / 255F}, PDDeviceRGB.INSTANCE);
                    highlight.setColor(orange);
                    highlight.setConstantOpacity(0.3f); // 30% transparent

                    //PDAnnotationPopup popup = new PDAnnotationPopup();
                    //highlight.setPopup(popup);
                    //highlight.setAnnotationName("My Annotation Name");
                    highlight.setContents(annotation_text);
                    //highlight.setIntent("My intent");
                    //highlight.setRichContents("Rich content");
                    highlight.setSubject("Highlight");
                    highlight.setTitlePopup(reviewer);
                    if (date != null) {
                        highlight.setModifiedDate(date);
                    }
                    //highlight.setModifiedDate(new Calendar("20180125T0121"));
                    annotations.add(highlight);
                    if (DEBUG) {
                        System.out.println("The string '" + criteria + "' highlighted!");
                    }
                } else {
                    if (DEBUG) {
                        System.out.println("The string '" + criteria + "' can't be found and highlighted!");
                    }
                }
            }
        }
    }

   
}