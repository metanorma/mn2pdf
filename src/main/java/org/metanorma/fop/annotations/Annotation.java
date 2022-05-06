package org.metanorma.fop.annotations;

import org.metanorma.fop.annotations.PDFTextAnnotationStripper;
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
import org.metanorma.fop.Util;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 *
 * @author Alexander Dyuzhev
 */
public class Annotation {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private boolean DEBUG = false;
    
    public void process(File pdf, String xmlReview) throws IOException {
        PDDocument document = null;
        
        try {
            document = PDDocument.load(pdf);
            
            
            // iterate for each 'annotation' in xmlReview
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(xmlReview));
                
                Document sourceXML = dBuilder.parse(is);
                //XPath xPath = XPathFactory.newInstance().newXPath();

                NodeList nodes_annotations = sourceXML.getElementsByTagName("annotation");
                for (int i = 0; i < nodes_annotations.getLength(); i++) {
                    Node node_annotation = nodes_annotations.item(i);
                    
                    String reviewer = "";
                    try {
                        reviewer = node_annotation.getAttributes().getNamedItem("reviewer").getTextContent();
                    } catch (Exception ex) { }
                    
                    String date = "";
                    Calendar cal = null;
                    try {
                        date = node_annotation.getAttributes().getNamedItem("date").getTextContent();
                        cal = Util.getCalendarDate(date);
                    } catch (Exception ex) {}
                    
                    
                    NodeList annotations_data = ((Element)node_annotation).getElementsByTagName("data");
                    Node annotation_data = annotations_data.item(0);
                    String annotation_text = innerXml(annotation_data); //.getTextContent();
                    
                    if (DEBUG) {
                        System.out.println("Author=" + reviewer);
                        System.out.println("Date=" + date);
                        System.out.println("Annotation=" + annotation_text);
                    }
                    
                    NodeList nodes_texts = ((Element)node_annotation).getElementsByTagName("text");
                    
                    for (int j = 0; j < nodes_texts.getLength(); j++) {
                        Node node_text = nodes_texts.item(j);
                        NamedNodeMap node_text_att = node_text.getAttributes();
                        int page = Integer.parseInt(node_text_att.getNamedItem("page").getTextContent());
                        
                        float x = 100;
                        try {
                            x = Float.parseFloat(node_text_att.getNamedItem("x").getTextContent());
                        } catch (Exception ex) {}
                        
                        float y = 100;
                        try {
                            y = Float.parseFloat(node_text_att.getNamedItem("y").getTextContent());
                        } catch (Exception ex) {}
                        
                        String highlight_text = node_text.getTextContent();
                        
                        boolean doHighlight = false;
                        try {
                            doHighlight = Boolean.parseBoolean(node_text_att.getNamedItem("highlight").getTextContent());
                        } catch (Exception ex) {}
                        
                        boolean doPostIt = true;
                        try {
                            doPostIt = Boolean.parseBoolean(node_text_att.getNamedItem("postit").getTextContent());
                        } catch (Exception ex) {}
                        
                        if (DEBUG) {
                            System.out.println("page=" + page);
                            System.out.println("x=" + x);
                            System.out.println("y=" + y);
                            System.out.println("text=" + highlight_text);
                        }
                        
                        HighlightArea highlightArea = new HighlightArea();
                        
                        HighlightArea postItPopup  = new HighlightArea();
                        
                        PDFTextStripper stripper = new PDFTextAnnotationStripper(reviewer, cal, annotation_text, highlight_text, 
                                doPostIt, doHighlight, 
                                x, y,
                                highlightArea,
                                postItPopup);
                        stripper.setSortByPosition(true);
                        
                        
                        
                        
                        //stripper.setStartPage( 0 );
                        stripper.setStartPage(page);
                        //stripper.setEndPage( document.getNumberOfPages() );
                        stripper.setEndPage(page);
                        
                        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                        stripper.writeText(document, dummy);
                        
                        System.out.println("postItPopup position=" + postItPopup.getPosition().getLowerLeftX());
                        System.out.println("highlightArea quadPositions=" + highlightArea.getQuadPoints());
                        System.out.println("highlightArea position=" + highlightArea.getPosition().getLowerLeftX());
                        
                        document.save(pdf);
                        
                        
                    }
                    
                }
                
                //XPathExpression query = xPath.compile("//annotation");
                
            } catch (IOException | NumberFormatException | ParserConfigurationException | DOMException | SAXException ex) {
                logger.severe("Can't read annotation data from xml.");
                ex.printStackTrace();
            } 
            
            
            
            

            document.save(pdf);
            
        } finally {
            if( document != null ) {
                document.close();
            }
        }
        
    }
    
    private String innerXml(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
           sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        return sb.toString(); 
    }
    
}
