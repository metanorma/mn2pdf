package org.metanorma.fop.annotations;

/**
 *
 * @author Alexander Dyuzhev
 */
public class AnnotationArea {
    
    private float quadPoints[] = {};
    
    private float position[] = {};
    
    public AnnotationArea() {
        
    }

    public float[] getQuadPoints() {
        return quadPoints;
    }
    
    public void setQuadPoints(float[] quadPoints) {
        this.quadPoints = quadPoints;
    }

    public float[] getPosition() {
        return position;
    }

    public void setPosition(float[] position) {
        this.position = position;
    }
    
}
