package org.metanorma.fop.annotations;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.metanorma.utils.LoggerHelper;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author Alexander Dyuzhev
 */
public class FileAttachmentAnnotation {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    private boolean DEBUG = false;
    
    public void process(File pdf) throws IOException {
        //PDDocument document = null;

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(pdf.getAbsoluteFile()))) {
            //document = PDDocument.load(pdf);

            ArrayList<String> embeddedFileAnnotations = new ArrayList<>();

            int numberOfPages = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                List<PDAnnotation> annotations = page.getAnnotations();
                
                for (PDAnnotation annotation: annotations) {
                    if (annotation instanceof PDAnnotationFileAttachment) {
                        annotation.constructAppearances();
                        PDFileSpecification f = ((PDAnnotationFileAttachment) annotation).getFile();
                        embeddedFileAnnotations.add(f.getFile());
                        //annotations.set(annotataionIndex, annotation);
                    }
                }
                //document.getPage(pageIndex).setAnnotations(annotations);
            }

            // remove attachments which have FileAttachment annotation equivalent
            PDDocumentNameDictionary namesDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());
            PDEmbeddedFilesNameTreeNode efTree = namesDictionary.getEmbeddedFiles();
            if (efTree != null)
            {
                Map<String, PDComplexFileSpecification> names = efTree.getNames();
                Map<String, PDComplexFileSpecification> newnames = new HashMap<>();

                Iterator<Map.Entry<String,PDComplexFileSpecification>> iter = names.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String,PDComplexFileSpecification> entry = iter.next();
                    PDComplexFileSpecification fileSpec = entry.getValue();
                    String embeddedFileName = fileSpec.getFile();
                    if(!embeddedFileAnnotations.contains(embeddedFileName)){
                        newnames.put(entry.getKey(), entry.getValue());
                    }
                }

                efTree.setNames(newnames);
                namesDictionary.setEmbeddedFiles(efTree);
            }

            document.save(pdf);
                
        } catch (IOException ex) {
            logger.severe("Can't read annotation data from PDF.");
            ex.printStackTrace();
        }


        /*finally {
            if( document != null ) {
                document.close();
            }
        }*/
        
    }
    
}
