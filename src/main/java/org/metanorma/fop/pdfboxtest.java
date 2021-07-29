package org.metanorma.fop;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDGamma;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDSoftMask;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;


/**
 *
 * @author Alexander Dyuzhev
 */
public class pdfboxtest {
    public static void main(String[] args) {
        String filepath = "D:\\Metanorma\\XML\\BIPM44\\Result\\si-brochure-en.presentation.pdf";
        
        Path pdf = Paths.get(filepath);
        
        PDDocument doc;
        try {
            doc = PDDocument.load(pdf.toFile());
            
            PDGamma colourBlue = new PDGamma();
            colourBlue.setB(1);
            
            for (PDPage page : doc.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                float pw = page.getMediaBox().getUpperRightX();
                float ph = page.getMediaBox().getUpperRightY();
            
                PDResources resources = page.getResources();

                /*ImageGraphicsEngine extractor = new ImageGraphicsEngine(page);
                extractor.run();*/
                
                
                for (COSName xObjectName : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(xObjectName);
                    if (xObject instanceof PDFormXObject) {
                        
                    }
                    if (xObject instanceof PDImageXObject) {
                        RenderedImage image = ((PDImageXObject) xObject).getImage();
                        int x = image.getMinX();
                        int y = image.getMinY();
                        int width = image.getWidth();
                        int height = image.getHeight();
                        
                        PDRectangle position = new PDRectangle();
                        position.setLowerLeftX(x);
                        position.setLowerLeftY(y);
                        
                        position.setUpperRightX(x + width);
                        position.setUpperRightY(y + height);
                        
                        PDAnnotationText text = new PDAnnotationText();
                        text.setContents("Mathml text");
                        text.setRectangle(position);
                        text.setOpen(true);
                        text.setConstantOpacity(50f);
                        annotations.add(text);
                    }
                }
                
                if (!annotations.isEmpty()) {
                    page.setAnnotations(annotations);
                }
                
            }
            
            //PDPage page1 = doc.getPage(0);
            
            
            doc.save("D:\\Metanorma\\XML\\BIPM44\\Result\\si-brochure-en.presentation.pdfbox.pdf");
            
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

        
            
    }
}
