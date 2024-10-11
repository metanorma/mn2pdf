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

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlgraphics.xmp.XMPConstants;
import org.apache.xmlgraphics.xmp.XMPHandler;

import org.apache.fop.fo.extensions.xmp.XMPContentHandlerFactory;
import org.apache.fop.fo.extensions.xmp.XMPMetadata;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;

/**
 * ContentHandler (parser) for restoring PDF extension objects from XML.
 */
public class PDFExtensionHandler extends DefaultHandler implements ContentHandlerFactory.ObjectSource {

    /** Logger instance */
    protected static final Log log = LogFactory.getLog(PDFExtensionHandler.class);

    private PDFExtensionAttachment returnedObject;
    private ObjectBuiltListener listener;

    // PDFEmbeddedFileAttachment related state
    private Attributes lastAttributes;

    // PDFDictionaryAttachment related
    private Stack<PDFCollectionExtension> collections = new Stack<PDFCollectionExtension>();
    private boolean captureContent;
    private StringBuffer characters;
    private XMPHandler xmpHandler;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (PDFExtensionAttachment.CATEGORY.equals(uri)) {
            if (localName.equals(PDFEmbeddedFileAttachment.ELEMENT)) {
                lastAttributes = new AttributesImpl(attributes);
            } else if (PDFDictionaryType.Action.elementName().equals(localName)) {
                PDFActionExtension action = new PDFActionExtension();
                String id = attributes.getValue(PDFDictionaryElement.ATT_ID);
                if (id != null) {
                    action.setProperty(PDFDictionaryExtension.PROPERTY_ID, id);
                }
                String type = attributes.getValue(PDFActionElement.ATT_TYPE);
                if (type != null) {
                    action.setProperty(PDFActionExtension.PROPERTY_TYPE, type);
                }
                collections.push(action);
            } else if (PDFObjectType.Array.elementName().equals(localName)) {
                PDFArrayExtension array = new PDFArrayExtension();
                String key = attributes.getValue(PDFCollectionEntryElement.ATT_KEY);
                if (key != null) {
                    array.setKey(key);
                }
                collections.push(array);
            } else if (PDFDictionaryType.Catalog.elementName().equals(localName)) {
                PDFCatalogExtension catalog = new PDFCatalogExtension();
                collections.push(catalog);
            } else if (PDFDictionaryType.Dictionary.elementName().equals(localName)) {
                PDFDictionaryExtension dictionary = new PDFDictionaryExtension();
                String key = attributes.getValue(PDFCollectionEntryElement.ATT_KEY);
                if (key != null) {
                    dictionary.setKey(key);
                }
                collections.push(dictionary);
            } else if (PDFDictionaryType.Layer.elementName().equals(localName)) {
                PDFLayerExtension layer = new PDFLayerExtension();
                String id = attributes.getValue(PDFDictionaryElement.ATT_ID);
                if (id != null) {
                    layer.setProperty(PDFDictionaryExtension.PROPERTY_ID, id);
                }
                collections.push(layer);
            } else if (PDFDictionaryType.Navigator.elementName().equals(localName)) {
                PDFNavigatorExtension navigator = new PDFNavigatorExtension();
                String id = attributes.getValue(PDFDictionaryElement.ATT_ID);
                if (id != null) {
                    navigator.setProperty(PDFDictionaryExtension.PROPERTY_ID, id);
                }
                collections.push(navigator);
            } else if (PDFDictionaryType.Page.elementName().equals(localName)) {
                PDFPageExtension page = new PDFPageExtension();
                String pageNumbers = attributes.getValue(PDFPageElement.ATT_PAGE_NUMBERS);
                if (pageNumbers != null) {
                    page.setProperty(PDFPageExtension.PROPERTY_PAGE_NUMBERS, pageNumbers);
                }
                collections.push(page);
            } else if (PDFDictionaryType.Info.elementName().equals(localName)) {
                PDFDocumentInformationExtension info = new PDFDocumentInformationExtension();
                collections.push(info);
            } else if (PDFDictionaryType.VT.elementName().equals(localName)) {
                PDFVTExtension dictionary = new PDFVTExtension();
                collections.push(dictionary);
            } else if (PDFDictionaryType.PagePiece.elementName().equals(localName)) {
                PDFPagePieceExtension dictionary = new PDFPagePieceExtension();
                collections.push(dictionary);
            } else if (PDFObjectType.hasValueOfElementName(localName)) {
                PDFCollectionEntryExtension entry;
                if (PDFObjectType.Reference.elementName().equals(localName)) {
                    entry = new PDFReferenceExtension();
                } else {
                    entry = new PDFCollectionEntryExtension(PDFObjectType.valueOfElementName(localName));
                }
                String key = attributes.getValue(PDFCollectionEntryElement.ATT_KEY);
                if (key != null) {
                    entry.setKey(key);
                }
                if (entry instanceof PDFReferenceExtension) {
                    String refid = attributes.getValue(PDFReferenceElement.ATT_REFID);
                    if (refid != null) {
                        ((PDFReferenceExtension) entry).setReferenceId(refid);
                    }
                }
                if (!collections.empty()) {
                    PDFCollectionExtension collection = collections.peek();
                    collection.addEntry(entry);
                    if (!(entry instanceof PDFReferenceExtension)) {
                        captureContent = true;
                    }
                }
            } else {
                throw new SAXException("Unhandled element " + localName + " in namespace: " + uri);
            }
        } else if (XMPConstants.XMP_NAMESPACE.equals(uri) || xmpHandler != null) {
            if (xmpHandler == null) {
                xmpHandler = (XMPHandler) new XMPContentHandlerFactory().createContentHandler();
            }
            xmpHandler.startElement(uri, localName, qName, attributes);
        } else {
            log.warn("Unhandled element " + localName + " in namespace: " + uri);
        }
    }

    @Override
    public void characters(char[] data, int start, int length) throws SAXException {
        if (captureContent) {
            if (characters == null) {
                characters = new StringBuffer((length < 16) ? 16 : length);
            }
            characters.append(data, start, length);
        }
        if (xmpHandler != null) {
            xmpHandler.characters(data, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (PDFExtensionAttachment.CATEGORY.equals(uri)) {
            setExtension();
            if (PDFEmbeddedFileAttachment.ELEMENT.equals(localName)) {
                String name = lastAttributes.getValue("filename");
                String src = lastAttributes.getValue("src");
                String desc = lastAttributes.getValue("description");
                String rel = lastAttributes.getValue("afrelationship");
                String volatility = lastAttributes.getValue("volatile");
                String linkAsFileAnnotation = lastAttributes.getValue("link-as-file-annotation");
                PDFEmbeddedFileAttachment pdfEmbeddedFileAttachment = new PDFEmbeddedFileAttachment(name, src, desc);
                pdfEmbeddedFileAttachment.setRel(rel);
                pdfEmbeddedFileAttachment.setVolatile(volatility);
                pdfEmbeddedFileAttachment.setLinkAsFileAnnotation(linkAsFileAnnotation);
                this.lastAttributes = null;
                this.returnedObject = pdfEmbeddedFileAttachment;
            } else if (PDFDictionaryType.hasValueOfElementName(localName)) {
                if (!collections.empty() && (collections.peek() instanceof PDFDictionaryExtension)) {
                    PDFDictionaryExtension dictionary = (PDFDictionaryExtension) collections.pop();
                    if (!collections.empty()) {
                        PDFCollectionExtension collectionOuter = collections.peek();
                        collectionOuter.addEntry(dictionary);
                    } else if (dictionary.getDictionaryType() != PDFDictionaryType.Dictionary) {
                        this.returnedObject = new PDFDictionaryAttachment(dictionary);
                    } else {
                        throw new SAXException(
                                new IllegalStateException("generic dictionary not permitted at outer level"));
                    }
                } else {
                    throw new SAXException(new IllegalStateException("collections stack is empty or not a dictionary"));
                }
            } else if (PDFObjectType.Array.elementName().equals(localName)) {
                if (!collections.empty() && (collections.peek() instanceof PDFArrayExtension)) {
                    PDFArrayExtension array = (PDFArrayExtension) collections.pop();
                    if (!collections.empty()) {
                        PDFCollectionExtension collectionOuter = collections.peek();
                        collectionOuter.addEntry(array);
                    } else {
                        throw new SAXException(new IllegalStateException("array not permitted at outer level"));
                    }
                } else {
                    throw new SAXException(new IllegalStateException("collections stack is empty or not an array"));
                }
            } else if (PDFObjectType.hasValueOfElementName(localName)) {
                if (!collections.empty()) {
                    PDFCollectionExtension collection = collections.peek();
                    PDFCollectionEntryExtension entry = collection.getLastEntry();
                    if (entry != null) {
                        if (characters != null) {
                            entry.setValue(characters.toString());
                            characters = null;
                        }
                    } else {
                        throw new SAXException(new IllegalStateException("no current entry"));
                    }
                } else {
                    throw new SAXException(new IllegalStateException("entry not permitted at outer level"));
                }
            }
        }
        if (xmpHandler != null) {
            xmpHandler.endElement(uri, localName, qName);
        }
        captureContent = false;
    }

    private void setExtension() {
        if (xmpHandler != null) {
            PDFPageExtension pdfPageExtension = (PDFPageExtension) collections.peek();
            XMPMetadata wrapper = new XMPMetadata(xmpHandler.getMetadata());
            pdfPageExtension.setExtension(wrapper);
            xmpHandler = null;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        if (listener != null) {
            listener.notifyObjectBuilt(getObject());
        }
    }

    public Object getObject() {
        return returnedObject;
    }

    public void setObjectBuiltListener(ObjectBuiltListener listener) {
        this.listener = listener;
    }
}
