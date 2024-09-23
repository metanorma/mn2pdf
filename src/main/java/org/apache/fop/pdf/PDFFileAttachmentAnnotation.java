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
 * PDF annotation for /FileAttachment
 * @since PDF 1.3
 */
public class PDFFileAttachmentAnnotation extends PDFAction {

    private String objId;

    private PDFFileSpec fileSpec;

    /**
     * Creates a new /Launch action.
     * @param fileSpec the embedded file object
     */
    public PDFFileAttachmentAnnotation(PDFFileSpec fileSpec) {
        this.objId = fileSpec.getObjectID();
        this.fileSpec = fileSpec;
    }

    /** {@inheritDoc} */
    public String getAction() {
        return this.referencePDF();
    }

    public String getFileAttachmentAnnotation() {
        StringBuilder sb = new StringBuilder();
        String annotationDesc = fileSpec.get("Desc").toString();
        String annotationF = fileSpec.get("F").toString();
        String relationship = fileSpec.get("AFRelationship").toString();
        String objNumber = fileSpec.referencePDF();  //  "7 0 R"; Todo
        /*sb.append("/FileAttachment\n" + "/FS " + //this.referencePDF() + "\n"
                "<<\n" +
                "/EF << /F " + objNumber +" >> /F (" + annotationF + ") /Type /F\n" +
                "/AFRelationship " + relationship + "\n" +
                ">>\n" +
                "/Contents (" + annotationDesc + ")\n" +
                "/Name /Paperclip\n" +
                "\n");*/
        sb.append("/FileAttachment\n" +
                "/FS " + objNumber + "\n" +
                "/Contents (" + annotationDesc + ")\n" +
                "/Name /Paperclip\n" +
                "\n");

        return sb.toString();
    }

    /** {@inheritDoc} */
    public String toPDFString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append("<<\n/EF << /F ");
        //sb.append(objId);
        sb.append("77 0 R");
        sb.append(" >> /F (_site/documents/_test_attachments.tc4_attachments/document.presentation.pdf) /Type /F");
        //sb.append(" /Desc ([NO INFORMATION AVAILABLE])");
        sb.append("\n>>");
        return sb.toString();
    }
    /** {@inheritDoc} */
    protected boolean contentEquals(PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFJavaScriptLaunchAction)) {
            return false;
        }

        PDFFileAttachmentAnnotation launch = (PDFFileAttachmentAnnotation) obj;

        if (!launch.objId.equals(objId  )) {
            return false;
        }

        return true;
    }

}
