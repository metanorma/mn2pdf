package org.metanorma.fop.annotations;

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
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

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
    
    //private String annotation_text = "";
    
    private String highlight_text = "";
    private boolean doHighlight = false;
    private boolean doPostIt = false;
    
    private float x = 0f;
    private float y = 0f;
    
    AnnotationArea annotationArea = null;
    
    private Calendar date = null;
    
    private boolean processedAlready = false;
    private boolean postItAlready = false;
    private boolean highlightedAlready = false;

    private PDColor orange = new PDColor(new float[]{1, 195 / 255F, 51 / 255F}, PDDeviceRGB.INSTANCE);
    
    
    //String author, Calendar date, String text,
    public PDFTextAnnotationStripper( String highlight_text, 
            boolean doPostIt, boolean doHighlight, 
            float x, float y,
            AnnotationArea annotationArea)  throws IOException {
        super();
        //this.reviewer = author;
        //this.date = date;
        //this.annotation_text = text;
        this.highlight_text = highlight_text;
        
        this.doHighlight = doHighlight;
        this.highlightedAlready = doHighlight == false;
        
        this.doPostIt = doPostIt;
        this.postItAlready = doPostIt == false;
        
        this.x = x;
        this.y = y;
        
        this.annotationArea = annotationArea;
    }

    
    /**
     * Override the default functionality of PDFTextStripper.writeString()
     */

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        
        if (!processedAlready) { // && !highlight_text.isEmpty()
        
            float posXInit  = 0, 
                  posXEnd   = 0, 
                  posYInit  = 0,
                  posYEnd   = 0,
                  width     = 0, 
                  height    = 0, 
                  fontHeight = 0;

            boolean start_pos_found = false;
            boolean end_pos_found = false;

            if (DEBUG) {
                System.out.println("Current string: '" + string + "'");
            }

            
            if (doPostIt && !postItAlready) {
                // add post-it annotation (popup window)

                posXInit = x;
                posXEnd  = x+20;
                float y_shift = 0; // to move a bit higher, otherwise post-it note will overlap highlighted text
                float height_note = 25;
                posYInit = textPositions.get(0).getPageHeight() - y + y_shift;
                posYEnd  = textPositions.get(0).getPageHeight() - y + y_shift + height_note;
                
                PDRectangle position = new PDRectangle();
                position.setLowerLeftX(posXInit);
                position.setLowerLeftY(posYInit);
                position.setUpperRightX(posXEnd);
                position.setUpperRightY(posYEnd);
                
                System.out.println("Position popup:");
                System.out.println("LowerLeftX:" + position.getLowerLeftX());
                System.out.println("LowerLeftY:" + position.getLowerLeftY());
                System.out.println("UpperRightX:" + position.getUpperRightX());
                System.out.println("UpperRightLowerLeftY:" + position.getUpperRightY());
                
                annotationArea.setPosition(position);
                
                
                /*
                List<PDAnnotation> annotations = document.getPage(this.getCurrentPageNo() - 1).getAnnotations();
                
                
                PDAnnotationText text = new PDAnnotationText();

                text.setRichContents("<body xmlns=\"http://www.w3.org/1999/xhtml\">" + 
                    annotation_text + 
                    "</body>");
                //text.setContents("Simple text content.");
                text.setRectangle(position);
                text.setColor(orange);
                text.setOpen(true);
                text.setConstantOpacity(0.6f);
                text.setTitlePopup(reviewer);
                if (date != null) {
                    text.setModifiedDate(date);
                }
                
                annotations.add(text);*/
                
                postItAlready = true;
                
            } 
            
            if (doHighlight && !highlightedAlready && string.contains(highlight_text)) {
                
                if (DEBUG) {
                    System.out.println("Determine exactly start position:");
                }
                
                int start_pos = 0;
                int end_pos = textPositions.size() - 1;

                if (DEBUG && (string.contains("proflie") || string.contains("power system"))) {
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
                } // start position

                if (start_pos_found) { // find end position if start position found only

                    if (DEBUG) {
                        System.out.println("Determine exactly end position:");
                    }

                    StringBuilder phrase = new StringBuilder();
                    for(int j = start_pos; j < textPositions.size(); j++) {
                        //System.out.println(textPositions.get(j).getUnicode());
                        phrase.append(textPositions.get(j).getUnicode());
                        if (DEBUG && string.contains("power ")) {
                            System.out.println(phrase.toString());
                        }
                        if (phrase.toString().equals(highlight_text)) {
                            end_pos = j;
                            if (DEBUG) {
                                System.out.println("End position: " + j);
                            }
                            end_pos_found = true;
                            break;
                        }
                    }
                } // end position

                
                if (start_pos_found && end_pos_found) {
                
                    posXInit = textPositions.get(start_pos).getXDirAdj();
                    posXEnd  = textPositions.get(end_pos).getXDirAdj() + textPositions.get(end_pos).getWidth();
                    posYInit = textPositions.get(start_pos).getPageHeight() - textPositions.get(start_pos).getYDirAdj();
                    posYEnd  = textPositions.get(start_pos).getPageHeight() - textPositions.get(end_pos).getYDirAdj();
                    width    = textPositions.get(start_pos).getWidthDirAdj();
                    height   = textPositions.get(start_pos).getHeightDir();

                    if (DEBUG) {
                        System.out.println(string + " X-Init = " + posXInit + "; Y-Init = " + posYInit + "; X-End = " + posXEnd + "; Y-End = " + posYEnd + "; Font-Height = " + fontHeight);
                    }

                    float quadPoints[] = {posXInit, posYEnd + height + 2, posXEnd, posYEnd + height + 2, posXInit, posYInit - 2, posXEnd, posYEnd - 2};

                    annotationArea.setQuadPoints(quadPoints);
                    
                    PDRectangle position = new PDRectangle();
                    position.setLowerLeftX(posXInit);
                    position.setLowerLeftY(posYEnd);
                    position.setUpperRightX(posXEnd);
                    position.setUpperRightY(posYEnd + height + 15);
                
                    System.out.println("Position highlight:");
                    System.out.println("LowerLeftX:" + position.getLowerLeftX());
                    System.out.println("LowerLeftY:" + position.getLowerLeftY());
                    System.out.println("UpperRightX:" + position.getUpperRightX());
                    System.out.println("UpperRightLowerLeftY:" + position.getUpperRightY());
                    
                    
                    System.out.println("Quad points highlight:");
                    System.out.println(quadPoints[0]);
                    System.out.println(quadPoints[1]);
                    System.out.println(quadPoints[2]);
                    System.out.println(quadPoints[3]);
                    System.out.println(quadPoints[4]);
                    System.out.println(quadPoints[5]);
                    System.out.println(quadPoints[6]);
                    System.out.println(quadPoints[7]);
                    
                    annotationArea.setPosition(position);
                    
                    /*
                    List<PDAnnotation> annotations = document.getPage(this.getCurrentPageNo() - 1).getAnnotations();
                    
                    PDAnnotationTextMarkup highlight = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);

                    highlight.setRectangle(position);

                    // quadPoints is array of x,y coordinates in Z-like order (top-left, top-right, bottom-left,bottom-right) 
                    // of the area to be highlighted
                    highlight.setQuadPoints(quadPoints);
                    highlight.setColor(orange);
                    highlight.setConstantOpacity(0.3f); // 30% transparent
                    highlight.setSubject("Highlight");
                
                    highlight.setTitlePopup(reviewer);
                    
                    if (date != null) {
                        highlight.setModifiedDate(date);
                    }
                    */
                    
                    
                    //PDAnnotationPopup popup = new PDAnnotationPopup();
                    //highlight.setPopup(popup);
                    //highlight.setAnnotationName("My Annotation Name");                    
                    /*
                    //highlight.setIntent("My intent");
                    */
                    
                    
                    
                    //annotations.add(highlight);
                    
                    highlightedAlready = true;
                    
                    if (DEBUG) {
                        System.out.println("The string '" + highlight_text + "' highlighted!");
                    }
                } else {
                    if (DEBUG) {
                        System.out.println("The string '" + highlight_text + "' can't be found and highlighted!");
                    }
                }
                
            }

            //processedAlready = postItAlready && highlightedAlready;
            processedAlready = postItAlready || highlightedAlready;
        }
    }
   
}