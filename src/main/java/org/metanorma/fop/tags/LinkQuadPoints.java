package org.metanorma.fop.tags;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.metanorma.utils.LoggerHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class LinkQuadPoints {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private PDStructureTreeRoot structureTreeRoot;

    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_link_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                mergeLinks(kid);
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf); // , CompressParameters.NO_COMPRESSION
        } catch (IOException ex) {
            logger.severe("Can't process Link.");
            ex.printStackTrace();
        }
        finally {
            Files.deleteIfExists(pdf_tmp);
        }
    }

    private void mergeLinks(Object element) {
        if (element instanceof PDStructureNode) {
            List<Object> kids = ((PDStructureNode) element).getKids();
            for (int i = 0; i < kids.size(); i++) {
                Object kid = kids.get(i);

                if (kid instanceof PDStructureElement) {
                    PDStructureElement pdStructureElement = (PDStructureElement) kid;

                    if (pdStructureElement.getStructureType().equals("Link")) {
                        List<PDObjectReference> PDObjectReferences = new ArrayList<>();
                        List<PDAnnotationLink> PDAnnotationLinks = new ArrayList<>();
                        for (Object o: pdStructureElement.getKids()) {
                            if (o instanceof PDObjectReference) {
                                PDObjectReference objRef = (PDObjectReference) o;
                                COSObjectable refObj =  objRef.getReferencedObject();
                                if (refObj instanceof PDAnnotationLink) {
                                    PDAnnotationLink link = (PDAnnotationLink) refObj;
                                    PDAnnotationLinks.add(link);
                                    PDObjectReferences.add(objRef);
                                }
                            }
                        }

                        // if more than 1 Link, them merge and convert /Rect to /QuadPoints
                        if (PDAnnotationLinks.size() > 1) {
                            int countQuadPoints = 8 * PDAnnotationLinks.size();
                            float[] quadPoints = new float[countQuadPoints];
                            // gathering the /Rect coordinates
                            for(int j = 0; j < PDAnnotationLinks.size(); j++) {
                                PDAnnotationLink pdLink = PDAnnotationLinks.get(j);
                                PDRectangle pdRectangle = pdLink.getRectangle();
                                //*          ***************  (x2,y2)
                                //*          *             *
                                //*          *             *
                                //*          *             *
                                //*  (x1,y1) ***************
                                float x1 = pdRectangle.getLowerLeftX();
                                float y1 = pdRectangle.getLowerLeftY();
                                float x2 = pdRectangle.getUpperRightX();
                                float y2 = pdRectangle.getUpperRightY();

                                // /QuadPoints:
                                //*   6  7                      4  5
                                //*  (x4,y4) ***************  (x3,y3)
                                //*          *             *
                                //*          *             *
                                //*   0  1   *             *    2  3
                                //*  (x1,y1) ***************  (x2,y2)
                                quadPoints[j * 8 + 0] = x1; // x1
                                quadPoints[j * 8 + 1] = y1; // y1
                                quadPoints[j * 8 + 2] = x2; // x2
                                quadPoints[j * 8 + 3] = y1; // y2
                                quadPoints[j * 8 + 4] = x2; // x3
                                quadPoints[j * 8 + 5] = y2; // y3
                                quadPoints[j * 8 + 6] = x1; // x4
                                quadPoints[j * 8 + 7] = y2; // y4

                            }
                            PDAnnotationLink firstPDLink = PDAnnotationLinks.get(0);
                            firstPDLink.setQuadPoints(quadPoints);
                            firstPDLink.getCOSObject().setItem(COSName.RECT, null); // because firstPDLink.setRectangle(null) raises exception

                            //remove 2nd, 3rd... Link
                            for(int j = 1; j < PDAnnotationLinks.size(); j++) {
                                pdStructureElement.removeKid(PDObjectReferences.get(j));
                            }
                        }
                    }
                    else {
                        mergeLinks(kid);
                    }
                }
            }
        }
    }
}
