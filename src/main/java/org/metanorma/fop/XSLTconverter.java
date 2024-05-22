package org.metanorma.fop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import static org.metanorma.Constants.DEBUG;
import static org.metanorma.Constants.ERROR_EXIT_CODE;
import static org.metanorma.fop.PDFGenerator.logger;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author Alexander Dyuzhev
 */
public class XSLTconverter {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    //private Properties xslparams = new Properties();

    Document sourceXSLT;

    File tmpfileXSL;

    private Transformer transformerFO;
    
    private long startTime;
    
    public XSLTconverter(File fXSL) {
        
        TransformerFactory factoryFO = TransformerFactory.newInstance();
        try {
            sourceXSLT = getDocument(fXSL);
            transformerFO = factoryFO.newTransformer(new StreamSource(fXSL));
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
        transformerFO.setOutputProperty(OutputKeys.ENCODING, "UTF-16"); // to fix issue with UTF-16 surrogate pairs
    }

    public XSLTconverter(File fXSL, String preprocessXSLT, String outPath) {
        TransformerFactory factoryFO = TransformerFactory.newInstance();
        try {
            sourceXSLT = getDocument(fXSL);

            if (!preprocessXSLT.isEmpty()) {
                // content of fXSL file
                String xsltString = new String(Files.readAllBytes(fXSL.toPath()));
                String xsltEnd = "</xsl:stylesheet>";
                // add preprocess XSLT at the end of main XSLT
                xsltString = xsltString.replace(xsltEnd, preprocessXSLT + xsltEnd);

                // SystemId Unknown; Line #0; Column #0; Unknown error in XPath.
                // SystemId Unknown; Line #10648; Column #30; java.lang.NullPointerException
                //transformerFO = factoryFO.newTransformer(new StreamSource(new StringReader(xsltString)));

                // save XSLT to the file
                String tmpXSL = outPath + ".xsl";
                BufferedWriter writer = new BufferedWriter(new FileWriter(tmpXSL));
                writer.write(xsltString);
                writer.close();

                tmpfileXSL = new File(tmpXSL);
                transformerFO = factoryFO.newTransformer(new StreamSource(tmpfileXSL));
            } else {
                transformerFO = factoryFO.newTransformer(new StreamSource(fXSL));
            }
        } catch (TransformerConfigurationException | IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }

        transformerFO.setOutputProperty(OutputKeys.ENCODING, "UTF-16"); // to fix issue with UTF-16 surrogate pairs
    }

    public Transformer getTransformer ()
    {
        return transformerFO;
    }

    private Document getDocument (File file) {
        Document doc;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream xsltstream = new FileInputStream(file);
            doc = dBuilder.parse(xsltstream);
            return doc;
        }  catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
        return null;
    }
    
    public void setParams(Properties xslparams) {
        //this.xslparams = xslparams;
        Iterator xslparamsIterator = xslparams.keySet().iterator();
        while(xslparamsIterator.hasNext()){
            String name   = (String) xslparamsIterator.next();
            String value = xslparams.getProperty(name);
            
            transformerFO.setParameter(name, value);
        }
    }
    
    public void setParam(String name, Object value) {
        transformerFO.setParameter(name, value);
    }
    
    public Object getParam(String name) {
        return transformerFO.getParameter(name);
    }

    public void transform(SourceXMLDocument sourceXMLDocument) throws TransformerException {
        transform(sourceXMLDocument, true);
    }

    public void transform(SourceXMLDocument sourceXMLDocument, boolean isFinalTransform) throws TransformerException {

        String methodName = getClass().getSimpleName() + "." + (new Object(){}.getClass().getEnclosingMethod().getName());
        Profiler.addMethodCall(methodName);
        startTime = System.currentTimeMillis();

        //Setup input for XSLT transformation
        Source src = sourceXMLDocument.getStreamSource();
        
        //Util.OutputJaxpImplementationInfo();
            
        // Step 0. Convert XML to FO file with XSL
        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);

        transformerFO.setParameter("final_transform", String.valueOf(isFinalTransform));
        //Start XSLT transformation and FO generating
        transformerFO.transform(src, sr);

        String xmlFO = resultWriter.toString();
        
        sourceXMLDocument.setXMLFO(xmlFO);

        Profiler.printProcessingTime(methodName, startTime);
        Profiler.removeMethodCall();
    }

    private String readValue(String xpath) {
        String value = "";
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression query = xPath.compile(xpath);
            Node textElement = (Node)query.evaluate(sourceXSLT, XPathConstants.NODE);
            if(textElement != null) {
                value = textElement.getTextContent();
            }
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
        return value;
    }

    public boolean hasParamAddMathAsText() {
        String param_add_math_as_text = readValue("/*[local-name() = 'stylesheet']/*[local-name() = 'param'][@name = 'add_math_as_text']");
        return param_add_math_as_text.equalsIgnoreCase("true");
    }

    public boolean hasParamAddMathAsAttachment() {
        String param_add_math_as_attachment = readValue("/*[local-name() = 'stylesheet']/*[local-name() = 'param'][@name = 'add_math_as_attachment']");
        return param_add_math_as_attachment.equalsIgnoreCase("true");
    }

    public boolean isApplyAutolayoutAlgorithm() {
        String variable_isApplyAutolayoutAlgorithm = readValue("/*[local-name() = 'stylesheet']/*[local-name() = 'variable'][@name = 'isApplyAutolayoutAlgorithm_']");
        return variable_isApplyAutolayoutAlgorithm.trim().equalsIgnoreCase("true");
    }

    public void deleteTmpXSL() {
        if (tmpfileXSL != null) {
            try {
                Files.deleteIfExists(tmpfileXSL.toPath());
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

}
