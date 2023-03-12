package org.metanorma.fop.fonts;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 *
 * @author Alexander Dyuzhev
 */
@JacksonXmlRootElement(localName = "alternate")
public class FOPFontAlternate {
   
    @JacksonXmlProperty(isAttribute=true, localName = "embed-url")
    String embed_url;
    
    @JacksonXmlProperty(isAttribute=true, localName = "sub-font")
    String sub_font;
    
    @JacksonXmlProperty(isAttribute=true, localName = "simulate-style")
    String simulate_style;

    @JacksonXmlProperty(isAttribute=true, localName = "another-font-family")
    String another_font_family;

    public String getEmbed_url() {
        return embed_url;
    }

    public void setEmbed_url(String embed_url) {
        this.embed_url = embed_url;
    }

    public String getSub_font() {
        return sub_font;
    }

    public void setSub_font(String sub_font) {
        this.sub_font = sub_font;
    }

    public String getSimulate_style() {
        return simulate_style;
    }

    public void setSimulate_style(String simulate_style) {
        this.simulate_style = simulate_style;
    }

    public String getAnother_font_family() {
        return another_font_family;
    }

    public void setAnother_font_family(String another_font_family) {
        this.another_font_family = another_font_family;
    }
}
