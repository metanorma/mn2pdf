package org.metanorma.fop.annotations;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.fdf.FDFAnnotation;
import org.apache.pdfbox.pdmodel.fdf.FDFDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.metanorma.fop.Util;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
                
                // iteration for each popup (Post-It) annotations
                //NodeList nodes_annotations = sourceXML.getElementsByTagName("text");
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodes_annotations = (NodeList) xPath.compile("//*[local-name() = 'text' or local-name() = 'highlight']").evaluate(sourceXML, XPathConstants.NODESET);
                
                for (int i = 0; i < nodes_annotations.getLength(); i++) {
                    Node node_annotation = nodes_annotations.item(i);
                    
                    String element_name = node_annotation.getNodeName();
                    
                    // format 'date'
                    try {
                        Node att_date = node_annotation.getAttributes().getNamedItem("date");
                        String date = att_date.getTextContent();
                        att_date.setNodeValue(Util.getXFDFDate(date));
                    } catch (Exception ex) {}
                    
                    int page = Integer.parseInt(node_annotation.getAttributes().getNamedItem("page").getTextContent());
                    
                    Node att_rect = node_annotation.getAttributes().getNamedItem("rect");
                    
                    String rect = att_rect.getTextContent();
                    String[] rect_components = rect.split(",");
                    
                    
                    // evaluate start position for find
                    float x = 100;
                    try {
                        x = Float.parseFloat(rect_components[0]);
                    } catch (Exception ex) {}

                    float y = 100;
                    try {
                        y = Float.parseFloat(rect_components[1]);
                    } catch (Exception ex) {}
                    
                    boolean doPostIt = false;
                    doPostIt = element_name.equals("text");
                    boolean doHighlight = false;
                    doHighlight = element_name.equals("highlight");
                    
                    
                    if (DEBUG) {
                        System.out.println("page=" + page);
                        System.out.println("x=" + x);
                        System.out.println("y=" + y);
                    }
                    
                    AnnotationArea annotationArea = new AnnotationArea();
                    
                    String highlight_text = "";
                    
                    if (doHighlight) {
                        try {
                            Node node_hightlighttext = ((Element)node_annotation).getElementsByTagName("hightlighttext").item(0);  
                            highlight_text = node_hightlighttext.getTextContent();
                        } catch (Exception ex) {}
                    }
                    
                    PDFTextStripper stripper = new PDFTextAnnotationStripper(highlight_text, 
                            doPostIt, doHighlight, 
                            x, y,
                            annotationArea);
                    stripper.setSortByPosition(true);

                    stripper.setStartPage(page + 1);
                    stripper.setEndPage(page + 1);

                    Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
                    stripper.writeText(document, dummy);

                    String str_rect = Arrays.toString(annotationArea.getPosition()).replace("[","").replace("]","").replace(" ","");
                    
                    float y_lower = annotationArea.getPosition()[1];
                    float y_upper = annotationArea.getPosition()[3];
                    
                    att_rect.setTextContent(str_rect);
                    
                    if (doHighlight) {
                        Node att_coords = node_annotation.getAttributes().getNamedItem("coords");
                        att_coords.setTextContent(Arrays.toString(annotationArea.getQuadPoints()).replace("[", "").replace("]", "").replace(" ", ""));
                    }
                    
                    if (DEBUG) {
                        System.out.println("postItPopup position=" + Arrays.toString(annotationArea.getPosition()));
                    }
                    
                    Node node_popup = ((Element)node_annotation).getElementsByTagName("popup").item(0);
                    
                    Node att_popup_rect = node_popup.getAttributes().getNamedItem("rect");
                    
                    float[] popup_rect = {595,y_lower - 100,790,y_upper};
                    String str_popup_rect = Arrays.toString(popup_rect).replace("[","").replace("]","").replace(" ","");
                    
                    att_popup_rect.setTextContent(str_popup_rect);
                }
                
                
                // updated XML Review
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(sourceXML), new StreamResult(writer));
                String updatedXMLReview = writer.getBuffer().toString();
                
              
                if (DEBUG) {   //DEBUG: write updated review xml file
                    try ( 
                        BufferedWriter xmlwriter = Files.newBufferedWriter(Paths.get(pdf.getAbsolutePath() + ".if.xfdf.updated.xml"))) {
                        xmlwriter.write(updatedXMLReview);                    
                    }
                }
                                
                
                // import XFDF annotation xml
                
                FDFDocument fdfDoc = FDFDocument.loadXFDF(new ByteArrayInputStream(updatedXMLReview.getBytes(StandardCharsets.UTF_8)));
                List<FDFAnnotation> fdfAnnots = fdfDoc.getCatalog().getFDF().getAnnotations();
                
                // group annotations relate to one page and add them into page
                HashMap<Integer,List<PDAnnotation>> map_pdfannots = new HashMap<>();
                
                for (int i=0; i<fdfDoc.getCatalog().getFDF().getAnnotations().size(); i++) {
                    FDFAnnotation fdfannot = fdfAnnots.get(i);
                    int page = fdfannot.getPage();
                    
                    PDAnnotation pdfannot = PDAnnotation.createAnnotation(fdfannot.getCOSObject());

                    if (map_pdfannots.get(page) == null) {
                        map_pdfannots.put(page, new ArrayList<PDAnnotation>());
                    }
                    map_pdfannots.get(page).add(pdfannot);
                }
                
                for (Map.Entry<Integer,List<PDAnnotation>> set: map_pdfannots.entrySet()) {
                    document.getPage(set.getKey()).setAnnotations(set.getValue());
                }
                
                fdfDoc.close();
                
                document.save(pdf);
                
            } catch (IOException | NumberFormatException | ParserConfigurationException | DOMException | TransformerException | SAXException | XPathException ex) {
                logger.severe("Can't read annotation data from xml.");
                ex.printStackTrace();
            } 
            
            
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
