package org.metanorma.fop.tags;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
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
 *
 * @author Alexander Dyuzhev
 */
public class TableCaptionTFoot {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean DEBUG = true;

    private PDStructureTreeRoot structureTreeRoot;

    private PDStructureElement pdStructureElementTableCaption;

    private PDStructureNode pdStructureNodePreviousParent;

    private PDStructureElement pdStructureElementTable;
    private PDStructureElement pdStructureElementTableTFoot;

    private PDStructureElement pdStructureElementTFoot;


    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_tablecaption_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                moveCaptionInTable(kid);
            }

            for (Object kid: kids) {
                moveTFootInTable(kid);
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf); // , CompressParameters.NO_COMPRESSION

        } catch (IOException ex) {
            logger.severe("Can't process Caption tag for Table.");
            ex.printStackTrace();
        }
        finally {
            Files.deleteIfExists(pdf_tmp);
        }




    }

    /**
     * Move tag <Caption> (before <Table>) inside <Table>
     * @param element current element
     */
    private void moveCaptionInTable(Object element) {
        if (element instanceof PDStructureElement) {
            PDStructureElement pdStructureElement = (PDStructureElement) element;

            if (!pdStructureElement.getStructureType().equals("Table")) {
                pdStructureElementTableCaption = null;
            }

            if (pdStructureElement.getStructureType().equals("Caption")) {
                pdStructureElementTableCaption = pdStructureElement;
                pdStructureNodePreviousParent = pdStructureElement.getParent();

            } else if ("Table".equals(pdStructureElement.getStructureType())) {
                if (pdStructureElementTableCaption != null &&
                    pdStructureNodePreviousParent != null) {

                    // remove Caption element before the table
                    pdStructureNodePreviousParent.removeKid(pdStructureElementTableCaption);
                    pdStructureNodePreviousParent = null;

                    List<Object> kids = pdStructureElement.getKids();
                    // add Caption element as first kid in Table
                    pdStructureElementTableCaption.setParent(pdStructureElement);
                    kids.add(0, pdStructureElementTableCaption);
                    pdStructureElement.setKids(kids);
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

    /**
     * Move tag <TFoot> (after <Table>) inside <Table>, or inside <Table><TFoot> if exists already
     * @param element current element
     */
    private void moveTFootInTable(Object element) {
        if (element instanceof PDStructureElement) {
            PDStructureElement pdStructureElement = (PDStructureElement) element;

            String structureType = pdStructureElement.getStructureType();

            if (structureType.equals("Table")) {
                pdStructureElementTable = pdStructureElement;
                pdStructureElementTableTFoot = null;

                for (Object tableKid : pdStructureElementTable.getKids()) {
                    if (tableKid instanceof PDStructureElement) {
                        PDStructureElement tableKidElement = (PDStructureElement) tableKid;
                        if (tableKidElement.getStructureType().equals("TFoot"))
                            // find the <Table>/<TFoot>
                            pdStructureElementTableTFoot = tableKidElement;
                    }
                }

            } else if (structureType.equals("TFoot")) {
                pdStructureElementTFoot = pdStructureElement;

                // remove TFoot from tags tree (from parent element)
                PDStructureNode pdStructureNodeTFootParent = pdStructureElementTFoot.getParent();
                pdStructureNodeTFootParent.removeKid(pdStructureElementTFoot);

                if (pdStructureElementTableTFoot != null) {
                    // add >TFoot> kids into exist <Table><TFoot>
                    List<Object> tableFootKids = pdStructureElementTableTFoot.getKids();
                    for (Object tFootKid : pdStructureElementTFoot.getKids()) {
                        if (tFootKid instanceof PDStructureElement) {
                            PDStructureElement tFootKidElement = (PDStructureElement) tFootKid;
                            tFootKidElement.setParent(pdStructureElementTableTFoot);
                            tableFootKids.add(tFootKidElement);
                        }
                    }
                    pdStructureElementTableTFoot.setKids(tableFootKids);
                } else {
                    // add TFoot at the end of Table
                    List<Object> tableKids = pdStructureElementTable.getKids();
                    pdStructureElementTFoot.setParent(pdStructureElementTable);
                    tableKids.add(pdStructureElementTFoot);
                    pdStructureElementTable.setKids(tableKids);
                }

            } else if (!structureType.equals("Table") &&
                        !structureType.equals("TFoot")) {
                pdStructureElementTable = null;
                pdStructureElementTableTFoot = null;
                pdStructureElementTFoot = null;
            }
        }

        // process kids elements only if Table and TFoot not found the yet
        if (pdStructureElementTable == null && pdStructureElementTFoot == null) {
            if (element instanceof PDStructureNode) {
                for (Object kid : ((PDStructureNode) element).getKids()) {
                    moveTFootInTable(kid);
                }
            }
        }
    }
}
