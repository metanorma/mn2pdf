package org.metanorma.fop.fonts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author Alexander Dyuzhev
 */
@JacksonXmlRootElement(localName = "font-triplet")
public class FOPFontTriplet {
    
    String substprefix = "Sans";
    String substsuffix = "Regular";
    
    @JacksonXmlProperty(isAttribute=true)
    String name;
    
    @JacksonXmlProperty(isAttribute=true)
    String style;
    
    @JacksonXmlProperty(isAttribute=true)
    String weight;

    public FOPFontTriplet() {
        
    }
    
    public FOPFontTriplet(String name, String weight, String style) {
        this.name = name;
        this.weight = weight;
        this.style = style;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getWeight() {
        return weight;
    }

    public String getWeightNumerical() {
        switch (weight) {
            case "thin":
            case "hairline":
                return "100";
            case "extra-light":
                return "200";
            case "light":
                return "300";
            case "normal":
                return "400";
            case "medium":
                return "500";
            case "semi-bold":
            case "demi-bold":
                return "600";
            case "bold":
                return "700";
            case "extra-bold":
            case "ultra-bold":
                return "800";
            case "black":
            case "heavy":
                return "900";
        }
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
    
    @JsonIgnore
    public String getFontSubstituionByDefault() {
        
        // if default fonts (will be download in default font folder)
        // no replace
        /*if (name.equals("HanSans") ||
            name.equals("STIX Two Math") ||
            name.startsWith("SourceS") ||
            name.startsWith("SourceCodePro")) {
            return "";
        }*/
        determineSubstFontPrefix(name);
        determineSubstFontSuffix(name, weight, style);
        //String fontFamilySubst = DefaultFonts.DEFAULTFONT_PREFIX + substprefix + DefaultFonts.DEFAULTFONT_SUFFIX + "-" + substsuffix;
        String fontFamilySubst = DefaultFonts.DEFAULTFONT_NOTO_PREFIX + substprefix + DefaultFonts.DEFAULTFONT_NOTO_SUFFIX + "-" + substsuffix;
        return fontFamilySubst;
    }
 
    private void determineSubstFontPrefix (String fontname) {
        substprefix = "Sans";
        if (fontname.toLowerCase().contains("arial")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("times")) {
            substprefix = "Sans";//"Serif";
        } else if (fontname.toLowerCase().contains("cambria")) {
            substprefix = "Sans";//"Serif";
        } else if (fontname.toLowerCase().contains("calibri")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("cour")) {
            //substprefix = "Code";
            substprefix = "SansMono";
        } else if (fontname.toLowerCase().contains("monospace")) {
            //substprefix = "Code";
            substprefix = "SansMono";
        } else if (fontname.toLowerCase().contains("sans")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("serif")) {
            substprefix = "Sans";//"Serif";
        }
    }
    
    private void determineSubstFontSuffix(String fontname, String fontweight, String fontstyle) {
        substsuffix = "Regular";
        String pfx = "";
        if (fontname.contains("Light") || fontname.contains("Lt")) {
            pfx = "Light";
            substsuffix = "Light";
        }
        if (fontstyle.equals("italic")) {
            if (fontweight.equals("bold")) {
                //substsuffix = "BoldIt";
                substsuffix = "BoldItalic";
                if (substprefix.equals("SansMono")) {
                    substsuffix = "Bold";
                }
            } else {
                //substsuffix = pfx + "It";
                substsuffix = pfx + "Italic";
                if (substprefix.equals("SansMono")) {
                    substsuffix = "Regular";
                }
            }
        }
        if (fontweight.equals("bold")) {
            if (fontstyle.equals("italic")) {
                //substsuffix = "BoldIt";
                substsuffix = "BoldItalic";
                if (substprefix.equals("SansMono")) {
                    substsuffix = "Bold";
                }
            } else {
                substsuffix = "Bold";
            }
        }
    }
    
}
