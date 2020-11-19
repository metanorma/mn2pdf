package com.metanorma.fop.fonts;

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
    
    
}
