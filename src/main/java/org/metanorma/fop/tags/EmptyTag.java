package org.metanorma.fop.tags;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDArtifactMarkedContent;
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
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class EmptyTag {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    private boolean prevIsText = false;

    private PDStructureTreeRoot structureTreeRoot;

    public void process(File pdf) throws IOException {

        Path pdf_tmp = Paths.get(pdf.getAbsolutePath() + "_tags_tmp");
        Files.copy(Paths.get(pdf.getAbsolutePath()), pdf_tmp, StandardCopyOption.REPLACE_EXISTING);

        try (PDDocument document = Loader.loadPDF(pdf_tmp.toFile())) {

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

    private void removeEmptyTags(Object element, int indent) throws IOException {

        if (element instanceof PDStructureNode) {
            List<Object> kids = ((PDStructureNode) element).getKids();

            List<Object> kidsToRemove = new ArrayList<>();

            for (int i = 0; i < kids.size(); i++) {
                Object kid = kids.get(i);

                //System.out.println(kid.getClass());
                if (kid instanceof PDStructureElement) {
                    PDStructureElement se = (PDStructureElement)kid;
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
                    int mcid = mkr.getMCID();

                    PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
                    extractor.processPage(page);
                    String tag = "";
                    StringBuilder textSB = new StringBuilder();
                    for (PDMarkedContent group : extractor.getMarkedContents()) {
                        if (group.getMCID() == mcid) {
                            tag = group.getTag();

                            if (!tag.equals("Figure")) { //tag <Figure> contains PathPath... without text
                                for (Object item : group.getContents()) {
                                    if (item instanceof TextPosition) {
                                        textSB.append(((TextPosition) item).getUnicode());
                                    }
                                }
                            }
                            break;
                        }
                    }

                    //System.out.print(getIndent(indent));
                    //System.out.println(textSB);
                    prevIsText = true;

                    // remove white spaces
                    String text = textSB.toString().trim();
                    text = text
                            .replace("\u00a0", "")
                            .replace("\u2002", "")
                            .replace("\u2003", "")
                            .replace("\u2009", "")
                            .replace("\u200a", "")
                            .replace("\u200b", "");

                    if (!tag.equals("Figure") && text.isEmpty()) {
                        // remove this item from tags tree
                        kidsToRemove.add(kid);
                        // System.out.println("Text removed.");
                    }
                }

                removeEmptyTags(kid, ++indent);
            }

            for (int i = 0; i < kidsToRemove.size(); i++) {
                kids.remove(kidsToRemove.get(i));
            }
            if (!kidsToRemove.isEmpty()) {
                ((PDStructureNode) element).setKids(kids);
            }


        }
    }

    private String getIndent(int indent) {
        StringBuilder sb = new StringBuilder(indent);
        for (int j = 0; j < indent; j++) {
            sb.append(" ");
        }
        return sb.toString();
    }

}
