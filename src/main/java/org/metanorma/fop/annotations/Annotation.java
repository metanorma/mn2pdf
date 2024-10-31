package org.metanorma.fop.annotations;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import org.apache.fop.pdf.PDFObject;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDParentTreeValue;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
import org.xml.sax.SAXException;

/**
 *
 * @author Alexander Dyuzhev
 */
public class Annotation {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private boolean DEBUG = false;

    private HashMap<String,PDAnnotation> hashMapDocumentAnnotations = new HashMap<>();

    private PDStructureTreeRoot structureTreeRoot;

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
                        /*System.out.println("page=" + page);
                        System.out.println("x=" + x);
                        System.out.println("y=" + y);*/
                    }
                    
                    AnnotationArea annotationArea = new AnnotationArea();
                    
                    String highlight_text = "";
                    
                    if (doHighlight) {
                        try {
                            Node node_hightlighttext = ((Element)node_annotation).getElementsByTagName("hightlighttext").item(0);  
                            highlight_text = node_hightlighttext.getTextContent();
                            // delete node
                            node_hightlighttext.getParentNode().removeChild(node_hightlighttext);
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

                    if (annotationArea.getPosition().length == 0) {
                        logger.severe("Can't highlight the text '" + highlight_text + "'.");
                    } else {

                        float y_lower = annotationArea.getPosition()[1];
                        float y_upper = annotationArea.getPosition()[3];

                        att_rect.setTextContent(Util.floatArrayToString(annotationArea.getPosition()));

                        if (doHighlight) {
                            Node att_coords = node_annotation.getAttributes().getNamedItem("coords");

                            StringBuilder sb_quadPoints = new StringBuilder();
                            sb_quadPoints.append(Util.floatArrayToString(annotationArea.getQuadPoints()));

                            // =====================================
                            // union next highlights with current
                            // =====================================
                            List<Node> nodes_delete = new ArrayList<>();
                            NodeList nodes_next_highlight = ((Element)node_annotation).getElementsByTagName("next_highlight");
                            for (int j = 0; j < nodes_next_highlight.getLength(); j++) {
                                Node node_next_highlight = nodes_next_highlight.item(j);

                                Node att_next_rect = node_next_highlight.getAttributes().getNamedItem("rect");

                                String next_rect = att_next_rect.getTextContent();
                                String[] next_rect_components = next_rect.split(",");

                                // evaluate start position for find
                                float x_next = 100;
                                try {
                                    x_next = Float.parseFloat(next_rect_components[0]);
                                } catch (Exception ex) {}

                                float y_next = 100;
                                try {
                                    y_next = Float.parseFloat(next_rect_components[1]);
                                } catch (Exception ex) {}

                                AnnotationArea annotationNextArea = new AnnotationArea();

                                String highlight_next_text = "";

                                try {
                                    Node node_hightlighnextttext = ((Element)node_next_highlight).getElementsByTagName("hightlighttext").item(0);
                                    highlight_next_text = node_hightlighnextttext.getTextContent();
                                } catch (Exception ex) {}

                                PDFTextStripper stripper_next = new PDFTextAnnotationStripper(highlight_next_text,
                                        doPostIt, doHighlight,
                                        x_next, y_next,
                                        annotationNextArea);
                                stripper_next.setSortByPosition(true);

                                stripper_next.setStartPage(page + 1);
                                stripper_next.setEndPage(page + 1);

                                Writer dummy_next = new OutputStreamWriter(new ByteArrayOutputStream());
                                stripper_next.writeText(document, dummy_next);

                                String quadPoints = Util.floatArrayToString(annotationNextArea.getQuadPoints());
                                if (!quadPoints.isEmpty()) {
                                    sb_quadPoints.append(",");
                                    sb_quadPoints.append(quadPoints);
                                }

                                //node_next_highlight.getParentNode().removeChild(node_next_highlight);
                                nodes_delete.add(node_next_highlight);
                            }
                            for (int j=0; j<nodes_delete.size();j++) {
                                Node node = nodes_delete.get(j);
                                node.getParentNode().removeChild(node);
                            }
                            // =====================================
                            // End: union next highlights with current
                            // =====================================

                            att_coords.setTextContent(sb_quadPoints.toString());
                        }

                        if (DEBUG) {
                            //System.out.println("postItPopup position=" + Arrays.toString(annotationArea.getPosition()));
                        }

                        Node node_popup = ((Element)node_annotation).getElementsByTagName("popup").item(0);

                        Node att_popup_rect = node_popup.getAttributes().getNamedItem("rect");

                        float[] popup_rect = {595,y_lower - 100,790,y_upper};

                        att_popup_rect.setTextContent(Util.floatArrayToString(popup_rect));
                    }
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
                HashMap<Integer,List<PDAnnotation>> mapPDFannots = new HashMap<>();
                for (int i=0; i<fdfDoc.getCatalog().getFDF().getAnnotations().size(); i++) {
                    FDFAnnotation fdfannot = fdfAnnots.get(i);
                    int page = fdfannot.getPage();
                    
                    PDAnnotation pdfannot = PDAnnotation.createAnnotation(fdfannot.getCOSObject());

                    pdfannot.constructAppearances(); // requires for PDF/A
                    if (mapPDFannots.get(page) == null) {
                        mapPDFannots.put(page, new ArrayList<PDAnnotation>());
                    }
                    mapPDFannots.get(page).add(pdfannot);
                }

                for (Map.Entry<Integer,List<PDAnnotation>> set: mapPDFannots.entrySet()) {
                    PDPage page = document.getPage(set.getKey());
                    List<PDAnnotation> pageAnotations = page.getAnnotations();
                    // merge existing annotations (including hyperlinks) and new annotations
                    pageAnotations.addAll(set.getValue());
                    document.getPage(set.getKey()).setAnnotations(pageAnotations);
                }
                
                fdfDoc.close();

                document.save(pdf);

            } catch (IOException | NumberFormatException | ParserConfigurationException | DOMException | TransformerException | SAXException | XPathException ex) {
                logger.severe("Can't read annotation data from xml.");
                ex.printStackTrace();
            } 

            try {
                document = PDDocument.load(pdf); // important
                hashMapDocumentAnnotations = getAnnotationIDmap(document);

                structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();
                COSArray aDocument = (COSArray) structureTreeRoot.getK();
                fixAnnotationTags(aDocument, null, 0);

                clearEmptyAnnotations(document);

                document.save(pdf);
            } catch (IOException ex) {
                logger.severe("Can't enclose the annotation into the Annot tag.");
                ex.printStackTrace();
            }

        } finally {
            if( document != null ) {
                document.close();
            }
        }
        
    }

    private HashMap<String,PDAnnotation> getAnnotationIDmap(PDDocument document) throws IOException {
        HashMap<String,PDAnnotation> hashMapDocumentAnnotations = new HashMap<>();
        for(int i = 0; i< document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            for (PDAnnotation pdAnnotation: page.getAnnotations()){
                COSDictionary pdAnnotationDict = pdAnnotation.getCOSObject();
                if (pdAnnotationDict != null) {
                    // subject contains id 'Annot___', see xfdf_simple.xsl, attribute 'subject'
                    String subj = pdAnnotationDict.getString(COSName.SUBJ);
                    if (subj != null) {
                        hashMapDocumentAnnotations.put(subj, pdAnnotation);
                    }
                }
            }
        }
        return hashMapDocumentAnnotations;
    }

    private void fixAnnotationTags(COSArray oArray, COSObject parentObject, int level) throws IOException {

        if (oArray != null) {
            for(int i = 0; i < oArray.size(); i++) {
                COSObject oArrayItem = (COSObject) oArray.get(i);

                COSName cName = (COSName) oArrayItem.getItem(COSName.S);
                if (cName != null) {
                    String tagName = cName.getName();

                    String levelPrefix = Collections.nCopies(level, " ").toString()
                            .replace("[", "")
                            .replace("]", "")
                            .replace(", ", "");
                    if (DEBUG) {
                        System.out.println(levelPrefix + tagName);
                    }

                    if (tagName.equals("Annot")) {
                        COSBase cbAlt = oArrayItem.getItem(COSName.ALT);
                        if (cbAlt != null) {
                            String tagAlt = ((COSString)cbAlt).toString();
                            String ANNOT_PREFIX = "COSString{";
                            if (tagAlt.startsWith(ANNOT_PREFIX + "Annot___")) {
                                // here replace exising tag Annot with new tag Annot

                                String annotationId = tagAlt.substring(ANNOT_PREFIX.length(), tagAlt.length() - 1);

                                if (DEBUG) {
                                    System.out.println(levelPrefix + "id=" + tagAlt);
                                }

                                // add the annotation element
                                COSDictionary anDict = new COSDictionary();
                                // set Tag name (S)
                                anDict.setItem(COSName.S, COSName.ANNOT);
                                // set Parent (P)
                                anDict.setItem(COSName.P, parentObject); //oArrayItem oArray
                                // set Page (PG)
                                COSArray oArrayK = (COSArray) oArrayItem.getItem(COSName.K);
                                anDict.setItem(COSName.PG, ((COSObject)oArrayK.get(0)).getItem(COSName.PG));

                                PDObjectReference objRef = new PDObjectReference();
                                anDict.setItem(COSName.K, objRef);

                                PDAnnotation foundAnnotation = hashMapDocumentAnnotations.get(annotationId);
                                objRef.setReferencedObject(foundAnnotation);

                                if (DEBUG) {
                                    System.out.println(oArrayItem.getItem(COSName.K));
                                }

                                try {
                                    oArrayItem.setObject(anDict);

                                    // from https://stackoverflow.com/questions/79083813/how-to-add-the-annotation-tag-in-tagged-pdf-using-pdfbox

                                    int parentTreeNextKey = structureTreeRoot.getParentTreeNextKey(); // -1, ignored here

                                    // assign a number to the annotation and insert the annotation element into the parent tree, and set ParentTreeNextKey
                                    PDNumberTreeNode parentTree = structureTreeRoot.getParentTree();
                                    Map<Integer, COSObjectable> numberTreeAsMap = getNumberTreeAsMap(parentTree);
                                    Set<Integer> keySet = numberTreeAsMap.keySet();

                                    if (parentTreeNextKey == -1) {
                                        parentTreeNextKey = keySet.stream().reduce(Integer::max).get() + 1;
                                    }

                                    foundAnnotation.setStructParent(parentTreeNextKey);
                                    structureTreeRoot.setParentTreeNextKey(parentTreeNextKey + 1);
                                    numberTreeAsMap.put(parentTreeNextKey, anDict);
                                    parentTree = new PDNumberTreeNode(PDParentTreeValue.class);
                                    parentTree.setNumbers(numberTreeAsMap);
                                    structureTreeRoot.setParentTree(parentTree);
                                    // END from stackoverflow
                                } catch (IOException e) {
                                    System.out.println(e.toString());
                                }
                            }
                        }
                    }
                }
                try {
                    COSArray oA_K = (COSArray) oArrayItem.getItem(COSName.K);
                    fixAnnotationTags(oA_K, oArrayItem, ++level);
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    private void clearEmptyAnnotations(PDDocument document) throws IOException {
        for (int i = 0; i < document.getNumberOfPages(); i++)
        {
            List<PDAnnotation> pageAnnotations = new ArrayList<>();
            PDPage page = document.getPage(i);
            for(PDAnnotation pageAnnotation: page.getAnnotations()) {
                if(!(pageAnnotation.getContents().startsWith("Annot___"))) {
                    pageAnnotations.add(pageAnnotation);
                }
            }
            document.getPage(i).setAnnotations(pageAnnotations);
        }
    }

    private Map<Integer, COSObjectable> getNumberTreeAsMap(PDNumberTreeNode tree) throws IOException {
        if (tree == null)
        {
            return new LinkedHashMap<>();
        }
        Map<Integer, COSObjectable> numbers = tree.getNumbers();
        if (numbers == null)
        {
            numbers = new LinkedHashMap<>();
        }
        else
        {
            // must copy because the map is read only
            numbers = new LinkedHashMap<>(numbers);
        }
        List<PDNumberTreeNode> kids = tree.getKids();
        if (kids != null)
        {
            for (PDNumberTreeNode kid : kids)
            {
                numbers.putAll(getNumberTreeAsMap(kid));
            }
        }
        return numbers;
    }

}
