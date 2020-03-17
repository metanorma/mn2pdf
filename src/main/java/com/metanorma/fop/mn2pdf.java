package com.metanorma.fop;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import net.sourceforge.jeuclid.fop.plugin.JEuclidFopFactoryConfigurator;
import org.xml.sax.SAXException;


/**
 * This class for the conversion of an XML file to PDF using FOP and JEuclid
 */
public class mn2pdf {
    static final String USAGE = "Usage: java -jar mn2pdf <path to fonts folder> <path to source XML file> <path to source XSLT file> <path to output PDF>";
    static final String INPUT_NOT_FOUND = "Error: %s file '%s' not found!";
    //static final String FOP_CONFIG_INPUT = "FOP config";
    static final String FONTS_FOLDER_INPUT = "Fonts path";
    static final String XML_INPUT = "XML";
    static final String XSL_INPUT = "XSL";
    static final String INPUT_LOG = "Input: %s (%s)";

    static final int ERROR_EXIT_CODE = -1;

    /**
     * Converts an XML file to a PDF file using FOP
     * @param config the FOP config file
     * @param xml the XML source file
     * @param xsl the XSL file
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException, SAXException In case of a FOP problem
     */
    public void convertmn2pdf(File fontPath, File xml, File xsl, File pdf) throws IOException, FOPException, SAXException, TransformerException, TransformerConfigurationException, TransformerConfigurationException {

        OutputStream out = null;
        try {
            // Step 0. Convert XML to FO file with XSL
            //Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xsl));

            //Setup input for XSLT transformation
            Source src = new StreamSource(xml);

            //Setup output
            StringWriter resultWriter = new StringWriter();
            StreamResult sr = new StreamResult(resultWriter);

            //Start XSLT transformation and FOP generating
            transformer.transform(src, sr);
            String xmlFO = resultWriter.toString();

            // Step 1: Construct a FopFactory by specifying a reference to the configuration file
            // (reuse if you plan to render multiple documents!)
            fontConfig fontConfig = new fontConfig(fontPath);
            FopFactory fopFactory = FopFactory.newInstance(fontConfig.getUpdatedConfig());
            //FopFactory fopFactory = FopFactory.newInstance(config)
            
            JEuclidFopFactoryConfigurator.configure(fopFactory);
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            // configure foUserAgent as desired

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new FileOutputStream(pdf);
            out = new BufferedOutputStream(out);

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            // Setup JAXP using identity transformer
            factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer(); // identity transformer

            // Setup input stream
            Source srcFO = new StreamSource(new StringReader(xmlFO));

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(srcFO, res);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    /**
     * Main method.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(USAGE);
            System.exit(ERROR_EXIT_CODE);
        }

        try {
            System.out.println("mn2pdf\n");
            System.out.println("Preparing...");

            //Setup font path, input and output files
            final String argFontsPath = args[0];
            File fFontsPath = new File(argFontsPath);
            final String argXML = args[1];
            File fXML = new File(argXML);
            if (!fXML.exists()) {
                System.out.println(String.format(INPUT_NOT_FOUND, XML_INPUT, fXML));
                System.exit(ERROR_EXIT_CODE);
            }
            final String argXSL = args[2];
            File fXSL = new File(argXSL);
            if (!fXSL.exists()) {
                System.out.println(String.format(INPUT_NOT_FOUND, XSL_INPUT, fXSL));
                System.exit(ERROR_EXIT_CODE);
            }
            final String argPDF = args[3];
            File fPDF = new File(argPDF);

            System.out.println(String.format(INPUT_LOG, FONTS_FOLDER_INPUT, argFontsPath));
            System.out.println(String.format(INPUT_LOG, XML_INPUT, fXML));
            System.out.println(String.format(INPUT_LOG, XSL_INPUT, fXSL));
            System.out.println("Output: PDF (" + fPDF + ")");
            System.out.println();
            System.out.println("Transforming...");

            mn2pdf app = new mn2pdf();
            app.convertmn2pdf(fFontsPath, fXML, fXSL, fPDF);

            System.out.println("Success!");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }

    }

}
