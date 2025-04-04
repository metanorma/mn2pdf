package org.metanorma.fop.tags;


import org.apache.pdfbox.Loader;
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
public class TableCaption {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean DEBUG = true;

    private PDStructureTreeRoot structureTreeRoot;

    private PDStructureElement pdStructureElementTableCaption;

    private PDStructureNode pdStructureNodePreviousParent;



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
            document.save(pdf); // , CompressParameters.NO_COMPRESSION
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
                pdStructureElementTableCaption = pdStructureElement;
                pdStructureNodePreviousParent = pdStructureElement.getParent();

            } else if ("Table".equals(pdStructureElement.getStructureType())) {
                if (pdStructureElementTableCaption != null &&
                    pdStructureNodePreviousParent != null) {

                    // remove Caption element before the table
                    pdStructureNodePreviousParent.removeKid(pdStructureElementTableCaption);
                    pdStructureNodePreviousParent = null;

                    List<Object> kids = pdStructureElement.getKids();
                    // add Caption element as first child in Table
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

}
