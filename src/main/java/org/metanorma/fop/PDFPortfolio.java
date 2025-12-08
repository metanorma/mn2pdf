package org.metanorma.fop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

//https://github.com/apache/pdfbox/blob/2.0/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/CreatePortableCollection.java
public class PDFPortfolio
{
    private Map<String, String> filesList;

    /**
     * Constructor.
     */
    public PDFPortfolio(Map<String, String> filesList)
    {
        this.filesList = filesList;
    }

    /**
     * Create a portable collection PDF with the files.
     *
     * @param outPDF The file to write the PDF to.
     *
     * @throws IOException If there is an error writing the data.
     */
    public void generate(String outPDF) throws Exception
    {
        byte[] pdfDefaultBytes = Util.getBytesFromResources(getClass().getClassLoader(), "pdfportfolio_default_page.pdf");

        try (PDDocument doc = Loader.loadPDF(pdfDefaultBytes))
        {
            //embedded files are stored in a named tree
            PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();

            List<PDComplexFileSpecification> listPDComplexFileSpecification = new ArrayList<>();
            for (Map.Entry<String, String> item : filesList.entrySet()) {

                //first create the file specification, which holds the embedded file
                PDComplexFileSpecification fs = new PDComplexFileSpecification();

                File attachmentFile = new File(item.getKey());

                // use both methods for backwards, cross-platform and cross-language compatibility.
                fs.setFile(attachmentFile.getName());
                fs.setFileUnicode(attachmentFile.getName());

                FileInputStream fis = new FileInputStream(attachmentFile);
                PDEmbeddedFile embeddedFile = new PDEmbeddedFile(doc, fis);
                embeddedFile.setSubtype("application/pdf");
                embeddedFile.setSize((int)attachmentFile.length()); // max 2gb
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(attachmentFile.lastModified());
                embeddedFile.setCreationDate(calendar);
                fis.close();

                // use both methods for backwards, cross-platform and cross-language compatibility.
                fs.setEmbeddedFile(embeddedFile);
                fs.setEmbeddedFileUnicode(embeddedFile);
                fs.setFileDescription(item.getValue());

                listPDComplexFileSpecification.add(fs);
            }

            Map<String, PDComplexFileSpecification> map = new HashMap<>();

            String firstFile = listPDComplexFileSpecification.get(0).getFile();
            for (PDComplexFileSpecification listPDComplexFileSpecificationItem : listPDComplexFileSpecification) {
                map.put(listPDComplexFileSpecificationItem.getFile(), listPDComplexFileSpecificationItem);
            }

            // create a new tree node and add the embedded file
            PDEmbeddedFilesNameTreeNode treeNode = new PDEmbeddedFilesNameTreeNode();
            treeNode.setNames(map);
            // add the new node as kid to the root node
            List<PDEmbeddedFilesNameTreeNode> kids = new ArrayList<>();
            kids.add(treeNode);
            efTree.setKids(kids);

            // add the tree to the document catalog
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
            names.setEmbeddedFiles(efTree);
            doc.getDocumentCatalog().setNames(names);

            // show attachments panel in some viewers 
            doc.getDocumentCatalog().setPageMode(PageMode.USE_ATTACHMENTS);

            // create collection directory
            COSDictionary collectionDic = new COSDictionary();
            COSDictionary schemaDict = new COSDictionary();
            schemaDict.setItem(COSName.TYPE, COSName.COLLECTION_SCHEMA);
            COSDictionary sortDic = new COSDictionary();
            sortDic.setItem(COSName.TYPE, COSName.COLLECTION_SORT);
            sortDic.setString(COSName.A, "true"); // sort ascending
            // "it identifies a field described in the parent collection dictionary"
            // sort by field 'order number'
            sortDic.setItem(COSName.S, COSName.getPDFName("Order"));
            collectionDic.setItem(COSName.TYPE, COSName.COLLECTION);
            collectionDic.setItem(COSName.SCHEMA, schemaDict);
            collectionDic.setItem(COSName.SORT, sortDic);
            collectionDic.setItem(COSName.VIEW, COSName.T); // D Details mode (Initial view Top pane), T (Initial view Left pane)
            collectionDic.setString(COSName.D, firstFile);

            COSDictionary fieldOrder = new COSDictionary();
            fieldOrder.setItem(COSName.TYPE, COSName.COLLECTION_FIELD);
            fieldOrder.setItem(COSName.SUBTYPE, COSName.N); // type: number field
            fieldOrder.setString(COSName.N, "Order number");
            fieldOrder.setInt(COSName.O, 1);
            fieldOrder.setBoolean(COSName.V, false); // hide column

            schemaDict.setItem("Order", fieldOrder);

            doc.getDocumentCatalog().getCOSObject().setItem(COSName.COLLECTION, collectionDic);
            doc.getDocumentCatalog().setVersion("1.7");

            for (PDComplexFileSpecification listPDComplexFileSpecificationItem : listPDComplexFileSpecification) {
                // collection item dictionary with fields for Nth file
                COSDictionary ciDict = new COSDictionary();
                ciDict.setItem(COSName.TYPE, COSName.COLLECTION_ITEM);
                ciDict.setInt("Order", listPDComplexFileSpecification.indexOf(listPDComplexFileSpecificationItem));
                listPDComplexFileSpecificationItem.getCOSObject().setItem(COSName.CI, ciDict);
            }

            Files.deleteIfExists(Paths.get(outPDF));
            doc.save(outPDF);
        }
    }
}
