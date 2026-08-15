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
 *
 * @author Alexander Dyuzhev
 */
public class FormulaAltText {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean DEBUG = true;

    private PDStructureTreeRoot structureTreeRoot;

    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_tablecaption_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                copyFigureAltTextToFormula(kid);
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf); // , CompressParameters.NO_COMPRESSION

        } catch (IOException ex) {
            logger.severe("Can't process alt text for Figure.");
            ex.printStackTrace();
        }
        finally {
            Files.deleteIfExists(pdf_tmp);
        }
    }

    /**
     * Copy alt text and actual text attributes from <Figure> to <Formula>
     * @param element current element
     */
    private void copyFigureAltTextToFormula(Object element) {
        boolean found = false;
        if (element instanceof PDStructureElement) {
            PDStructureElement pdStructureElement = (PDStructureElement) element;

            if (pdStructureElement.getStructureType().equals("Formula")) {

                String figureActualText = "";
                String figureAltText = "";
                for (Object kid: pdStructureElement.getKids()) {
                    if (kid instanceof PDStructureElement) {
                        PDStructureElement kidElement = (PDStructureElement) kid;
                        if (kidElement.getStructureType().equals("Figure")) {
                            figureActualText = kidElement.getActualText();
                            figureAltText = kidElement.getAlternateDescription();
                            found = true;
                            break;
                        }
                    }
                }
                if (figureActualText != null && !figureActualText.isEmpty()) {
                    pdStructureElement.setActualText(figureActualText);
                }
                if (figureAltText != null && !figureAltText.isEmpty()) {
                    pdStructureElement.setAlternateDescription(figureAltText);
                }

            }
        }
        if (!found) {
            if (element instanceof PDStructureNode) {
                for (Object kid : ((PDStructureNode) element).getKids()) {
                    copyFigureAltTextToFormula(kid);
                }
            }
        }
    }

}
