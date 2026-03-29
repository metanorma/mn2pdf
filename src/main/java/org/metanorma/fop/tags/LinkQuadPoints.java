package org.metanorma.fop.tags;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.handlers.PDLinkAppearanceHandler;
import org.metanorma.utils.LoggerHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class LinkQuadPoints {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private PDStructureTreeRoot structureTreeRoot;

    private final String ANNOTATION_TO_REMOVE = "_METANORMA_REMOVE";

    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_link_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                mergeLinks(kid);
            }

            int numberOfPages = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                List<PDAnnotation> annotations = page.getAnnotations();

                Iterator<PDAnnotation> iterator = annotations.iterator();
                boolean isRemoved = false;
                while (iterator.hasNext()) {
                    String contents = iterator.next().getContents();
                    if (contents.equals(ANNOTATION_TO_REMOVE)) {
                        iterator.remove();
                        isRemoved = true;
                    }
                }
                if (isRemoved) {
                    page.setAnnotations(annotations);
                }
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
                        List<PDObjectReference> pdObjectReferences = new ArrayList<>();
                        List<PDAnnotationLink> pdAnnotationLinks = new ArrayList<>();
                        for (Object o: pdStructureElement.getKids()) {
                            if (o instanceof PDObjectReference) {
                                PDObjectReference objRef = (PDObjectReference) o;
                                COSObjectable refObj =  objRef.getReferencedObject();
                                if (refObj instanceof PDAnnotationLink) {
                                    PDAnnotationLink link = (PDAnnotationLink) refObj;
                                    pdAnnotationLinks.add(link);
                                    pdObjectReferences.add(objRef);
                                }
                            }
                        }

                        // if more than 1 Link, them merge and convert /Rect to /QuadPoints
                        if (pdAnnotationLinks.size() > 1) {
                            int countQuadPoints = 8 * pdAnnotationLinks.size();
                            float[] quadPoints = new float[countQuadPoints];
                            PDRectangle pdLinkFirstRectangle = pdAnnotationLinks.get(0).getRectangle();
                            float x1_cover_rect = pdLinkFirstRectangle.getLowerLeftX();
                            float y1_cover_rect = pdLinkFirstRectangle.getLowerLeftY();
                            float x2_cover_rect = pdLinkFirstRectangle.getUpperRightX();
                            float y2_cover_rect = pdLinkFirstRectangle.getUpperRightY();

                            // gathering the /Rect coordinates
                            for(int j = 0; j < pdAnnotationLinks.size(); j++) {
                                PDAnnotationLink pdLink = pdAnnotationLinks.get(j);
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

                                if(x1 < x1_cover_rect) {
                                    x1_cover_rect = x1;
                                }
                                if(y1 < y1_cover_rect) {
                                    y1_cover_rect = y1;
                                }
                                if(x2 > x2_cover_rect) {
                                    x2_cover_rect = x2;
                                }
                                if(y2 > y2_cover_rect) {
                                    y2_cover_rect = y2;
                                }

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
                            PDAnnotationLink firstPDLink = pdAnnotationLinks.get(0);
                            firstPDLink.setQuadPoints(quadPoints);
                            // no need to remove /Rect, see PDLinkAppearanceHandler, method generateNormalAppearance
                            // firstPDLink.getCOSObject().setItem(COSName.RECT, null); // because firstPDLink.setRectangle(null) raises exception
                            PDRectangle pdRectangleOld = firstPDLink.getRectangle();
                            PDRectangle pdRectangleCover = new PDRectangle(x1_cover_rect, y1_cover_rect, x2_cover_rect - x1_cover_rect, y2_cover_rect - y1_cover_rect);
                            firstPDLink.setRectangle(pdRectangleCover);

                            firstPDLink.constructAppearances();
                            //remove 2nd, 3rd... Link
                            for(int j = 1; j < pdAnnotationLinks.size(); j++) {
                                pdStructureElement.removeKid(pdObjectReferences.get(j));
                                // mark Annotation for removing
                                pdAnnotationLinks.get(j).setContents(ANNOTATION_TO_REMOVE);
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
