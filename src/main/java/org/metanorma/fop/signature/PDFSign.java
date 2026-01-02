package org.metanorma.fop.signature;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.examples.signature.CreateSignatureBase;
import org.apache.pdfbox.examples.signature.SigUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

// From https://github.com/apache/pdfbox/blob/3.0/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateSignature.java
public class PDFSign extends CreateSignatureBase
{
    /**
     * Initialize the signature creator with a keystore and certificate password.
     *
     * @param keystore the pkcs12 keystore containing the signing certificate
     * @param pin the password for recovering the key
     * @throws KeyStoreException if the keystore has not been initialized (loaded)
     * @throws NoSuchAlgorithmException if the algorithm for recovering the key cannot be found
     * @throws UnrecoverableKeyException if the given password is wrong
     * @throws CertificateException if the certificate is not valid as signing time
     * @throws IOException if no certificate could be found
     */
    public PDFSign(KeyStore keystore, char[] pin)
            throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException
    {
        super(keystore, pin);
    }

    /**
     * Signs the given PDF file.
     * @param inFile input PDF file
     * @throws IOException if the input file could not be read
     */
    public void signDetached(File inFile, File outFile) throws IOException
    {
        if (inFile == null || !inFile.exists())
        {
            throw new FileNotFoundException("Document for signing does not exist");
        }

        //File outFile =
        // sign
        try (FileOutputStream fos = new FileOutputStream(outFile);
                PDDocument doc = Loader.loadPDF(inFile))
        {
            signDetached(doc, fos);
        }
    }

    public void signDetached(PDDocument document, OutputStream output)
            throws IOException
    {
        // call SigUtils.checkCrossReferenceTable(document) if Adobe complains
        // and read https://stackoverflow.com/a/71293901/535646
        // and https://issues.apache.org/jira/browse/PDFBOX-5382

        int accessPermissions = SigUtils.getMDPPermission(document);
        if (accessPermissions == 1)
        {
            throw new IllegalStateException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
        }     

        // create signature dictionary
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

        // the signing date, needed for valid signature
        signature.setSignDate(Calendar.getInstance());

        // Optional: certify 
        if (accessPermissions == 0)
        {
            SigUtils.setMDPPermission(document, signature, 2);
        }        

        SignatureOptions signatureOptions = new SignatureOptions();
        // Size can vary, but should be enough for purpose.
        signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
        // register signature dictionary and sign interface
        document.addSignature(signature, this, signatureOptions);

        // write incremental (only for signing purpose)
        document.saveIncremental(output);
    }
}
