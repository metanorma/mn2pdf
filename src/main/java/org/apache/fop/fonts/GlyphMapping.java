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

/* $Id: GlyphMapping.java 1900111 2022-04-21 13:48:07Z ssteiner $ */

package org.apache.fop.fonts;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.complexscripts.fonts.GlyphPositioningTable;
import org.apache.fop.complexscripts.fonts.GlyphTable;
import org.apache.fop.complexscripts.util.CharScript;
import org.apache.fop.complexscripts.util.Characters;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.CharUtilities;

// import static org.apache.fop.fonts.type1.AdobeStandardEncoding.i;

/**
 * Stores the mapping of a text fragment to glyphs, along with various information.
 */
public class GlyphMapping {

    private static final Log LOG = LogFactory.getLog(GlyphMapping.class);
    /** Inclusive. */
    public final int startIndex;
    /** Exclusive. */
    public final int endIndex;
    private int wordCharLength;
    public final int wordSpaceCount;
    public int letterSpaceCount;
    public MinOptMax areaIPD;
    public final boolean isHyphenated;
    public final boolean isSpace;
    public final boolean isZeroWidthSpace; // to check for zero-width space (otherwise we get redundant spaces in copy-paste text from PDF)
    public boolean breakOppAfter;
    public final Font font;
    public final int level;
    public final int[][] gposAdjustments;
    public String mapping;
    public List associations;
    public boolean isUpright;

    public GlyphMapping(int startIndex, int endIndex, int wordSpaceCount, int letterSpaceCount,
            MinOptMax areaIPD, boolean isHyphenated, boolean isSpace, boolean breakOppAfter,
            Font font, int level, int[][] gposAdjustments) {
        this(startIndex, endIndex, wordSpaceCount, letterSpaceCount, areaIPD, isHyphenated,
                isSpace, breakOppAfter, font, level, gposAdjustments, null, null, false);
    }

    public GlyphMapping(int startIndex, int endIndex, int wordSpaceCount, int letterSpaceCount,
                        MinOptMax areaIPD, boolean isHyphenated, boolean isSpace, boolean isZeroWidthSpace, boolean breakOppAfter,
                        Font font, int level, int[][] gposAdjustments) {
        this(startIndex, endIndex, wordSpaceCount, letterSpaceCount, areaIPD, isHyphenated,
                isSpace, isZeroWidthSpace, breakOppAfter, font, level, gposAdjustments, null, null, false);
    }

    public GlyphMapping(int startIndex, int endIndex, int wordSpaceCount, int letterSpaceCount,
                        MinOptMax areaIPD, boolean isHyphenated, boolean isSpace, boolean breakOppAfter,
                        Font font, int level, int[][] gposAdjustments, String mapping, List associations, boolean isUpright) {
        this(startIndex, endIndex, wordSpaceCount, letterSpaceCount, areaIPD, isHyphenated,
                isSpace, false, breakOppAfter, font, level, gposAdjustments, mapping, associations, isUpright);
    }

    public GlyphMapping(int startIndex, int endIndex, int wordSpaceCount, int letterSpaceCount,
            MinOptMax areaIPD, boolean isHyphenated, boolean isSpace, boolean isZeroWidthSpace, boolean breakOppAfter,
            Font font, int level, int[][] gposAdjustments, String mapping, List associations, boolean isUpright) {
        assert startIndex <= endIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.wordCharLength = -1;
        this.wordSpaceCount = wordSpaceCount;
        this.letterSpaceCount = letterSpaceCount;
        this.areaIPD = areaIPD;
        this.isHyphenated = isHyphenated;
        this.isSpace = isSpace;
        this.isZeroWidthSpace = isZeroWidthSpace;
        this.breakOppAfter = breakOppAfter;
        this.font = font;
        this.level = level;
        this.gposAdjustments = gposAdjustments;
        this.mapping = mapping;
        this.associations = associations;
        this.isUpright = isUpright;
    }

    public static GlyphMapping doGlyphMapping(TextFragment text, int startIndex, int endIndex,
            Font font, MinOptMax letterSpaceIPD, MinOptMax[] letterSpaceAdjustArray,
            char precedingChar, char breakOpportunityChar, final boolean endsWithHyphen, int level,
            boolean dontOptimizeForIdentityMapping, boolean retainAssociations, boolean retainControls, boolean isVertical) {
        GlyphMapping mapping;
        if (font.performsSubstitution() || font.performsPositioning()) {
            mapping = processWordMapping(text, startIndex, endIndex, font,
                    breakOpportunityChar, endsWithHyphen, level,
                    dontOptimizeForIdentityMapping, retainAssociations, retainControls, isVertical);
        } else {
            mapping = processWordNoMapping(text, startIndex, endIndex, font,
                    letterSpaceIPD, letterSpaceAdjustArray, precedingChar, breakOpportunityChar, endsWithHyphen, level);
        }
        return mapping;
    }

    private static GlyphMapping processWordMapping(TextFragment text, int startIndex,
            int endIndex, final Font font, final char breakOpportunityChar,
            final boolean endsWithHyphen, int level,
            boolean dontOptimizeForIdentityMapping, boolean retainAssociations, boolean retainControls, boolean isVertical) {
        int nLS = 0; // # of letter spaces
        String script = text.getScript();
        String language = text.getLanguage();

        if (LOG.isDebugEnabled()) {
            LOG.debug("PW: [" + startIndex + "," + endIndex + "]: {"
                    + " +M"
                    + ", level = " + level
                    + " }");
        }

        // 1. extract unmapped character sequence.
        CharSequence ics = text.subSequence(startIndex, endIndex);

        // 2. if script is not specified (by FO property) or it is specified as 'auto',
        // then compute dominant script.
        if ((script == null) || "auto".equals(script)) {
            script = CharScript.scriptTagFromCode(CharScript.dominantScript(ics));
        }
        if ((language == null) || "none".equals(language) || "ar".equals(language)) {
            language = "dflt";
        }

        // 3. perform mapping of chars to glyphs ... to glyphs ... to chars, retaining
        // associations if requested.
        List associations = retainAssociations ? new ArrayList() : null;

        // This is a workaround to read the ligature from the font even if the script
        // does not match the one defined for the table.
        // More info here: https://issues.apache.org/jira/browse/FOP-2638
        // zyyy == SCRIPT_UNDEFINED
        if ("zyyy".equals(script) || "auto".equals(script)) {
            script = "*";
        }

        CharSequence mcs = font.performSubstitution(ics, script, language, associations, retainControls, isVertical);

        // 4. compute glyph position adjustments on (substituted) characters.
        int[][] gpa = null;
        if (font.performsPositioning()) {
            // handle GPOS adjustments
            gpa = font.performPositioning(mcs, script, language, isVertical);
        }
        if (useKerningAdjustments(font, script, language)) {
            // handle standard (non-GPOS) kerning adjustments
            gpa = getKerningAdjustments(mcs, font, gpa);
        }

        // 5. reorder combining marks so that they precede (within the mapped char sequence) the
        // base to which they are applied; N.B. position adjustments (gpa) are reordered in place.
        mcs = font.reorderCombiningMarks(mcs, gpa, script, language, associations, isVertical);

        // 6. compute word ipd based on final position adjustments.
        MinOptMax ipd = MinOptMax.ZERO;

        // The gpa array is sized by code point count
        boolean isUprightChar = false;
        for (int i = 0, cpi = 0, n = mcs.length(); i < n; i++, cpi++) {
            int c = mcs.charAt(i);

            if (CharUtilities.containsSurrogatePairAt(mcs, i)) {
                c = Character.toCodePoint((char) c, mcs.charAt(++i));
            }

            if (isVertical) {
                isUprightChar = isUprightChar || Characters.isUprightOrientation(c);
            }
            int w = font.getCharWidth(c); // TODO Vertical text use vertical width
            if (w < 0) {
                w = 0;
            }
            if (gpa != null) {
                w += gpa[cpi][GlyphPositioningTable.Value.IDX_X_ADVANCE];
            }
            ipd = ipd.plus(w);
        }

        // [TBD] - handle letter spacing

        return new GlyphMapping(startIndex, endIndex, 0, nLS, ipd, endsWithHyphen, false,
                breakOpportunityChar != 0, font, level, gpa,
                !dontOptimizeForIdentityMapping && CharUtilities.isSameSequence(mcs, ics) ? null : mcs.toString(),
                associations, isUprightChar);
    }

    private static boolean useKerningAdjustments(final Font font, String script, String language) {
        return font.hasKerning() && !font.hasFeature(GlyphTable.GLYPH_TABLE_TYPE_POSITIONING, script, language, "kern");
    }

    /**
     * Given a mapped character sequence MCS, obtain glyph position adjustments from the
     * font's kerning data.
     *
     * @param mcs mapped character sequence
     * @param font applicable font
     * @return glyph position adjustments (or null if no kerning)
     */
    private static int[][] getKerningAdjustments(CharSequence mcs, final Font font, int[][] gpa) {
        int numCodepoints = Character.codePointCount(mcs, 0, mcs.length());
        // extract kerning array
        int[] kernings = new int[numCodepoints]; // kerning array

        int prevCp = -1;
        int i = 0;
        for (int cp : CharUtilities.codepointsIter(mcs)) {
            if (prevCp >= 0) {
                kernings[i] = font.getKernValue(prevCp, cp);
            }
            prevCp = cp;
            i++;
        }
        // was there a non-zero kerning?
        boolean hasKerning = false;
        for (int kerningValue : kernings) {
            if (kerningValue != 0) {
                hasKerning = true;
                break;
            }
        }
        // if non-zero kerning, then create and return glyph position adjustment array
        if (hasKerning) {
            if (gpa == null) {
                gpa = new int[numCodepoints][4];
            }
            for (i = 0; i < numCodepoints; i++) {
                if (i > 0) {
                    gpa [i - 1][GlyphPositioningTable.Value.IDX_X_ADVANCE] += kernings[i];
                }
            }
            return gpa;
        } else {
            return null;
        }
    }

    private static GlyphMapping processWordNoMapping(TextFragment text, int startIndex, int endIndex,
            final Font font, MinOptMax letterSpaceIPD, MinOptMax[] letterSpaceAdjustArray,
            char precedingChar, final char breakOpportunityChar, final boolean endsWithHyphen, int level) {
        boolean kerning = font.hasKerning();
        MinOptMax wordIPD = MinOptMax.ZERO;

        if (LOG.isDebugEnabled()) {
            LOG.debug("PW: [" + startIndex + "," + endIndex + "]: {"
                    + " -M"
                    + ", level = " + level
                    + " }");
        }

        CharSequence ics = text.subSequence(startIndex, endIndex);
        int offset = 0;
        for (int currentChar : CharUtilities.codepointsIter(ics)) {

            // character width
            int charWidth = font.getCharWidth(currentChar);
            wordIPD = wordIPD.plus(charWidth);

            // kerning
            if (kerning) {
                int kern = 0;
                if (offset > 0) {
                    int previousChar = Character.codePointAt(ics, offset - 1);
                    kern = font.getKernValue(previousChar, currentChar);
                } else if (precedingChar != 0) {
                    kern = font.getKernValue(precedingChar, currentChar);
                }
                if (kern != 0) {
                    addToLetterAdjust(letterSpaceAdjustArray, startIndex + offset, kern);
                    wordIPD = wordIPD.plus(kern);
                }
            }
            offset++;
        }
        if (kerning
                && (breakOpportunityChar != 0)
                && !isSpace(breakOpportunityChar)
                && endIndex > 0
                && endsWithHyphen) {
            int endChar = text.charAt(endIndex - 1);

            if (Character.isLowSurrogate((char) endChar)) {
                char highSurrogate = text.charAt(endIndex - 2);
                endChar = Character.toCodePoint(highSurrogate, (char) endChar);
            }

            int kern = font.getKernValue(endChar, (int) breakOpportunityChar);
            if (kern != 0) {
                addToLetterAdjust(letterSpaceAdjustArray, endIndex, kern);
                // TODO: add kern to wordIPD?
            }
        }
        // shy+chars at start of word: wordLength == 0 && breakOpportunity
        // shy only characters in word: wordLength == 0 && !breakOpportunity
        int wordLength = endIndex - startIndex;
        int letterSpaces = 0;
        if (wordLength != 0) {
            letterSpaces = wordLength - 1;
            // if there is a break opportunity and the next one (break character)
            // is not a space, it could be used as a line end;
            // add one more letter space, in case other text follows
            if ((breakOpportunityChar != 0) && !isSpace(breakOpportunityChar)) {
                letterSpaces++;
            }
        }
        assert letterSpaces >= 0;
        wordIPD = wordIPD.plus(letterSpaceIPD.mult(letterSpaces));

        // create and return the AreaInfo object
        return new GlyphMapping(startIndex, endIndex, 0, letterSpaces, wordIPD, endsWithHyphen, false,
                (breakOpportunityChar != 0) && !isSpace(breakOpportunityChar), font, level, null);
    }

    private static void addToLetterAdjust(MinOptMax[] letterSpaceAdjustArray, int index, int width) {
        if (letterSpaceAdjustArray[index] == null) {
            letterSpaceAdjustArray[index] = MinOptMax.getInstance(width);
        } else {
            letterSpaceAdjustArray[index] = letterSpaceAdjustArray[index].plus(width);
        }
    }

    /**
     * Indicates whether a character is a space in terms of this layout manager.
     *
     * @param ch the character
     * @return true if it's a space
     */
    public static boolean isSpace(final char ch) {
        return ch == CharUtilities.SPACE
                || CharUtilities.isNonBreakableSpace(ch)
                || CharUtilities.isFixedWidthSpace(ch);
    }

    /**
     * Obtain number of 'characters' contained in word. If word is mapped, then this
     * number may be less than or greater than the original length (breakIndex -
     * startIndex). We compute and memoize thius length upon first invocation of this
     * method.
     */
    public int getWordLength() {
        if (wordCharLength == -1) {
            if (mapping != null) {
                wordCharLength = mapping.length();
            } else {
                assert endIndex >= startIndex;
                wordCharLength = endIndex - startIndex;
            }
        }
        return wordCharLength;
    }

    public void addToAreaIPD(MinOptMax idp) {
        areaIPD = areaIPD.plus(idp);
    }

    public String toString() {
        return super.toString() + "{"
                + "interval = [" + startIndex + "," + endIndex + "]"
                + ", isSpace = " + isSpace
                + ", level = " + level
                + ", areaIPD = " + areaIPD
                + ", letterSpaceCount = " + letterSpaceCount
                + ", wordSpaceCount = " + wordSpaceCount
                + ", isHyphenated = " + isHyphenated
                + ", font = " + font
                + "}";
    }

}
