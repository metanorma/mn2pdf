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

package org.apache.fop.render.pdf.extensions;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.fop.pdf.PDFText;

/**
 * This is the pass-through value object for the PDF extension.
 */
public class PDFEmbeddedFileAttachment extends PDFExtensionAttachment {

    private static final long serialVersionUID = -1L;

    /** element name */
    protected static final String ELEMENT = "embedded-file";

    /** name of file to be embedded */
    private static final String ATT_NAME = "filename";

    /** source of file to be embedded (URI) */
    private static final String ATT_SRC = "src";

    /** a description of the file to be embedded */
    private static final String ATT_DESC = "description";

    /** A relationship between
     the component of this PDF document that refers to this file specification and
     the associated file denoted by this file specification dictionary */
    private static final String ATT_REL = "afrelationship";

    /**  A flag indicating whether the file referenced by the file
     specification is volatile (changes frequently with time). */
    private static final String ATT_VOLATILE = "volatile";


    /** An indication how to process link to the embedded file */
    private static final String ATT_LINKASFILEANNOTATION = "linkasfileannotation";

    /** filename attribute */
    private String filename;

    /** unicode filename attribute */
    private String unicodeFilename;

    /** description attribute (optional) */
    private String desc;

    /** source name attribute */
    private String src;

    /** associated file relationship */
    private String rel;

    /** associated file volatility */
    private String volatility;

    /** add link as file annotation */
    private String linkAsFileAnnotation;

    /**
     * No-argument contructor.
     */
    public PDFEmbeddedFileAttachment() {
        super();
    }

    /**
     * Default constructor.
     * @param filename the name of the file
     * @param src the location of the file
     * @param desc the description of the file
     */
    public PDFEmbeddedFileAttachment(String filename, String src, String desc) {
        super();
        this.setFilename(filename);
        this.src = src;
        this.desc = desc;
    }

    public PDFEmbeddedFileAttachment(String filename, String src, String desc, String rel) {
        super();
        this.setFilename(filename);
        this.src = src;
        this.desc = desc;
        this.rel = rel;
    }

    /**
     * Returns the file name.
     * @return the file name
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the unicode file name.
     * @return the file name
     */
    public String getUnicodeFilename() {
        return unicodeFilename;
    }

    /**
     * Sets the file name.
     * @param name The file name to set.
     */
    public void setFilename(String name) {
        if (!PDFText.toPDFString(name).equals(name)) {
            // replace with auto generated filename, because non-ascii chars are used.
            this.filename = "att" + name.hashCode();
        } else {
            this.filename = name;
        }
        this.unicodeFilename = name;
    }

    /**
     * Returns the file description.
     * @return the description
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Sets the description of the file.
     * @param desc the description to set
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * Returns the source URI of the file.
     * @return the source URI
     */
    public String getSrc() {
        return src;
    }

    /**
     * Sets the source URI of the file.
     * @param src the source URI
     */
    public void setSrc(String src) {
        this.src = src;
    }

    /**
     * Returns the relationship of the file.
     * @return the AFRelationship
     */
    public String getRel() {
        return rel;
    }

    /**
     * Sets the relationship of the file.
     * @param rel the AFRelationship
     */
    public void setRel(String rel) {
        this.rel = rel;
    }

    /**
     * Returns the volatility of the file.
     * @return the volatile flag
     */
    public String getVolatile() {
        return volatility;
    }

    /**
     * Sets the volatility of the file.
     * @param volatility the AFRelationship
     */
    public void setVolatile(String volatility) {
        this.volatility = volatility;
    }

    /**
     * Returns the indication of link as file annotation.
     * @return the linkAsFileAnnotation
     */
    public String getLinkAsFileAnnotation() {
        return linkAsFileAnnotation;
    }

    /**
     * Sets the indication of link as file annotation.
     * @param linkAsFileAnnotation the indication
     */
    public void setLinkAsFileAnnotation(String linkAsFileAnnotation) {
        this.linkAsFileAnnotation = linkAsFileAnnotation;
    }

    /** {@inheritDoc} */
    public String getCategory() {
        return CATEGORY;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "PDFEmbeddedFile(name=" + getFilename() + ", " + getSrc() + ")";
    }

    /**
     * @return the element name
     */
    protected String getElement() {
        return ELEMENT;
    }

    /** {@inheritDoc} */
    public void toSAX(ContentHandler handler) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        if (filename != null && filename.length() > 0) {
            atts.addAttribute("", ATT_NAME, ATT_NAME, "CDATA", filename);
        }
        if (src != null && src.length() > 0) {
            atts.addAttribute("", ATT_SRC, ATT_SRC, "CDATA", src);
        }
        if (desc != null && desc.length() > 0) {
            atts.addAttribute("", ATT_DESC, ATT_DESC, "CDATA", desc);
        }
        if (rel != null && rel.length() > 0) {
            atts.addAttribute("", ATT_REL, ATT_REL, "CDATA", rel);
        }
        if (volatility != null && volatility.length() > 0) {
            atts.addAttribute("", ATT_VOLATILE, ATT_VOLATILE, "CDATA", volatility);
        }
        if (linkAsFileAnnotation != null && linkAsFileAnnotation.length() > 0) {
            atts.addAttribute("", ATT_LINKASFILEANNOTATION, ATT_LINKASFILEANNOTATION, "CDATA", linkAsFileAnnotation);
        }

        String element = getElement();
        handler.startElement(CATEGORY, element, element, atts);
        handler.endElement(CATEGORY, element, element);
    }

}
