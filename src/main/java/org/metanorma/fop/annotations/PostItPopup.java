package org.metanorma.fop.annotations;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 *
 * @author Alexander Dyuzhev
 */
public class PostItPopup {
    
    private PDRectangle position = new PDRectangle();
    
    public PostItPopup () {
        
    }

    public PDRectangle getPosition() {
        return position;
    }

    public void setPosition(PDRectangle position) {
        this.position = position;
    }
 
}
