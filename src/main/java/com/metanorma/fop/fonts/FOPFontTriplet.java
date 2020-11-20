package com.metanorma.fop.fonts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author Alexander Dyuzhev
 */
@JacksonXmlRootElement(localName = "font-triplet")
public class FOPFontTriplet {
    
    @JacksonXmlProperty(isAttribute=true)
    String name;
    
    @JacksonXmlProperty(isAttribute=true)
    String style;
    
    @JacksonXmlProperty(isAttribute=true)
    String weight;

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

    public void setWeight(String weight) {
        this.weight = weight;
    }
    
    @JsonIgnore
    public String getFontSubstituionByDefault() {
        String substprefix = getSubstFontPrefix(name);
        String substsuffix = getSubstFontSuffix(name, weight, style);
        String fontFamilySubst = DefaultFonts.DEFAULTFONT_PREFIX + substprefix + DefaultFonts.DEFAULTFONT_SUFFIX + "-" + substsuffix;
        return fontFamilySubst;
    }
 
    private String getSubstFontPrefix (String fontname) {
        String substprefix = "Sans";
        if (fontname.toLowerCase().contains("arial")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("times")) {
            substprefix = "Sans";//"Serif";
        } else if (fontname.toLowerCase().contains("cambria")) {
            substprefix = "Sans";//"Serif";
        } else if (fontname.toLowerCase().contains("calibri")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("cour")) {
            substprefix = "Code";
        } else if (fontname.toLowerCase().contains("sans")) {
            substprefix = "Sans";
        } else if (fontname.toLowerCase().contains("serif")) {
            substprefix = "Sans";//"Serif";
        }
        return substprefix;
    }
    
    private String getSubstFontSuffix(String fontname, String fontweight, String fontstyle) {
        String substsuffix = "Regular";
        String pfx = "";
        if (fontname.contains("Light")) {
            pfx = "Light";
            substsuffix = "Light";
        }
        if (fontstyle.equals("italic")) {
            if (fontweight.equals("bold")) {
                substsuffix = "BoldIt";
            } else {
                substsuffix = pfx + "It";
            }
        }
        if (fontweight.equals("bold")) {
            if (fontstyle.equals("italic")) {
                substsuffix = "BoldIt";
            } else {
                substsuffix = "Bold";
            }
        }
        return substsuffix;
    }
    
}
