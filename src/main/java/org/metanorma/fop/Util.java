package org.metanorma.fop;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import static org.metanorma.Constants.DEBUG;
import static org.metanorma.fop.SourceXMLDocument.tmpfilepath;

import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.apache.fop.svg.SVGUtilities;
import org.metanorma.fop.fonts.FOPFont;
import org.metanorma.utils.LoggerHelper;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.*;
import org.xml.sax.InputSource;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 *
 * @author Alexander Dyuzhev
 */
public class Util {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);

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

    public static long getFileSize(File file) {
        long length = file.length();
        return length;
    }

    public static void downloadFile(String url, Path destPath) {
        logger.info("Downloading " + url + "...");
        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(destPath.toString());
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Can''t downloaded a file: {0}", ex.getMessage());
        }
    }
    
    // https://www.baeldung.com/java-compress-and-uncompress
    public static void unzipFile(Path zipPath, String destPath, List<String> defaultFontList, List<String> extractedFonts) {
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
                        logger.log(Level.INFO, "Extracting font file {0}...", newFile.getAbsolutePath());
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        extractedFonts.add(zipEntryName);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (Exception ex) {
            logger.log(Level.INFO, "Can''t unzip a file: {0}", ex.getMessage());
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
                if(mainClass != null && mainClass.contains("org.metanorma.fop.mn2pdf")) {
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
            logger.info(getJaxpImplementationInfo("DocumentBuilderFactory", DocumentBuilderFactory.newInstance().getClass()));
            logger.info(getJaxpImplementationInfo("XPathFactory", XPathFactory.newInstance().getClass()));
            logger.info(getJaxpImplementationInfo("TransformerFactory", TransformerFactory.newInstance().getClass()));
            logger.info(getJaxpImplementationInfo("SAXParserFactory", SAXParserFactory.newInstance().getClass()));
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
            logger.log(Level.INFO, "Can''t create a log file: {0}", ex.toString());
        }
    }
    
    public static void createIndexFile_Old(String indexxmlFilePath, String intermediateXML) {
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
            logger.info(ex.toString());
        }    
        catch (Exception ex) {
            logger.severe("Can't save index.xml into temporary folder");
            ex.printStackTrace();
        }    
    }
    
    // get file from classpath, resources folder
    public static InputStream getStreamFromResources(ClassLoader classLoader, String fileName) throws Exception {
        InputStream stream = classLoader.getResourceAsStream(fileName);
        if(stream == null) {
            throw new Exception("Cannot get resource \"" + fileName + "\" from Jar file.");
        }
        return stream;
    }

    public static int getCoverPagesCount (File fXSL) {
        int countpages = 0;
        try {            
            // open XSL and find 
            // <xsl:variable name="coverpages_count">2</xsl:variable>
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream xmlstream = new FileInputStream(fXSL);
            Document sourceXML = dBuilder.parse(xmlstream);
            
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//*[local-name() = 'variable'][@name='coverpages_count']");
            NodeList vars = (NodeList) expr.evaluate(sourceXML, XPathConstants.NODESET);
            if (vars.getLength() > 0) {
                countpages = Integer.valueOf(vars.item(0).getTextContent());
            }
        } catch (Exception ex) {
            logger.severe("Can't read coverpages_count variable from source XSL.");
            ex.printStackTrace();
        }        
        return countpages;
    }
    
    public static String readValueFromXML(File file, String xpath) {
        String value = "";
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document sourceXML = dBuilder.parse(file);
            
            XPath xPath = XPathFactory.newInstance().newXPath();
            
            XPathExpression query = xPath.compile(xpath);
            Node textElement = (Node)query.evaluate(sourceXML, XPathConstants.NODE);
            if(textElement != null) {
                value = textElement.getTextContent();
            }        
            
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
        
        return value;
    }
    
    public static String readValueFromXMLString(String xml, String xpath) {
        String value = "";
        try {
            InputSource is = new InputSource(new StringReader(xml));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document sourceXML = dBuilder.parse(is);
            
            XPath xPath = XPathFactory.newInstance().newXPath();
            
            XPathExpression query = xPath.compile(xpath);
            Node textElement = (Node)query.evaluate(sourceXML, XPathConstants.NODE);
            if(textElement != null) {
                value = textElement.getTextContent();
            }        
            
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
        
        return value;
    }
        
    public static String unescape(String str) {
        return org.apache.commons.lang3.StringEscapeUtils.unescapeXml(str);
    }
    
    private static String previousFontName = "";
    
    public static String getFontSize(String text, String fontName, int width, int height) {
        
        if (!previousFontName.equals(fontName)) {
            // Register font for rendering string
            Font[] fonts;
            fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            boolean isExist = false;
            for (Font font : fonts) {
                /*System.out.print(font.getFontName() + " : ");
                System.out.print(font.getFamily() + " : ");
                System.out.print(font.getName());
                System.out.println();*/
                if (font.getFontName().equals(fontName)) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                for(FOPFont fopFont: fontConfig.fopFonts) {
                    if (fopFont.isUsing() && fopFont.getName().equals(fontName)) {
                        previousFontName = fontName;
                        fontConfig.registerFont(null, fopFont.getPath());
                        isExist = true;
                        break;
                    }
                }
            }
            if (!isExist) {
                 fontName = "Serif"; // use it if font not found
            }
        }
        
        Font font = new Font(fontName, Font.PLAIN, 12);
        float fontSize = 12.0f; // start value
        
        int renderedWidth = 20000000;
        int renderedHeight = 20000000;
        
        if (DEBUG) {
            //System.out.println("Math object width=" + width + ", height=" + height);
        }
        
        while ((renderedWidth > width || renderedHeight > height) && fontSize >= 1.0f) {
            fontSize-=1.0f;
            font = font.deriveFont(fontSize);
            FontRenderContext frc = new FontRenderContext(null, false, false);
            TextLayout tl = new TextLayout(text, font, frc);
            //Rectangle rect = tl.getPixelBounds(frc, 0f, 0f);
            Rectangle2D rect2d = tl.getBounds();
            
            renderedWidth = (int) (rect2d.getWidth() * 1000);
            renderedHeight = (int) (rect2d.getHeight()* 1000);
            
            if (DEBUG) {
                //System.out.println("font-size=" + fontSize + ", width=" + renderedWidth + ", height=" + renderedHeight);
            }
        }
        fontSize = (fontSize == 0f ? 0.5f : fontSize);
        return String.valueOf((int)(fontSize * 1000));
        
    }

    public static int getStringWidth(String text, String fontName) {
        int stringWidth = 0;
        Font font = new Font(fontName, Font.PLAIN, 10);
        stringWidth = (int)SVGUtilities.getStringWidth(text, font) * 100;
        return stringWidth;
    }

    public static float getStringWidthByFontSize(String text, String fontName, int fontSize) {
        float stringWidth = 0.0f;
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        stringWidth = SVGUtilities.getStringWidth(text, font);
        return stringWidth;
    }

    public static String saveFileToDisk(String filename, String content) {
        try {
            Files.createDirectories(tmpfilepath);
            Path filepath = Paths.get(tmpfilepath.toString(), filename);
            try (BufferedWriter bw = Files.newBufferedWriter(filepath)) {
                bw.write(content);
            }
            return filepath.toString();
        } catch (IOException ex) {
        logger.log(Level.SEVERE, "Can't save a file into a temporary directory {0}", tmpfilepath.toString());
        ex.printStackTrace();
        }
        return "";
    }
    
    
    public static Node syntaxHighlight(String code, String lang)  {
        try {
            if (DEBUG) {
                logger.info("Start syntaxHighlight");
            }
            
            GraalJSEngine graalEngine = GraalJSEngine.getInstance();
            String highlightedCode = graalEngine.eval(code, lang);
            
            if (highlightedCode.isEmpty()) {
                return null;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document parsed = builder.parse(new InputSource(new StringReader(highlightedCode)));
            Node node = parsed.getDocumentElement();

            if (DEBUG) {
                logger.info("End syntaxHighlight");
            }
            return node;
        }
        
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Can't highlight syntax for the string '{0}'", code);
            ex.printStackTrace();
        }
        
        return null;
    }
    
    // Get list of files on resource folder (both for IDE and jar)
    public static List<String> getResourceFolderFiles(String folder) throws URISyntaxException, IOException {
        URI uri = mn2pdf.class.getClassLoader().getResource(folder).toURI();
        List<String> files = new ArrayList<>();
        try (FileSystem fileSystem = (uri.getScheme().equals("jar") ? FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap()) : null)) {
            Path path;
            if (fileSystem != null) { // from .jar
                path = fileSystem.getPath(folder);
            } else { // from IDE
                path = Paths.get(uri);
            }
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() { 
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    files.add(folder + "/" + file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return files;
    }

    public static Calendar getCalendarDate(String dateStr) {
        Calendar cal = Calendar.getInstance();
        
        // 2017-01-01T00:00:00Z
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");      
        try {
            Date date1 = sdf1.parse(dateStr);
            cal.setTime(date1);
            return cal;
        } catch (ParseException ex) {}
        
        // 20220422T000000
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        try {
            Date date2 = sdf2.parse(dateStr);
            cal.setTime(date2);
            return cal;
        } catch (ParseException ex) {}
        
        //20180125T0121
        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyyMMdd'T'HHmm");
        try {
            Date date3 = sdf3.parse(dateStr);
            cal.setTime(date3);
            return cal;
        } catch (ParseException ex) {}
        
        // convert to simple format 20220422
        String dateStrDefault = dateStr.replaceAll("-", "").substring(0,7);
        SimpleDateFormat sdf_default = new SimpleDateFormat("yyyymmdd");
        try {
            Date dateDefault =  sdf_default.parse(dateStrDefault);
            cal.setTime(dateDefault);
            return cal;
        } catch (ParseException ex) {}
        
        return cal;
    }
    
    // D:20220422000000
    public static String getXFDFDate(String dateStr) {
        Calendar cal = getCalendarDate(dateStr);
        
        StringBuilder sb_dateXFDF = new StringBuilder();
        sb_dateXFDF.append("D:");
        SimpleDateFormat format_xfdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sb_dateXFDF.append(format_xfdf.format(cal.getTime()));
        
        return sb_dateXFDF.toString();
    }
    
    public static String readFile (String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        return content;
    }

    public static String readFile (File filePath) throws IOException {
        String content = new String(Files.readAllBytes(filePath.toPath()), StandardCharsets.UTF_8);
        return content;
    }
    
    public static String floatArrayToString(float[] a) {
        return Arrays.toString(a).replace("[","").replace("]","").replace(" ","");
    }
    
    public static String innerXml(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        return sb.toString();
    }

    public static String encodeBase64(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    public static Node parseCSS(String cssString) {
        StringBuilder sbCSSxml = new StringBuilder();
        try {
            sbCSSxml.append("<css>");
            org.w3c.css.sac.InputSource source = new org.w3c.css.sac.InputSource(new StringReader(cssString));
            CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
            CSSStyleSheet sheet = parser.parseStyleSheet(source, null, null);

            CSSRuleList rules = sheet.getCssRules();
            for (int i = 0; i < rules.getLength(); i++) {
                final CSSRule rule = rules.item(i);

                if (rule instanceof CSSStyleRule) {
                    CSSStyleRule styleRule = (CSSStyleRule) rule;
                    SelectorList selectorList = ((CSSStyleRuleImpl) rule).getSelectors();

                    //System.out.println("selector: " + styleRule.getSelectorText());
                    CSSStyleDeclaration styleDeclaration = styleRule.getStyle();
                    StringBuilder properties = new StringBuilder();
                    for (int j = 0; j < styleDeclaration.getLength(); j++) {
                        String property = styleDeclaration.item(j);
                        String value = styleDeclaration.getPropertyCSSValue(property).getCssText();
                        //System.out.println("property: " + property);
                        //System.out.println("value: " + value);
                        properties.append("<property name=\"");
                        properties.append(property);
                        properties.append("\" value=\"");
                        properties.append(value);
                        properties.append("\"/>");
                    }

                    for (int s = 0; s < selectorList.getLength(); s++) {
                        String selectorText = selectorList.item(s).toString();
                        if (selectorText.contains(".")) {
                            selectorText = selectorText.substring(selectorText.lastIndexOf(".") + 1).trim();
                        } else if (selectorText.contains(" ")) {
                            selectorText = selectorText.substring(selectorText.lastIndexOf(" ") + 1).trim();
                        }
                        //selectorText = selectorText.replaceAll("sourcecode \\.","");
                        //System.out.println("selector: " + selectorText);
                        sbCSSxml.append("<class name=\"");
                        sbCSSxml.append(selectorText);
                        sbCSSxml.append("\">");
                        sbCSSxml.append(properties);
                        sbCSSxml.append("</class>");
                    }
                }
            }
            sbCSSxml.append("</css>");

        } catch (IOException e) {
            logger.severe("CSS parsing error: " + e.toString());
            sbCSSxml.setLength(0);
            sbCSSxml.append("<css></css>");
        }
        Node node = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document parsed = builder.parse(new InputSource(new StringReader(sbCSSxml.toString())));
            node = parsed.getDocumentElement();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            logger.severe("CSS parsing error: " + e.toString());
        }
        return node;
    }

    public static String getFilenameFromPath(String filepath) {
        filepath = filepath.replace("\\", "/");
        File file = new File(filepath);
        return file.getName();
        /*
        String[] filepathComponents = filepath.split("/");

        return filepathComponents[filepathComponents.length - 1];*/
    }
   
    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException e) {
            System.out.println("nodeToString Transformer Exception: " + e.toString());
        }
        return sw.toString();
    }

}
