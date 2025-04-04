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

package org.apache.fop.area.inline;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.fop.fonts.Font;

/**
 * Always (pre-) resolved page number area. Needed by BIDI code to distinguish
 * from UnresolvedPageNumber.
 */
public class ResolvedPageNumber extends TextArea {

    private static final long serialVersionUID = -1758369835371647979L;

    //Transient fields
    transient Font font;

    private boolean isVertical;

    public ResolvedPageNumber(Font f, boolean v) {
        font = f;
        isVertical = v;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    @Override
    public void addWord(String word, int offset, int level) {
        if(isVertical) {
            for(int i=0; i<word.length(); i++) {
                addWord(word.substring(i, i+1), font.getFontSize(), null, null, null, offset, false, true);
            }
        } else
            super.addWord(word, offset, level);
    }
}
