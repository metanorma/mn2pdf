package org.metanorma.fop.annotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import org.metanorma.utils.LoggerHelper;


/**
 *
 * @author Alexander Dyuzhev
 */
public class PDFTextAnnotationStripper extends PDFTextStripper {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private boolean DEBUG = false;
    
    private String highlight_text = "";
    private boolean doHighlight = false;
    private boolean doPostIt = false;
    
    private float x = 0f;
    private float y = 0f;
    
    AnnotationArea annotationArea = null;
    
    private boolean processedAlready = false;
    private boolean postItAlready = false;
    private boolean highlightedAlready = false;


    public PDFTextAnnotationStripper( String highlight_text, 
            boolean doPostIt, boolean doHighlight, 
            float x, float y,
            AnnotationArea annotationArea)  throws IOException {
        super();
        
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

        string = string.replace('\u00A0',' ');

        if (DEBUG) {
            System.out.println("Current string: '" + string + "'");
        }

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

            if (doPostIt && !postItAlready) {
                // add post-it annotation (popup window)

                posXInit = x;
                posXEnd  = x + 20;
                float y_shift = 5; // to move a bit higher, otherwise post-it note will overlap highlighted text
                float height_note = 25;
                posYInit = textPositions.get(0).getPageHeight() - y + y_shift;
                posYEnd  = textPositions.get(0).getPageHeight() - y + y_shift + height_note;
                
                float[] position = {posXInit, posYInit, posXEnd, posYEnd};
                /*PDRectangle position = new PDRectangle();
                position.setLowerLeftX(posXInit);
                position.setLowerLeftY(posYInit);
                position.setUpperRightX(posXEnd);
                position.setUpperRightY(posYEnd);*/
                
                if (DEBUG) {
                    System.out.println("Position popup: " + Arrays.toString(position));
                }
                
                annotationArea.setPosition(position);
                
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
                        if (phrase.toString().replace('\u00A0',' ').equals(highlight_text)) {
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
                    
                    float[] position = {posXInit, posYEnd, posXEnd, posYEnd + height + 15};
                    
                    annotationArea.setPosition(position);
                    
                    if (DEBUG) {
                        System.out.println("Position highlight: " + Arrays.toString(position));
                        System.out.println("Quad points highlight: " + Arrays.toString(quadPoints));
                    }
                    
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
            processedAlready = (doPostIt && postItAlready) || (doHighlight && highlightedAlready);
        }
    }
   
}