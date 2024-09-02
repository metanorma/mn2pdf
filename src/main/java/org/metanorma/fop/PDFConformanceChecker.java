package org.metanorma.fop;

import static org.metanorma.Constants.PDF_A_MODE;

public class PDFConformanceChecker {

    public static boolean hasException(String msg) {
        if (msg != null) {
            return msg.contains("PDFConformanceException") && (msg.contains("PDF/UA-1") || msg.contains(PDF_A_MODE));
        }
        return false;
    }
}
