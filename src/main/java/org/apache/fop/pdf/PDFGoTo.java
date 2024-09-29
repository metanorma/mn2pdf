/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.pdf;

import java.awt.geom.Point2D;

/**
 * class representing a /GoTo object.
 * This can either have a Goto to a page reference and location
 * or to a specified PDF reference string.
 */
public class PDFGoTo extends PDFAction {

    /**
     * the pageReference
     */
    private PDFReference pageReference;
    private String destination;
    private float xPosition;
    private float yPosition;
    private boolean isNamedDestination;

    /**
     * create a /GoTo object.
     *
     * @param destination name of the destination
     * @param isNamedDestination set to true if the destination is a named destination
     */
    public PDFGoTo(String destination, boolean isNamedDestination) {
        super();
        this.destination = destination;
        this.isNamedDestination = isNamedDestination;
    }

    /**
     * create a /GoTo object.
     *
     * @param pageReference the pageReference represented by this object
     */
    public PDFGoTo(String pageReference) {
        super();
        if (pageReference != null) {
            setPageReference(new PDFReference(pageReference));
        }
    }

    /**
     * create a /GoTo object.
     *
     * @param pageReference the PDF reference to the target page
     * @param position the target area's on-page coordinates in points
     */
    public PDFGoTo(String pageReference, Point2D position) {
        /* generic creation of object */
        this(pageReference);
        setPosition(position);
    }

    /**
     * Sets page reference after object has been created
     *
     * @param pageReference the new page reference to use
     */
    public void setPageReference(PDFReference pageReference) {
        this.pageReference = pageReference;
    }

    /**
     * Sets the target (X,Y) position
     *
     * @param position the target's on-page coordinates in points
     */
    public void setPosition(Point2D position) {
        this.xPosition = (float) position.getX();
        this.yPosition = (float) position.getY();
    }

    /**
     * Sets the x Position to jump to
     *
     * @param xPosition x position
     */
    public void setXPosition(float xPosition) {
        this.xPosition = xPosition;
    }

    /**
     * Sets the Y position to jump to
     *
     * @param yPosition y position
     */
    public void setYPosition(float yPosition) {
        this.yPosition = yPosition;
    }

    /**
     * Set the destination string for this Goto.
     *
     * @param dest the PDF destination string
     */
    public void setDestination(String dest) {
        destination = dest;
    }

    /**
     * Get the PDF reference for the GoTo action.
     *
     * @return the PDF reference for the action
     */
    public String getAction() {
        return referencePDF();
    }

    /**
     * {@inheritDoc}
     */
    public String toPDFString() {
        String dest;
        if (destination == null) {
            dest = "/D [" + this.pageReference + " /XYZ " + xPosition
                          + " " + yPosition + " null]\n";
        } else {
            dest = "/D [" + this.pageReference + " " + destination + "]\n";
            if (this.isNamedDestination) {
                dest = "/D (" + this.destination + ")\n";
             } else {
                dest = "/D [" + this.pageReference + " " + destination + "]\n";
             }
        }
        return "<< /Type /Action\n/S /GoTo\n" + dest + ">>";
    }

    /*
     * example
     * 29 0 obj
     * <<
     * /S /GoTo
     * /D [23 0 R /FitH 600]
     * >>
     * endobj
     */

    /** {@inheritDoc} */
    protected boolean contentEquals(PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFGoTo)) {
            return false;
        }

        PDFGoTo gt = (PDFGoTo)obj;

        if (gt.pageReference == null) {
            if (pageReference != null) {
                return false;
            }
        } else {
            if (!gt.pageReference.equals(pageReference)) {
                return false;
            }
        }

        if (destination == null) {
            if (!(gt.destination == null && gt.xPosition == xPosition
                && gt.yPosition == yPosition)) {
                return false;
            }
        } else {
            if (!destination.equals(gt.destination)) {
                return false;
            }
        }

        return (isNamedDestination == gt.isNamedDestination);
    }
}

