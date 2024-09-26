package org.metanorma.fop;

import org.metanorma.utils.LoggerHelper;
import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.report.HTMLReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import static org.metanorma.Constants.PDF_A_MODE;

public class VeraPDFValidator {

    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

    // see https://docs.verapdf.org/develop/
    public void validate(File filePDF, String validationFlavour) {
        VeraGreenfieldFoundryProvider.initialise();
        String sPDFAmode = validationFlavour.substring(validationFlavour.indexOf("-"));
        if (validationFlavour.contains("/UA-")) {
            sPDFAmode = validationFlavour.substring(validationFlavour.indexOf("/") + 1).replace("-","").toLowerCase();
        }

        PDFAFlavour flavour = PDFAFlavour.fromString(sPDFAmode);
        try (PDFAParser parser = Foundries.defaultInstance().createParser(new FileInputStream(filePDF), flavour)) {
            PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, false);
            ValidationResult result = validator.validate(parser);

            if (result.isCompliant()) {
                // File is a valid PDF/A  PDF_A_MODE
                return;
            } else {
                // it isn't
                logger.severe("PDF isn't valid " + validationFlavour + ":");
                String veraPDFresult = result.toString();
                veraPDFresult = veraPDFresult.replaceAll("(TestAssertion )","\n$1")
                        .replaceAll("\\s(message=)","\n$1")
                        .replaceAll("\\s(location=)","\n$1");
                logger.severe(veraPDFresult);
                //HTMLReport.writeHTMLReport()
            }
        } catch (IOException | ValidationException | ModelParsingException | EncryptedPdfException exception) {
            // Exception during validation
            logger.severe(exception.toString());
        }
        return;
    }

}
