package com.metanorma.fop;

import static com.metanorma.fop.mn2pdf.DEBUG;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author Alexander Dyuzhev
 */
public class Util {
    
    public static int getFileSize(URL url) {
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }
    
    public static void downloadFile(String url, Path destPath) {
        System.out.println("Downloading " + url + "...");
        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(destPath.toString());
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (Exception ex) {
            System.out.println("Can't downloaded a file: " + ex.getMessage());
        }
    }
    
    // https://www.baeldung.com/java-compress-and-uncompress
    public static void unzipFile(Path zipPath, String destPath, List<String> defaultFontList) {
        try {
            File destDir = new File(destPath);
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toString()));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if(!zipEntry.isDirectory()) {
                    String zipEntryName = new File(zipEntry.getName()).getName();
                    if (defaultFontList.contains(zipEntryName)) {
                        //File newFile = newFile(destDir, zipEntry);
                        File newFile = new File(destDir, zipEntryName);
                        System.out.println("Extracting font file " + newFile.getAbsolutePath() + "...");
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (Exception ex) {
            System.out.println("Can't unzip a file: " + ex.getMessage());
        }
    }
    
    // These method guards against writing files to the file system outside of the target folder. 
    // This vulnerability is called Zip Slip and you can read more about it here: https://snyk.io/research/zip-slip-vulnerability
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
         
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
         
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
         
        return new File(destinationDir, new File(zipEntry.getName()).getName());
    }
    
    public static String getAppVersion() {
        String version = "";
        /*Package pckg = Util.class.getPackage();
        version = pckg.getImplementationVersion();
        */
        /*URLClassLoader cl = (URLClassLoader) mn2pdf.class.getClassLoader();
        URL url = cl.findResource("META-INF/MANIFEST.MF");
        try {
            Manifest manifest = new Manifest(url.openStream());
            Attributes attr = manifest.getMainAttributes();
            version = manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (IOException ex)  {}
        */
        try {
            Enumeration<URL> resources = mn2pdf.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                // check that this is your manifest and do what you need or get the next one
                Attributes attr = manifest.getMainAttributes();
                String mainClass = attr.getValue("Main-Class");
                if(mainClass != null && mainClass.contains("com.metanorma.fop.mn2pdf")) {
                    version = manifest.getMainAttributes().getValue("Implementation-Version");
                }
            }
        } catch (IOException ex)  {
            version = "";
        }
        
        return version;
    }
    
    public static String getDecodedBase64SVGnode(String encodedString) { //throws SAXException, IOException, ParserConfigurationException {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String decodedString = new String(decodedBytes);
        return decodedString;
        /*if (decodedString.startsWith("<?xml")) {
            return decodedString.substring(decodedString.indexOf("?>") + 2);
        } else {
            return decodedString;
        }*/
    }
    
    public static String showAvailableAWTFonts() {
        final String[] fam = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        StringBuilder log = new StringBuilder();
        log.append("====================").append("\n");
        log.append("Available fonts:").append("\n");
        for (final String element : fam) {
            log.append(element).append("\n");
        }
        log.append("====================").append("\n");
        return log.toString();
    }
    
    
    public static void OutputJaxpImplementationInfo() {
        if (DEBUG) {
            System.out.println(getJaxpImplementationInfo("DocumentBuilderFactory", DocumentBuilderFactory.newInstance().getClass()));
            System.out.println(getJaxpImplementationInfo("XPathFactory", XPathFactory.newInstance().getClass()));
            System.out.println(getJaxpImplementationInfo("TransformerFactory", TransformerFactory.newInstance().getClass()));
            System.out.println(getJaxpImplementationInfo("SAXParserFactory", SAXParserFactory.newInstance().getClass()));
        }
    }

    private static String getJaxpImplementationInfo(String componentName, Class componentClass) {
        CodeSource source = componentClass.getProtectionDomain().getCodeSource();
        return MessageFormat.format(
                "{0} implementation: {1} loaded from: {2}",
                componentName,
                componentClass.getName(),
                source == null ? "Java Runtime" : source.getLocation());
    }
    
    public static String fixFontPath(String fontPath) {
        //return fontPath.replace("~", System.getProperty("user.home"));
        if (fontPath.startsWith("~")) {
            return System.getProperty("user.home") + fontPath.substring(1);
        }
        return fontPath;
    }
    
    public static void outputLog(Path file, String content) {
        try {
            try ( 
            BufferedWriter writer = Files.newBufferedWriter(file)) {   
                writer.write(content);
            } 
        } catch (IOException ex) {
            System.out.println("Can't create a log file: " + ex.toString());
        }
    }
    
    public static void createIndexFile(String indexxmlFilePath, String intermediateXML) {
        StringBuilder indexxml = new StringBuilder();
        try {
            InputSource is = new InputSource(new StringReader(intermediateXML));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            //Document sourceXML = dBuilder.parse(new File(intermediateXMLFilePath));
            Document sourceXML = dBuilder.parse(is);
            
            XPath xPath = XPathFactory.newInstance().newXPath();
           
            NodeList pageNumberCitations = sourceXML.getElementsByTagName("fo:page-number-citation");
            for (int i = 0; i < pageNumberCitations.getLength(); i++) {
                Node pageNumberCitation = pageNumberCitations.item(i);
                Node structId = pageNumberCitation.getAttributes().getNamedItem("foi:struct-id");
                String structIdValue = structId.getTextContent();
                
                XPathExpression query = xPath.compile("//text[@struct-ref = '" + structIdValue + "'][preceding-sibling::*[1][local-name() = 'id']][1]");
                Node textElement = (Node)query.evaluate(sourceXML, XPathConstants.NODE);
                /* <id name="4_2">
                ->   <text foi:struct-ref="4c7">12</text> */
                if(textElement != null) {
                    String pageNum = textElement.getTextContent(); //12    
                    
                    XPathExpression queryId = xPath.compile("//text[@struct-ref = '" + structIdValue + "']/preceding-sibling::id[1]");
                    Node idElement = (Node)queryId.evaluate(sourceXML, XPathConstants.NODE);
                    String name = "";
                    /* -> <id name="4_2">
                       <text foi:struct-ref="4c7">12</text> */
                    if (idElement != null) {
                        try {
                            name = idElement.getAttributes().getNamedItem("name").getTextContent();
                        } catch (Exception ex) {}
                    }
                    
                    if (!pageNum.isEmpty() && !name.isEmpty()) {
                        indexxml.append("<item id=\"");
                        indexxml.append(name);
                        indexxml.append("\">");
                        indexxml.append(pageNum);
                        indexxml.append("</item>\n");
                    }
                }    
            }

            if (indexxml.length() != 0) {
                indexxml.insert(0, "<index>\n");
                indexxml.append("</index>");
                try ( 
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(indexxmlFilePath))) {
                        writer.write(indexxml.toString());                    
                }
            }
               
        } catch (XPathExpressionException ex) {
            System.out.println(ex.toString());
        }    
        catch (Exception ex) {
            System.err.println("Can't save index.xml into temporary folder");
            ex.printStackTrace();
        }    
    }
    
    
}
