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

/**
 * Class representing a /FileSpec object.
 */
public class PDFFileSpec extends PDFDictionary {

    private String linkAsFileAnnotation = "false";

    /**
     * create a /FileSpec object.
     *
     * @param filename the filename represented by this object
     */
    public PDFFileSpec(String filename) {
        this(filename, filename);
    }

    /**
     * create a /FileSpec object.
     *
     * @param filename the filename represented by this object
     * @param unicodeFilename the unicode filename represented by this object
     */
    public PDFFileSpec(String filename, String unicodeFilename) {
        put("Type", new PDFName("Filespec"));
        put("F", filename);
        put("UF", unicodeFilename); // for non-ascii filenames, since PDF 1.7, 3.10.2
    }

    /**
     * Gets the filename.
     * @return filename
     */
    public String getFilename() {
        return (String)get("F");
    }

    /**
     * Gets the unicode filename
     * @return unicode filename
     */
    public String getUnicodeFilename() {
        return (String)get("UF");
    }

    /**
     * Associates an dictionary with pointers to embedded file streams with this file spec.
     * @param embeddedFileDict the dictionary with pointers to embedded file streams
     */
    public void setEmbeddedFile(PDFDictionary embeddedFileDict) {
        put("EF", embeddedFileDict);
    }

    /**
     * Sets a description for the file spec.
     * @param description the description
     * @since PDF 1.6
     */
    public void setDescription(String description) {
        put("Desc", description);
    }

    /**
     * Sets a AFRelationship for the file spec.
     * @param relationship the AFRelationship
     */
    public void setAFRelationship(String relationship) {
        put("AFRelationship", new PDFName(relationship));
    }

    /**
     * Sets a valatility for the file spec.
     * //@param volatilily the Volatility
     */
    public void setVolatile(String volatilily) {
        if (volatilily.equals("true")) {
            put("V", true);
        }
    }

    /**
     * Sets a linkAsFileAnnotation for the file spec.
     * @param linkAsFileAnnotation the indication of file attachment annotation
     */
    public void setLinkAsFileAnnotation(String linkAsFileAnnotation) {
        this.linkAsFileAnnotation = linkAsFileAnnotation;
    }

    /**
     * Gets the linkAsFileAnnotation.
     * @return linkAsFileAnnotation
     */
    public String getLinkAsFileAnnotation() {
        return linkAsFileAnnotation;
    }

    /** {@inheritDoc} */
    protected boolean contentEquals(PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFFileSpec)) {
            return false;
        }

        PDFFileSpec spec = (PDFFileSpec)obj;

        if (!spec.getFilename().equals(getFilename())) {
            return false;
        }

        return true;
    }
}
