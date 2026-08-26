package org.metanorma.fop.tags;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;
import org.metanorma.utils.LoggerHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.metanorma.Constants.DEBUG;

/**
 * Remove empty text in the PDF tags tree
 *
 * @author Alexander Dyuzhev
 */
public class EmptyTag {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean prevIsText = false;

    private PDStructureTreeRoot structureTreeRoot;

    private PDDocument document;

    private Map<Integer, Map<Integer, String>> emptyMarkedContents = new HashMap<>();

    /**
     *
     * @param pdf path to the PDF
     * @throws IOException
     */
    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_tags_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

            this.document = document;

            emptyMarkedContents = extractEmptyMarkedContents(document);

            structureTreeRoot = document.getDocumentCatalog().getStructureTreeRoot();

            List<Object> kids = structureTreeRoot.getKids();

            for (Object kid: kids) {
                int indent = 0;
                removeEmptyTags(kid, indent);
            }

            Files.deleteIfExists(pdf.toPath());
            document.save(pdf); // , CompressParameters.NO_COMPRESSION
        } catch (IOException ex) {
            logger.severe("Can't process text.");
            ex.printStackTrace();
        }
        finally {
            Files.deleteIfExists(pdf_tmp);
        }
    }

    /**
     *
     * @param element current Object in the PDF tree
     * @param indent indent for pretty print (for debug only)
     * @throws IOException
     */
    private void removeEmptyTags(Object element, int indent) throws IOException {

        if (element instanceof PDStructureNode) {
            List<Object> kids = ((PDStructureNode) element).getKids();

            List<Object> kidsToRemove = new ArrayList<>();

            for (int i = 0; i < kids.size(); i++) {
                Object kid = kids.get(i);

                //System.out.println(kid.getClass());
                if (kid instanceof PDStructureElement) {
                    //PDStructureElement se = (PDStructureElement)kid;
                    //System.out.print(getIndent(indent));
                    //System.out.println("<" + se.getStructureType() + ">");
                    prevIsText = false;
                }
                if (kid instanceof PDMarkedContentReference) {
                    if (prevIsText) {
                        indent--;
                    }

                    PDMarkedContentReference mkr = (PDMarkedContentReference)kid;

                    PDPage page = mkr.getPage();

                    int pageNum = document.getPages().indexOf(page);

                    int mcid = mkr.getMCID();

                    //System.out.print(getIndent(indent));
                    //System.out.println(textSB);
                    prevIsText = true;

                    if (emptyMarkedContents.get(pageNum) != null && emptyMarkedContents.get(pageNum).get(mcid) != null) {
                        // remove this item from tags tree
                        kidsToRemove.add(kid);
                        // System.out.println("Text removed.");
                    }
                }

                removeEmptyTags(kid, ++indent);
            }

            /*for (int i = 0; i < kidsToRemove.size(); i++) {
                kids.remove(kidsToRemove.get(i));
            }
            if (!kidsToRemove.isEmpty()) {
                ((PDStructureNode) element).setKids(kids);
            }*/

        }
    }

    /**
     * Return the empty text position (page, mcid) in the PDF tags tree
     *
     * @param document PDDocument object
     * @return Map of Page, MCID and empty text
     * @throws IOException
     */
    private Map<Integer, Map<Integer, String>> extractEmptyMarkedContents(PDDocument document) throws IOException {

        Map<Integer, Map<Integer, String>> emptyMarkedContents = new HashMap<>();

        if (DEBUG) {
            System.out.println("extractEmptyMarkedContents:");
        }

        for (int i = 0; i < document.getNumberOfPages(); i++) {

            PDPage page = document.getPage(i);

            PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
            extractor.processPage(page);

            for (PDMarkedContent group : extractor.getMarkedContents()) {
                if (!group.getTag().equals("Figure") && !group.getTag().equals("Artifact")) { //tag <Figure> contains PathPath... without text

                    if (DEBUG) {
                        System.out.print("<" + group.getTag() + "> ");
                    }

                    int mcid = group.getMCID();
                    StringBuilder textSB = new StringBuilder();
                    for (Object item : group.getContents()) {
                        if (item instanceof TextPosition) {
                            textSB.append(((TextPosition) item).getUnicode());
                        }
                    }

                    // remove white spaces
                    String text = textSB.toString().trim();
                    text = text
                            .replace("\u00a0", "")
                            .replace("\u2002", "")
                            .replace("\u2003", "")
                            .replace("\u2009", "")
                            .replace("\u200a", "")
                            .replace("\u200b", "");

                    if (text.isEmpty()) {
                        if (DEBUG) {
                            System.out.print("empty: ");
                        }
                        emptyMarkedContents.computeIfAbsent(i, k -> new HashMap<>()).put(mcid, text);
                    }
                    if (DEBUG) {
                        System.out.println(text);
                    }
                }
            }
        }
        return emptyMarkedContents;
    }

    /**
     * Return the count spaces based on indent value
     *
     * @param indent value of indent
     * @return spaces based on indent value
     */
    private String getIndent(int indent) {
        StringBuilder sb = new StringBuilder(indent);
        for (int j = 0; j < indent; j++) {
            sb.append(" ");
        }
        return sb.toString();
    }

}
