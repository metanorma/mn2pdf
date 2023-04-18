package org.metanorma.fop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.metanorma.fop.fonts.DefaultFonts;
import org.metanorma.fop.fonts.FOPFont;
import org.metanorma.fop.fonts.FOPFontAlternate;
import org.metanorma.fop.fonts.FOPFontTriplet;
import static org.metanorma.Constants.DEBUG;
import static org.metanorma.Constants.ERROR_EXIT_CODE;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.fop.fonts.autodetect.FontFileFinder;
import static org.metanorma.fop.PDFGenerator.logger;
import org.metanorma.utils.LoggerHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;


/**
 *
 * @author Alexander Dyuzhev
 */
class fontConfig {
    
    protected static final Logger logger = Logger.getLogger(LoggerHelper.LOGGER_NAME);
    
    static final String ENV_FONT_PATH = "MN_PDF_FONT_PATH";
    static final String WARNING_FONT = "WARNING: Font file '%s' (font name '%s', font style '%s', font weight '%s') doesn't exist. Replaced by '%s'.";
    private final String CONFIG_NAME = "pdf_fonts_config.xml";
    private final String CONFIG_NAME_UPDATED = CONFIG_NAME + ".out";
    
    private final String FONT_WEIGHT_DEFAULT = "normal";
    
    private final Document FOPconfigXML;

    static final String DEFAULT_FONT_PATH = "~/.metanorma/fonts";
    
    private List<String> sourceDocumentFontList;
    private File updatedConfig;
    private String fontPath;
    private List<String> messages = new ArrayList<>();;
    
    static List<FOPFont> fopFonts = new ArrayList<>();
    
    private File fFontManifest;

    private static List<String> registeredFonts = new ArrayList<>();

    private boolean isReady = false;
    
    //private final List<String> defaultFontList = new DefaultFonts().getDefaultFonts();
    
    StringBuilder fontManifestLog = new StringBuilder();
    StringBuilder fopFontsLog = new StringBuilder();

    private String fontConfigPath = "";

    public fontConfig() {
        
        setFontPath(DEFAULT_FONT_PATH);
        
	    FOPconfigXML = getSourceFOPConfigFile();
        
        if (fopFonts.isEmpty()) {
            fopFonts = getFOPfonts();
        }
        
        //for DEBUG only 
        /*for(FOPFont font: fonts) {
            XmlMapper xmlMapper = new XmlMapper();
            try {
                String xml = xmlMapper.writeValueAsString(font);
                System.out.println(xml);
            } catch (JsonProcessingException ex) {
                System.out.println("Error!");
            }
        }*/
    }
    
    public void setFontPath(String fontPath) {
        this.fontPath = Util.fixFontPath(fontPath);
        try {
            new File(this.fontPath).mkdirs();
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
    }

    public void setFontConfigPath(String fontConfigPath) {
        this.fontConfigPath = fontConfigPath;
    }

    public void setFontManifest(File fFontManifest) {

        this.fFontManifest = fFontManifest;
        /* Example expected format:
        Cambria:
            Regular:
            - "/Users/user/.fontist/fonts/CAMBRIA.TTC"
        Cambria Math:
            Regular:
            - "/Users/user/.fontist/fonts/CAMBRIA.TTC"
        */

        if (fFontManifest != null) {
            Yaml yaml = new Yaml();
            Map<String, Object> obj;
            try {
                obj = yaml.load(new FileInputStream(fFontManifest));
                //DEBUG
                //System.out.println(obj);
                for (Map.Entry<String, Object> entry : obj.entrySet()) {

                    String entryFontName = entry.getKey();
                    
                    
                    FOPFontTriplet fopFontTripletVariant1 = new FOPFontTriplet();
                    fopFontTripletVariant1.setName(entryFontName);
                    
                    FOPFontTriplet fopFontTripletDefault = getFontDefaultProperties(entryFontName);
                    
                    FOPFontTriplet fopFontTripletVariant2 = null;
                    if (!(fopFontTripletDefault.getName().equals(entryFontName)) || !(fopFontTripletDefault.getWeight().equals(FONT_WEIGHT_DEFAULT))) {
                        fopFontTripletVariant2 = fopFontTripletDefault;
                    }
                    
                    
                    Map<String, Object> fontEntries = (Map<String, Object>) entry.getValue();
                    
                    for (Map.Entry<String, Object> fontEntry : fontEntries.entrySet()) {
                    
                        // Regular, Bold Italic, etc.
                        Map<String,String> fontStyles = getFontStyles(fontEntry.getKey());
                        
                        fopFontTripletVariant1.setWeight(fontStyles.get("weight"));
                        fopFontTripletVariant1.setStyle(fontStyles.get("style"));
                        
                        List<FOPFontTriplet> fontVariants = new ArrayList<>();
                        fontVariants.add(fopFontTripletVariant1);
                        if (fopFontTripletVariant2 != null) {
                            fopFontTripletVariant2.setStyle(fontStyles.get("style"));
                            fontVariants.add(fopFontTripletVariant2);
                        }
                        
                        Map<String, Object> fontNamePathsEntries = (Map<String, Object>) fontEntry.getValue();
                        
                        String fontFullName = (String) fontNamePathsEntries.get("full_name");
 
                        //for(String fontPath : (List<String>)fontEntry.getValue()) {
                        for(String fontPath : (List<String>)fontNamePathsEntries.get("paths")) {

                            String fontPath_ = Util.fixFontPath(fontPath);
                            if (new File(fontPath_).exists()) {

                                for (FOPFontTriplet fontVariant: fontVariants) {
                                    final String fontName = fontVariant.getName();
                                    final String fontWeight = fontVariant.getWeight();
                                    final String fontStyle = fontVariant.getStyle();
                                
                                    List<FOPFont> fopFontsByNameWeightStyle = fopFonts.stream()
                                        .filter(fopFont -> !fopFont.isReadyToUse())
                                        .filter(fopFont -> fopFont.contains(fontName, fontWeight, fontStyle))                            
                                        .collect(Collectors.toList());

                                    if (fopFontsByNameWeightStyle.isEmpty()) { // create a new font entry in fopFonts array
                                        if (DEBUG) {
                                            //System.out.println("Create a new font entry: " + fontPath_ + " (" + fontName + " " + fontWeight + " " + fontStyle + ")");
                                            fontManifestLog.append("Create a new font entry: " + fontPath_ + " (" + fontName + ", font-weight='" + fontWeight + "', font-style='" + fontStyle + "')").append("\n");
                                        }
                                        FOPFontTriplet fopFontTriplet = new FOPFontTriplet(fontName, fontWeight, fontStyle);
                                        
                                        List<FOPFontTriplet> fopFontTriplets = new ArrayList<>();
                                        fopFontTriplets.add(fopFontTriplet);

                                        FOPFont newFOPFont = new FOPFont();
                                        newFOPFont.setEmbed_url(fontPath_);
                                        if (fontPath_.toLowerCase().endsWith(".ttc")) {
                                            //newFOPFont.setSub_font(fontName);
                                            newFOPFont.setSub_font(fontFullName);
                                        }
                                        newFOPFont.setReadyToUse(true);
                                        newFOPFont.setSource("manifest");
                                        newFOPFont.setFont_triplet(fopFontTriplets);

                                        fopFonts.add(newFOPFont);

                                        
                                        // set embed-url path for fonts with simulate-style="true" and similar sub-font
                                        fopFonts.stream()
                                            .filter(f -> !f.isReadyToUse())
                                            .filter(f -> f.getSimulate_style() != null && f.getSimulate_style().equals("true"))
                                            .filter(f -> f.getSub_font() != null && fontFullName.toLowerCase().equals(f.getSub_font().toLowerCase()))
                                            .forEach(f -> {
                                                f.setEmbed_url(fontPath_);
                                            });
                                        
                                    } else { //if there is font in array
                                        if (DEBUG) {
                                            //System.out.println("Update font entry: " + fontName + " to " + fontPath_);
                                            fontManifestLog.append("Update font entry: " + fontName + " to " + fontPath_).append("\n");
                                        }
                                        fopFontsByNameWeightStyle.stream()
                                                .forEach(f -> {
                                                    f.setEmbed_url(fontPath_);
                                                    f.setReadyToUse(true);
                                                    f.setSource("manifest");
                                                });

                                        // change sub-font for ttc fonts
                                        if (fontPath_.toLowerCase().endsWith(".ttc")) {
                                            fopFontsByNameWeightStyle.stream()
                                                //.filter(f -> !fontPath_.toLowerCase().contains(f.getEmbed_url().toLowerCase())) // in case if file names in embed-url and in manifest file are different
                                                //.forEach(f -> f.setSub_font(fontName));
                                                .forEach(f -> f.setSub_font(fontFullName));
                                        }

                                        //List<FOPFont> fopFontsWithSimulateStyleByName
                                        // set embed-url path for fonts with simulate-style="true" and similar font filename
                                        fopFonts.stream()
                                            .filter(f -> !f.isReadyToUse())
                                            .filter(f -> f.getSimulate_style() != null && f.getSimulate_style().equals("true"))
                                            .filter(f -> fontPath_.toLowerCase().contains(f.getEmbed_url().toLowerCase()))
                                            .filter(f -> f.contains(fontName))
                                            .forEach(f -> {
                                                f.setEmbed_url(fontPath_);
                                                f.setReadyToUse(true);
                                            });
                                    }
                                }
                            } else {
                                logger.log(Level.WARNING, "WARNING: font path ''{0}'' from the 'fontist' manifest file doesn''t exist!", fontPath);
                            }
                        }
                    }
                }

                // remove sub-font property, if font doesn't end with .ttc
                fopFonts.stream()
                        .filter(f -> f.isReadyToUse())
                        .filter(f -> f.getSub_font() != null)
                        .filter(f -> !f.getPath().toLowerCase().endsWith(".ttc"))
                        .forEach(f-> f.setSub_font(null));

            } catch (FileNotFoundException ex) {
                // make no sense, checking in main method
            } catch (Exception ex) {
                logger.log(Level.INFO, "ERROR: Error in processing font manifest file: {0}", ex.toString());
                logger.info("Expected format:");
                logger.info("Cambria:");
                logger.info("  Regular:");
                logger.info("    full_name: Cambria");
                logger.info("    paths:");
                logger.info("    - \"~/.fontist/fonts/CAMBRIA.TTC\"");
                logger.info("  Bold:");
                logger.info("    paths:");
                logger.info("    - \"~/.fontist/fonts/CAMBRIAB.TTF\"");
                logger.info("Cambria Math:");
                logger.info("  Regular:");
                logger.info("    paths:");
                logger.info("    - \"~/.fontist/fonts/CAMBRIA.TTC\"");
                logger.info("STIX Two Math:");
                logger.info("  Regular:");
                logger.info("    paths:");
                logger.info("    - \"~/.fontist/fonts/STIX2Math.otf\"");
                System.exit(ERROR_EXIT_CODE);
            }
        }
    }
    
    public void outputFontManifestLog(Path logPath) {
        if(DEBUG) {
            Util.outputLog(logPath, fontManifestLog.toString());
            logger.log(Level.INFO, "Font manifest reading log saved to ''{0}''.", logPath);
        }
    }
    
    public Map<String,String> getFontStyles(String style) {
        Map<String,String> fontstyle = new HashMap<>();
        
        String fontWeight = FONT_WEIGHT_DEFAULT; // default value
        String fontStyle = "normal"; // default value
        
        String fontStyle_weight = style.toLowerCase();
        String fontStyle_style = style.toLowerCase();
        
        if (style.contains(" ")) {
            String[] fontStyleParts = style.toLowerCase().split(" ");
            fontStyle_weight = fontStyleParts[0];
            fontStyle_style = fontStyleParts[1];
        }
        
        fontWeight = getFontWeight(fontStyle_weight);
        if (fontWeight.isEmpty()) {
            fontWeight = FONT_WEIGHT_DEFAULT;
        }
        
        switch (fontStyle_style) {
            case ("italic"):
                   fontStyle = "italic";
                   break;
            default: // regular
                break;
        }
        
        fontstyle.put("weight", fontWeight);
        fontstyle.put("style", fontStyle);
        
        return fontstyle;
    }
    
    private String getFontWeight(String fontStyle_weight) {
        String fontWeight = "";
        switch (fontStyle_weight.toLowerCase())  {
            case ("heavy"):
                fontWeight = "900";
                break;
            case ("black"):
                fontWeight = "900";
                break;
            case ("extrabold"):
                fontWeight = "800";
                break;
            case ("bold"):
                fontWeight = "bold";
                break;
            case ("semibold"):
                fontWeight = "600";
                break;
            case ("medium"):
                fontWeight = "500";
                break;
            case ("light"):
                fontWeight = "300";
                break;
            case ("hairline"):
                fontWeight = "200";
                break;
            case ("extralight"):
                fontWeight = "200";
                break;
            case ("thin"):
                fontWeight = "100";
                break;
            case ("normal"):
                fontWeight = FONT_WEIGHT_DEFAULT;
                break;
            default:
                break;
        }
        return fontWeight;
    }
    
    // Work Sans Black -> Work Sans and weight = 900
    public FOPFontTriplet getFontDefaultProperties(String font) {
        Map<String,String> fontproperties = new HashMap<>();
        
        String fontName = font;
        String fontWeight = FONT_WEIGHT_DEFAULT; // default value
        String fontStyle = "normal"; // default value
        
        String fontName_lc = font.toLowerCase();
        if (fontName_lc.contains(" ")) {
            fontWeight = fontName_lc.substring(fontName_lc.lastIndexOf(" ") + 1);
            fontWeight = getFontWeight(fontWeight);
            if (!fontWeight.isEmpty()){
                fontName = fontName.substring(0, fontName.lastIndexOf(" "));
            }
            if (fontWeight.isEmpty()) {
                fontWeight = FONT_WEIGHT_DEFAULT;
            }
        }
        
        FOPFontTriplet fontTriplet = new FOPFontTriplet(fontName, fontWeight, fontStyle);
        
        return fontTriplet;
    }
    
    public void setSourceDocumentFontList(List<String> sourceDocumentFontList) {

        this.sourceDocumentFontList = sourceDocumentFontList;
        
        isReady = false;
        
        for(String sourceDocumentFont: sourceDocumentFontList) {
            fopFonts.stream()
                    .filter(fopFont -> !fopFont.getFont_triplet().isEmpty())
                    .filter(fopFont -> fopFont.getName().equals(sourceDocumentFont))                    
                    .forEach(fopFont -> fopFont.setIsUsing(true));
            
           /* for (FOPFont fopFont: fopFonts) {
                if(!fopFont.getFont_triplet().isEmpty() && 
                        fopFont.getFont_triplet().get(0).getName().equals(sourceDocumentFont)) {
                    fopFont.setIsUsing(true);
                }
            }*/         
        }
    }
    
    
    private void updateConfig() throws IOException, Exception {

        if(!isReady) {

            // set file paths for fonts
            setFontsPaths();

            boolean isDefaultFontNeedToDownload =
                    messages.stream()
                    .filter(f -> f.contains("Replaced by"))
                    .filter(f -> f.contains(DefaultFonts.DEFAULTFONT_PREFIX) ||
                            f.contains(DefaultFonts.DEFAULTFONT_NOTO_PREFIX) ||
                            f.contains("STIX"))
                    .count() > 0 || 
                    sourceDocumentFontList.stream().filter(f -> f.contains("Noto")).count() > 0;
            
            //download Source/Noto family fonts and STIX2Math into fontPath
            // if there isn't manifest file
            // and in case of font replacement
            if (fFontManifest == null || isDefaultFontNeedToDownload) {
                new DefaultFonts().downloadDefaultFonts(fontPath);
            }

            //add fonts from from FOP config (except system fonts) to system available fonts            
            updateFontsForGraphicsEnvironment();
            
            updateFontsInFOPConfig(FOPconfigXML);
            
            //write updated FOP config file
            writeFOPConfigFile(FOPconfigXML);

            /*if (DEBUG) {
                Util.showAvailableAWTFonts();
            }*/
            isReady = true;
        }
    }
    
    public void outputAvailableAWTFonts(Path logPath) {
        if(DEBUG) {
            String awtFonts = Util.showAvailableAWTFonts();
            Util.outputLog(logPath, awtFonts);
            logger.log(Level.INFO, "Available AWT fonts list saved to ''{0}''.", logPath);
        }    
    }
    
    private Document getSourceFOPConfigFile()  {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream config = Util.getStreamFromResources(getClass().getClassLoader(), CONFIG_NAME);
            Document sourceFOPConfig = dBuilder.parse(config);

            return sourceFOPConfig;
        } catch (Exception ex) {
            logger.severe(ex.toString());
            return null;
        }
    }
    
    private List<FOPFont> getFOPfonts() {
        List<FOPFont> fonts = new ArrayList<>();
        
        NodeList fontNodes = FOPconfigXML.getElementsByTagName("font");
        
        //iterate each font from FOP config xml and create FOPFont  Object
        for (int i = 0; i < fontNodes.getLength(); i++) {
            Node fontNode = fontNodes.item(i);
            XmlMapper xmlMapper = new XmlMapper();

            try {
                // DEBUG String xml = xmlMapper.writeValueAsString(new FOPFont());

                FOPFont fopfont = xmlMapper.readValue(innerXml(fontNode), FOPFont.class);
                fonts.add(fopfont);
            } catch (JsonProcessingException ex) {
                logger.log(Level.SEVERE, "Error in reading font information: {0}", ex.toString());
                logger.log(Level.SEVERE, "XML fragment: {0}", innerXml(fontNode));
            }
        }

        return fonts;
    }
    
    public List<FOPFont> getUsedFonts ()
    {
        List<FOPFont> fonts = fopFonts.stream()
            .filter(fopFont -> fopFont.isUsing() || fopFont.getSource().equals("manifest"))
            .collect(Collectors.toList());
            
        return fonts;
    }
    
    
    
    // set file paths for fonts
    private void setFontsPaths() throws IOException, URISyntaxException {

        List<String> machineFontList = getMachineFonts();
        
        fopFonts.stream()
            .filter(fopFont -> !fopFont.getEmbed_url().isEmpty())
            .filter(fopFont -> !fopFont.isReadyToUse())
            .forEach(fopFont -> {
                
                if (!(new File (fopFont.getEmbed_url()).exists())){
                    
                    String msg = "";
                    String embed_url = Paths.get(fontPath, fopFont.getEmbed_url()).toString();
                    /*try {
                        embed_url = new File(embed_url).toURI().toURL().toString();
                    } catch (MalformedURLException ex) {
                        System.out.println("Can't obtain a font path: " + ex.toString());
                    }*/
                    
                    if (fopFont.isMn_default()) {
                        fopFont.setEmbed_url(embed_url);
                        fopFont.setReadyToUse(true);
                    }
                    
                    //if font is using only (skip unused font processing)
                    // skip default fonts
                    if (fopFont.isUsing() && !fopFont.isMn_default()) {
                        
                        fopFont.setEmbed_url(embed_url);
                        
                        File file_embed_url = new File (embed_url);
                        if (!file_embed_url.exists()) { // if font file doesn't exist
                            //msg = "WARNING: Font file '" + embed_url + "'";
                            //try to find system font (example for Windows - C:/Windows/fonts/)
                            String fontfilename = file_embed_url.getName();
                            String font_replacementpath = null;
                            String font_source = "";
                            for (String url: machineFontList) {
                                if (url.toLowerCase().endsWith(fontfilename.toLowerCase())) {
                                    font_replacementpath = url;
                                    font_source = "system";
                                    break;
                                }
                            }
                            // if there isn't system font, then try to find system font with alternate name
                            // Example: timesbd.ttf -> Times New Roman Bold.ttf
                            if (font_replacementpath == null) {
                                List<FOPFontAlternate> fopFontsAlternate = fopFont.getAlternate();

                                for(FOPFontAlternate fopFontAlternate : fopFontsAlternate) {
                                    if (font_replacementpath != null) {
                                        break;
                                    }
                                    for (String url: machineFontList) {
                                        if (url.toLowerCase().endsWith(fopFontAlternate.getEmbed_url().toLowerCase())) {
                                            font_replacementpath = url;
                                            font_source = "system";
                                            fopFont.setSub_font(fopFontAlternate.getSub_font());                                        
                                            fopFont.setSimulate_style(fopFontAlternate.getSimulate_style());
                                            if (fopFontAlternate.getAnother_font_family() != null && fopFontAlternate.getAnother_font_family().equals("true")) {
                                                fopFont.setMessage(String.format("WARNING: the font file '%s' is using for the font '%s' (font style '%s', font weight '%s').", fopFontAlternate.getEmbed_url() , fopFont.getName(), fopFont.getFont_triplet().get(0).getStyle(), fopFont.getFont_triplet().get(0).getWeight()));
                                            }
                                            break;
                                        }
                                    }
                                }
                            }

                            // if font didn't find on machine
                            if (font_replacementpath == null) {
                                //iterate each font-triplet
                                for(FOPFontTriplet fopFontTriplet: fopFont.getFont_triplet()) {

                                    String fontFamilySubst = fopFontTriplet.getFontSubstituionByDefault();

                                    font_replacementpath = Paths.get(fontPath, fontFamilySubst + ".ttf").toString();

                                    //printMessage(msg + " (font style '" + fontstyle + "', font weight '" + fontweight + "') doesn't exist. Replaced by '" + font_replacementpath + "'.");
                                    //printMessage(String.format(WARNING_FONT, embed_url, fopFontTriplet.getStyle(), fopFontTriplet.getWeight(), font_replacementpath));
                                    fopFont.setMessage(String.format(WARNING_FONT, embed_url, fopFontTriplet.getName(), fopFontTriplet.getStyle(), fopFontTriplet.getWeight(), font_replacementpath));

                                    /*try{
                                        font_replacementpath = new File(font_replacementpath).toURI().toURL().toString();
                                    } catch (MalformedURLException ex) {
                                        System.out.println("Can't obtain a font path: " + ex.toString());
                                    }*/
                                }
                            }
                            if (font_replacementpath != null) {
                                fopFont.setEmbed_url(font_replacementpath);
                                fopFont.setSource(font_source);
                            }
                        }
                        fopFont.setReadyToUse(true);
                    }
                }
            });
       
        StringBuilder sb = new StringBuilder();
        fopFonts.stream()
                .filter(f -> f.isUsing())
                .forEach(f -> {
                    String msg = f.getMessages();
                    if (!msg.isEmpty()) {
                        sb.append(msg);
                    }
                });
        printMessage(sb.toString());
    }

    private List<String> getMachineFonts() throws IOException{

        List<URL> systemFontListURL = new ArrayList<>();
        List<URL> userFontListURL = new ArrayList<>();
        
        FontFileFinder fontFileFinder = new FontFileFinder(null);
        
        if(new File(fontPath).exists()) {
            userFontListURL =  fontFileFinder.find(fontPath);
        }
        systemFontListURL = fontFileFinder.find();
        
        userFontListURL.addAll(systemFontListURL);
        
        // %20 to space character replacement
        List<String> machineFontList =  new ArrayList<>();
        for(URL url: userFontListURL){
            machineFontList.add(URLDecoder.decode(url.toString(), StandardCharsets.UTF_8.name()));
        }

        return machineFontList;
    }
    
    private void updateFontsInFOPConfig(Document xmlDocument) {

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "/fop/renderers/renderer/fonts";
        try {
            Node nodeFonts = (Node) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
            // remove all tags 'font'
            removeChilds(nodeFonts);
            
            // add 'font' from fopFonts array
            for(FOPFont fopFont: fopFonts) {
                
                String embed_url = fopFont.getEmbed_url();
                String embed_url_updated = embed_url;
                try {
                    if (!embed_url.startsWith("file:")) {
                        // add file: prefix and update xml attribute embed-url
                        embed_url_updated = new File(embed_url).toURI().toURL().toString();
                    }
                }
                catch (MalformedURLException ex) { }
                
                fopFont.setEmbed_url(embed_url_updated);
                
                try {
                    String fopFontString = new XmlMapper().writeValueAsString(fopFont);
                    //restore path
                    fopFont.setEmbed_url(embed_url);
                    if (DEBUG) {
                        //System.out.println("DEBUG: FOP config font entry:");
                        //System.out.println(fopFontString);
                        fopFontsLog.append(fopFontString).append("\n");
                    }
                    Node newNodeFont =  DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(fopFontString.getBytes()))
                        .getDocumentElement();
                    //newNodeFont.getAttributes().getNamedItem("embed-url").setTextContent(embed_url);
           
                    Document doc = nodeFonts.getOwnerDocument();
                    newNodeFont = doc.importNode(newNodeFont, true);
                    nodeFonts.appendChild(newNodeFont);
                    
                    /*if (fopFont.isUsing()) {
                        String msg = fopFont.getMessages();
                        if(!msg.isEmpty()) {
                            System.out.println();
                        }
                    }*/
                    
                } catch (SAXException | IOException | ParserConfigurationException ex) {
                    logger.log(Level.SEVERE, "Error in FOP font xml processing: {0}", ex.toString());
                }
            }
        } catch (XPathExpressionException ex) {
            logger.severe(ex.toString());
        }
    }
    
    public void outputFOPFontsLog(Path logPath) {
        if(DEBUG) {
            Util.outputLog(logPath, fopFontsLog.toString());
            logger.log(Level.INFO, "FOP fonts config items saved to ''{0}''.", logPath);
        }    
    }
    
    private void removeChilds(Node node) {
        while (node.hasChildNodes())
        {
            node.removeChild(node.getFirstChild());
        }
    }
    
    private void writeFOPConfigFile(Document xmlDocument) throws IOException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();

            StringWriter writer = new StringWriter();

            //transform document to string 
            transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));

            String xmlString = writer.getBuffer().toString();   
            
            //System.out.println(xmlString);
            Path updateConfigPath = (this.fontConfigPath.isEmpty()) ? Paths.get(this.fontPath, CONFIG_NAME_UPDATED) : Paths.get(this.fontConfigPath + "." + CONFIG_NAME_UPDATED);
            updatedConfig = updateConfigPath.toFile();
            try (BufferedWriter bw = Files.newBufferedWriter(updateConfigPath)) 
            {
                bw.write(xmlString);
            }
        } 
        catch (TransformerException e) 
        {
            e.printStackTrace();
        }        
    }

    // get updated config file name
    public File getConfig() throws Exception {
        
        updateConfig();
        
        return updatedConfig;
    }

    public void deleteConfigFile() {
        if (!this.fontConfigPath.isEmpty()) {
            if (updatedConfig != null) {
                try {
                    Files.deleteIfExists(updatedConfig.toPath());
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public void setPDFUAmode(String mode) throws SAXException, IOException, ParserConfigurationException{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();        
        Document configXML = dBuilder.parse(updatedConfig);
        NodeList pdfuamodelist = configXML.getElementsByTagName("pdf-ua-mode");
        if (pdfuamodelist != null) {
            Node pdfuamode = pdfuamodelist.item(0);
            pdfuamode.setTextContent(mode);
        }
        writeFOPConfigFile(configXML);
    }
    
    
    
    private void updateFontsForGraphicsEnvironment(){

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        try (Stream<Path> walk = Files.walk(Paths.get(this.fontPath))) {
                List<String> fontfiles = walk.map(x -> x.toString())
                                .filter(f -> f.endsWith(".ttf") || f.endsWith(".TTF") || f.endsWith(".ttc") || f.endsWith(".TTC") || f.endsWith(".otf") || f.endsWith(".OTF")).collect(Collectors.toList());
                
                // add used fonts (except system)
                getUsedFonts().stream()
                        .filter(f -> !f.getSource().equals("system"))
                        .forEach(f -> fontfiles.add(f.getEmbed_url()));
                
                fontfiles.forEach(fontfile -> {
                    registerFont(ge, fontfile);
            });                
        } catch (IOException e) {
                e.printStackTrace();
        }
    }
    
    public static void registerFont(GraphicsEnvironment ge, String fontFile){
        if (!registeredFonts.contains(fontFile)) {

            if (ge == null) {
                ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            }
            try {
                // try to obtain font to check an exception:
                // java.lang.NullPointerException: Cannot load from short array because "sun.awt.FontConfiguration.head" is null
                String[] names = ge.getAvailableFontFamilyNames();
                int count = names.length;
            } catch (Exception e) {
                logger.severe("[ERROR] " + e.toString());
                logger.severe("[ERROR] fontconfig not found or no one system font not installed.");
                System.exit(ERROR_EXIT_CODE);
            }
            try {
                Font ttfFont = Font.createFont(Font.TRUETYPE_FONT, new File(fontFile));
                //register the font
                ge.registerFont(ttfFont);
                registeredFonts.add(fontFile);
            } catch(FontFormatException e) {
                try {
                    Font type1Font = Font.createFont(Font.TYPE1_FONT, new File(fontFile));
                    //register the font
                    ge.registerFont(type1Font);
                    registeredFonts.add(fontFile);
                } catch(FontFormatException e1) {
                    e1.printStackTrace();
                }  catch (IOException e2) {
                    e2.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void printMessage(String msg) {
        if (!msg.isEmpty()) {
            if (!messages.contains(msg)) {
                logger.warning(msg);
                messages.add(msg);
            }
        }
    }

    public List<String> getMessages() {
        return messages;
    }

    public void printMessages() {
        for(String msg: getMessages()) {
            logger.warning(msg);
        }
    }

    private String innerXml(Node node) {
       /* DOMImplementationLS lsImpl = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
           sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        return sb.toString(); 
        */
       ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            Source source = new DOMSource(node);
            Result target = new StreamResult(out);       
            transformer.transform(source, target);
        } catch (TransformerException ex) {}

        return out.toString();
    }

}
