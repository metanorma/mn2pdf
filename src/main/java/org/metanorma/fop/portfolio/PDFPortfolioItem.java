package org.metanorma.fop.portfolio;

public class PDFPortfolioItem {

    private String pdfAbsolutePath;

    private String documentIdentifier;

    private boolean deleteOnFlush = false;

    public PDFPortfolioItem (String pdfAbsolutePath, String documentIdentifier, boolean deleteOnFlush) {
        this.pdfAbsolutePath = pdfAbsolutePath;
        this.documentIdentifier = documentIdentifier;
        this.deleteOnFlush = deleteOnFlush;
    }

    public String getPdfAbsolutePath() {
        return pdfAbsolutePath;
    }

    public String getDocumentIdentifier() {
        return documentIdentifier;
    }

    public boolean isDeleteOnFlush() {
        return deleteOnFlush;
    }
}
