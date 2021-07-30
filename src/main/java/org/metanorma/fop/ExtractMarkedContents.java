package org.metanorma.fop;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;

/**
 * This is an example on how to get the x/y coordinates of image locations.
 *
 * @author Ben Litchfield
 */
public class ExtractMarkedContents {
    
    public static void main( String[] args ) throws IOException
    {
        String filepath = "D:\\Metanorma\\mn2pdf\\test.pdf";
        
        PDDocument document = null;
        try
        {
            document = PDDocument.load( new File(filepath) );
            ExtractMarkedContents printer = new ExtractMarkedContents();
            int pageNum = 0;
            
            Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = new HashMap<>();
            
            for( PDPage page : document.getPages() )
            {
                PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
                extractor.processPage(page);
                
                Map<Integer, PDMarkedContent> theseMarkedContents = new HashMap<>();
                markedContents.put(page, theseMarkedContents);
                for (PDMarkedContent markedContent : extractor.getMarkedContents()) {
                    String sAlt = markedContent.getAlternateDescription();
                    String sActual = markedContent.getActualText();
                    theseMarkedContents.put(markedContent.getMCID(), markedContent);
                }
                
                PDStructureNode root = document.getDocumentCatalog().getStructureTreeRoot();
                 showStructure(root, markedContents);
                
                pageNum++;
                System.out.println( "Processing page: " + pageNum );
                
            }
        }
        finally
        {
            if( document != null )
            {
                document.close();
            }
        }
        
    }

    static void showStructure(PDStructureNode node, Map<PDPage, Map<Integer, PDMarkedContent>> markedContents) {
        String structType = null;
        PDPage page = null;
        if (node instanceof PDStructureElement) {
            PDStructureElement element = (PDStructureElement) node;
            structType = element.getStructureType();
            page = element.getPage();
        }
        Map<Integer, PDMarkedContent> theseMarkedContents = markedContents.get(page);
        System.out.printf("<%s>\n", structType);
        
        if (structType != null && structType.equals("Figure")) {
            COSDictionary dict = node.getCOSObject();
            for(Entry<COSName, COSBase> e:dict.entrySet()) {
                System.out.println("Key=" + e.getKey());
                System.out.println("Value=" + e.getValue());
            }
        }
        for (Object object : node.getKids()) {
            if (object instanceof COSArray) {
                for (COSBase base : (COSArray) object) {
                    if (base instanceof COSDictionary) {
                        showStructure(PDStructureNode.create((COSDictionary) base), markedContents);
                    } else if (base instanceof COSNumber) {
                        showContent(((COSNumber)base).intValue(), theseMarkedContents);
                    } else {
                        System.out.printf("?%s\n", base);
                    }
                }
            } else if (object instanceof PDStructureNode) {
                showStructure((PDStructureNode) object, markedContents);
            } else if (object instanceof Integer) {
                showContent((Integer)object, theseMarkedContents);
            } else {
                System.out.printf("?%s\n", object);
            }

        }
        System.out.printf("</%s>\n", structType);
    }

    /**
     * @see #showStructure(PDStructureNode, Map)
     * @see #testExtractTestWPhromma()
     */
    static void showContent(int mcid, Map<Integer, PDMarkedContent> theseMarkedContents) {
        PDMarkedContent markedContent = theseMarkedContents != null ? theseMarkedContents.get(mcid) : null;
        List<Object> contents = markedContent != null ? markedContent.getContents() : Collections.emptyList();
        StringBuilder textContent =  new StringBuilder();
        for (Object object : contents) {
            if (object instanceof TextPosition) {
                textContent.append(((TextPosition)object).getUnicode());
            } else {
                textContent.append("?" + object);
            }
        }
        System.out.printf("%s\n", textContent);
    }
}
