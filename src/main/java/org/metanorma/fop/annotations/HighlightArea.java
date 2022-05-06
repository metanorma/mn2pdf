package org.metanorma.fop.annotations;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 *
 * @author Alexander Dyuzhev
 */
public class HighlightArea {
    
    private float quadPoints[] = {};
    
    private PDRectangle position = new PDRectangle();
    
    public HighlightArea() {
        
    }

    public float[] getQuadPoints() {
        return quadPoints;
    }
    
    public void setQuadPoints(float[] quadPoints) {
        this.quadPoints = quadPoints;
    }

    public PDRectangle getPosition() {
        return position;
    }

    public void setPosition(PDRectangle position) {
        this.position = position;
    }
    
}
