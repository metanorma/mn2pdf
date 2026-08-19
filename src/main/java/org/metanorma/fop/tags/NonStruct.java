package org.metanorma.fop.tags;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.metanorma.utils.LoggerHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class intended for PDF post-processing:
 * merge the tags NonStruct with preceding tags
 *
 * @author Alexander Dyuzhev
 */
public class NonStruct {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean DEBUG = true;

    private PDStructureTreeRoot structureTreeRoot;

    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_nonstruct_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                mergeNonStruct(kid);
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf); // , CompressParameters.NO_COMPRESSION

        } catch (IOException ex) {
            logger.severe("Can't process NonStruct tags.");
            ex.printStackTrace();
        }
        finally {
            Files.deleteIfExists(pdf_tmp);
        }
    }

    /**
     * Move NonStruct kids to preceding tag
     * @param element current element
     */
    private void mergeNonStruct(Object element) {
        if (element instanceof PDStructureElement) {
            PDStructureElement pdStructureElement = (PDStructureElement) element;

            if (pdStructureElement.getStructureType().equals("NonStruct")) {

                List<Object> currentKids = pdStructureElement.getKids();

                PDStructureNode parent = pdStructureElement.getParent();
                if (parent != null) {
                    List<Object> parentKids = parent.getKids();

                    // doesn't working
                    //int indexCurrent = currentKids.indexOf(pdStructureElement);

                    // find current tag position in the parent kids
                    long currObjNum = pdStructureElement.getCOSObject().getKey().getNumber();
                    int indexCurrent = -1;
                    for (int i = 0; i < parentKids.size(); i++) {
                        Object kid = parentKids.get(i);
                        if (kid instanceof PDStructureElement) {
                            long objNum = ((PDStructureElement) kid).getCOSObject().getKey().getNumber();
                            if (objNum == currObjNum) {
                                indexCurrent = i;
                                break;
                            }
                        }
                    }

                    if (indexCurrent > 0) {
                        Object precedingTag = parentKids.get(indexCurrent - 1);
                        if (precedingTag instanceof PDStructureElement) {
                            PDStructureElement prevTag = (PDStructureElement) precedingTag;
                            List<Object> prevKids = prevTag.getKids();

                            for (Object currentKid : currentKids) {
                                if (currentKid instanceof PDStructureElement) {
                                    ((PDStructureElement) currentKid).setParent(prevTag);
                                }
                                prevKids.add(currentKid);
                            }
                            prevTag.setKids(prevKids);
                        }
                        parent.removeKid(pdStructureElement);
                    }
                }
            }
        }

        if (element instanceof PDStructureNode) {
            for (Object kid : ((PDStructureNode) element).getKids()) {
                mergeNonStruct(kid);
            }
        }

    }

}
