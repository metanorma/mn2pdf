package org.metanorma.fop;

import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.util.ArrayList;
import java.util.List;

public class PDFHiddenTextStripper extends PDFTextStripper {

    List<String> transparentItems = new ArrayList<>();

    /**
     * Override the default functionality of PDFTextStripper.processTextPosition()
     */
    @Override
    protected void processTextPosition(TextPosition text) {

        PDGraphicsState state = getGraphicsState();

        // Check alpha constant (0.0 is fully transparent, 1.0 is opaque)
        double alpha = state.getNonStrokeAlphaConstant();

        // Check Rendering Mode (3 is invisible)
        int renderingMode = state.getTextState().getRenderingMode().intValue();

        // alpha == 0 ||
        if (renderingMode == 3) {
            // Text is transparent/invisible
            /*
            System.out.println("Transparent text: " + text.toString());
            System.out.println("alpha: " + alpha);
            System.out.println("renderingMode: " + renderingMode);
            */
            transparentItems.add(text.toString());
        }
        super.processTextPosition(text);
    }

    public String getTransparentText() {
        return String.join("", transparentItems);
    }
}