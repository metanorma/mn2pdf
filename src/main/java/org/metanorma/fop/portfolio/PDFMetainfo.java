package org.metanorma.fop.portfolio;

import java.io.File;
import java.nio.file.Paths;

public class PDFMetainfo {

    private String xmlFilePath;

    private String pdfFileName;

    private String documentIdentifier;

    public PDFMetainfo (String xmlFilePath, String pdfFileName, String documentIdentifier) {
        this.xmlFilePath = xmlFilePath;
        this.pdfFileName = pdfFileName;
        this.documentIdentifier = documentIdentifier;
    }

    public String getXmlFilePath() {
        return xmlFilePath;
    }

    public String getPDFFileName() {
        return pdfFileName;
    }

    public String getDocumentIdentifier() {
        return documentIdentifier;
    }
}
