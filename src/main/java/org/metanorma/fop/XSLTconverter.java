package org.metanorma.fop;

import java.io.File;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import static org.metanorma.Constants.ERROR_EXIT_CODE;
import org.metanorma.utils.LoggerHelper;

/**
 *
 * @author Alexander Dyuzhev
 */
public class XSLTconverter {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    Properties xslparams = new Properties();
    
    Transformer transformerFO;
    
    public XSLTconverter(File fXSL) {
        
        TransformerFactory factoryFO = TransformerFactory.newInstance();
        try {
            transformerFO = factoryFO.newTransformer(new StreamSource(fXSL));
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_EXIT_CODE);
        }
            
        transformerFO.setOutputProperty(OutputKeys.ENCODING, "UTF-16"); // to fix issue with UTF-16 surrogate pairs
        
    }
    
    public Transformer getTransformer ()
    {
        return transformerFO;
    }
    
    
    public void setParams(Properties xslparams) {
        this.xslparams = xslparams;
        Iterator xslparamsIterator = xslparams.keySet().iterator();
        while(xslparamsIterator.hasNext()){
            String name   = (String) xslparamsIterator.next();
            String value = xslparams.getProperty(name);
            
            transformerFO.setParameter(name, value);
        }
        
    }
    
    public void transform(SourceXMLDocument sourceXMLDocument) throws TransformerException {
        
        //Setup input for XSLT transformation
        Source src = sourceXMLDocument.getStreamSource();
        
        //Util.OutputJaxpImplementationInfo();
            
        // Step 0. Convert XML to FO file with XSL
        StringWriter resultWriter = new StringWriter();
        StreamResult sr = new StreamResult(resultWriter);
        
        //Start XSLT transformation and FO generating
        transformerFO.transform(src, sr);

        String xmlFO = resultWriter.toString();
        
        sourceXMLDocument.setXMLFO(xmlFO);
        
    }
    
}
