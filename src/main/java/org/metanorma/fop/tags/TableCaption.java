package org.metanorma.fop.tags;

import org.apache.fop.pdf.StructureType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.metanorma.utils.LoggerHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class TableCaption {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean DEBUG = true;

    private PDStructureTreeRoot structureTreeRoot;

    private PDStructureElement pdStructureElementTableCaption;

    COSDictionary newCaption;

    private PDStructureElement elementToRemove;
    private PDStructureNode parentNodeFromRemove;

    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_tablecaption_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                moveCaptionInTable(kid);
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf);
        } catch (IOException ex) {
            logger.severe("Can't process Caption tag for Table.");
            ex.printStackTrace();
        }
        finally {
            Files.deleteIfExists(pdf_tmp);
        }
    }


    private void moveCaptionInTable(Object element) {
        if (element instanceof PDStructureElement) {
            PDStructureElement pdStructureElement = (PDStructureElement) element;

            if (!pdStructureElement.getStructureType().equals("Table")) {
                pdStructureElementTableCaption = null;
            }

            if (pdStructureElement.getStructureType().equals("Caption")) {

                newCaption = new COSDictionary();
                newCaption.setItem(COSName.S, pdStructureElement.getCOSObject().getCOSName(COSName.S));
                newCaption.setItem(COSName.P, pdStructureElement.getCOSObject().getItem("P"));
                newCaption.setItem(COSName.K, pdStructureElement.getCOSObject().getItem("K"));

                pdStructureElementTableCaption = pdStructureElement;

                String elementKey = pdStructureElement.getCOSObject().getKey().toString();

                PDStructureNode parentNode = pdStructureElement.getParent();
                if (parentNode != null) {
                    for (Object parentKid : parentNode.getKids()) {
                        if (parentKid instanceof PDStructureElement) {
                            PDStructureElement parentPDStructureElement = (PDStructureElement) parentKid;
                            String parentNodeKey = parentPDStructureElement.getCOSObject().getKey().toString();
                            if (parentPDStructureElement.getStructureType().equals("Caption") &&
                                    elementKey.equals(parentNodeKey)) {

                                //parentNode.removeKid(parentPDStructureElement);

                                // Element to remove:
                                // parentNodeFromRemove from which
                                // elementToRemove which should be removed
                                parentNodeFromRemove = parentNode;
                                elementToRemove = parentPDStructureElement;
                                break;
                            }
                        }
                    }
                }
            }else if ("Table".equals(pdStructureElement.getStructureType())) {
                if (pdStructureElementTableCaption != null) {
                    List<Object> kids = pdStructureElement.getKids();
                    // add Caption element as first child in Table

                    newCaption.setItem(COSName.P, pdStructureElement);

                    //pdStructureElementTableCaption.setParent(pdStructureElement);

                    //pdStructureElementTableCaption.getCOSObject().setItem(COSName.P, new COSString(pdStructureElement.getCOSObject().getKey().toString()));

                    //kids.add(0, pdStructureElementTableCaption);
                    kids.add(0, newCaption);

                    pdStructureElement.setKids(kids);
                    //pdStructureElementTableCaption = null;

                    //elementToRemove.setStructureType(COSName.ARTIFACT.getName());
                    /*List<Object> elementToRemoveKids = elementToRemove.getKids();
                    for (Object elementToRemoveKid: elementToRemoveKids) {
                        if (elementToRemoveKid instanceof PDMarkedContentReference) {
                            elementToRemove.removeKid((PDMarkedContentReference)elementToRemoveKid);
                        }
                    }*/

                    parentNodeFromRemove.removeKid(elementToRemove);
                }
            }
        }
        if (pdStructureElementTableCaption == null) {
            if (element instanceof PDStructureNode) {
                for (Object kid : ((PDStructureNode) element).getKids()) {
                    moveCaptionInTable(kid);
                }
            }
        }
    }

}
