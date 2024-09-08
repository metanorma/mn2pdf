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

package org.apache.fop.layoutmgr;

import java.util.Collections;
import java.util.List;

import org.apache.fop.fo.Constants;
import org.apache.fop.layoutmgr.inline.AlignmentContext;
import org.apache.fop.layoutmgr.inline.HyphContext;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.WritingMode;


/**
 * This class is used to pass information to the getNextKnuthElements()
 * method. It is set up by higher level LM and used by lower level LM.
 */
public final class LayoutContext {

    /** Generated break possibility is first in a new area */
    public static final int NEW_AREA = 0x01;

    /**
     * If this flag is set, it indicates that any break-before values other than "auto" should
     * not cause a mandatory break as this break was already handled by a parent layout manager.
     */
    public static final int SUPPRESS_BREAK_BEFORE = 0x02;

    public static final int FIRST_AREA = 0x04;

    public static final int LAST_AREA = 0x08;

    public static final int RESOLVE_LEADING_SPACE = 0x10;

    private static final int TREAT_AS_ARTIFACT = 0x20;

    private int flags; // Contains some set of flags defined above

    /**
     * Total available stacking dimension for a "galley-level" layout
     * manager in block-progression-direction. It is passed by the
     * parent LM.
     * These LM <b>may</b> wish to pass this information down to lower
     * level LM to allow them to optimize returned break possibilities.
     */
    private MinOptMax stackLimitBP;

    /** to keep track of spanning in multi-column layout */
    private int currentSpan = Constants.NOT_SET;
    private int nextSpan = Constants.NOT_SET;

    /** inline-progression-dimension of nearest ancestor reference area */
    private int refIPD;
    //TODO After the split of stackLimit into stackLimitBP and stackLimitIP there's now some
    //overlap with refIPD. Need to investigate how best to refactor that.

    /** the writing mode established by the nearest ancestor reference area */
    private WritingMode writingMode = WritingMode.LR_TB;

    /** Current pending space-after or space-end from preceding area */
    private SpaceSpecifier trailingSpace;

    /** Current pending space-before or space-start from ancestor areas */
    private SpaceSpecifier leadingSpace;

    /**
     * A list of pending marks (border and padding) on the after edge when a page break occurs.
     * May be null.
     */
    private List pendingAfterMarks;

    /**
     * A list of pending marks (border and padding) on the before edge when a page break occurs.
     * May be null.
     */
    private List pendingBeforeMarks;

    /** Current hyphenation context. May be null. */
    private HyphContext hyphContext;

    /** Alignment in BP direction */
    private int bpAlignment = Constants.EN_START;

    /** Stretch or shrink value when making areas. */
    private double ipdAdjust;

    /** Stretch or shrink value when adding spaces. */
    private double dSpaceAdjust;

    private AlignmentContext alignmentContext;

    /** Amount of space before / start */
    private int spaceBefore;
    /** Amount of space after / end */
    private int spaceAfter;

    /** Amount of space to reserve at the beginning of each line */
    private int lineStartBorderAndPaddingWidth;
    /** Amount of space to reserve at the end of each line */
    private int lineEndBorderAndPaddingWidth;

    private int breakBefore;
    private int breakAfter;

    private Keep pendingKeepWithNext = Keep.KEEP_AUTO;
    private Keep pendingKeepWithPrevious = Keep.KEEP_AUTO;

    private int disableColumnBalancing;

    public static LayoutContext newInstance() {
        return new LayoutContext(0);
    }

    public static LayoutContext copyOf(LayoutContext copy) {
        return new LayoutContext(copy);
    }

    /**
     * Returns a descendant of the given layout context. The new context is the same as
     * what would have been created by {@link #newInstance()}, except for inheritable
     * properties that are passed on by the parent. At the moment, the only inheritable
     * property is the value returned by {@link #treatAsArtifact()}.
     */
    public static LayoutContext offspringOf(LayoutContext parent) {
        LayoutContext offspring = new LayoutContext(0);
        offspring.setTreatAsArtifact(parent.treatAsArtifact());
        offspring.setWritingMode(parent.getWritingMode());
        return offspring;
    }

    private LayoutContext(LayoutContext parentLC) {
        this.flags = parentLC.flags;
        this.refIPD = parentLC.refIPD;
        this.writingMode = parentLC.writingMode;
        setStackLimitBP(parentLC.getStackLimitBP());
        this.leadingSpace = parentLC.leadingSpace; //???
        this.trailingSpace = parentLC.trailingSpace; //???
        this.hyphContext = parentLC.hyphContext;
        this.bpAlignment = parentLC.bpAlignment;
        this.dSpaceAdjust = parentLC.dSpaceAdjust;
        this.ipdAdjust = parentLC.ipdAdjust;
        this.alignmentContext = parentLC.alignmentContext;
        this.lineStartBorderAndPaddingWidth = parentLC.lineStartBorderAndPaddingWidth;
        this.lineEndBorderAndPaddingWidth = parentLC.lineEndBorderAndPaddingWidth;
        copyPendingMarksFrom(parentLC);
        this.pendingKeepWithNext = parentLC.pendingKeepWithNext;
        this.pendingKeepWithPrevious = parentLC.pendingKeepWithPrevious;
        // Copy other fields as necessary.
        this.disableColumnBalancing = parentLC.disableColumnBalancing;
    }

    private LayoutContext(int flags) {
        this.flags = flags;
        this.refIPD = 0;
        stackLimitBP = MinOptMax.ZERO;
        leadingSpace = null;
        trailingSpace = null;
    }

    /** @param source from which pending marks are copied */
    public void copyPendingMarksFrom(LayoutContext source) {
        if (source.pendingAfterMarks != null) {
            this.pendingAfterMarks = new java.util.ArrayList(source.pendingAfterMarks);
        }
        if (source.pendingBeforeMarks != null) {
            this.pendingBeforeMarks = new java.util.ArrayList(source.pendingBeforeMarks);
        }
    }

    /** @param flags to set */
    public void setFlags(int flags) {
        setFlags(flags, true);
    }

    /**
     * @param flags to set or clear
     * @param bSet true to set, false to clear
     */
    public void setFlags(int flags, boolean bSet) {
        if (bSet) {
            this.flags |= flags;
        } else {
            this.flags &= ~flags;
        }
    }

    /** @param flags to clear */
    public void unsetFlags(int flags) {
        setFlags(flags, false);
    }

    /** @return true if new area is set */
    public boolean isStart() {
        return ((this.flags & NEW_AREA) != 0);
    }

    /** @return true if new area is set and leading space is non-null */
    public boolean startsNewArea() {
        return ((this.flags & NEW_AREA) != 0 && leadingSpace != null);
    }

    /** @return true if first area is set */
    public boolean isFirstArea() {
        return ((this.flags & FIRST_AREA) != 0);
    }

    /** @return true if last area is set */
    public boolean isLastArea() {
        return ((this.flags & LAST_AREA) != 0);
    }

    /** @return true if suppress break before is set */
    public boolean suppressBreakBefore() {
        return ((this.flags & SUPPRESS_BREAK_BEFORE) != 0);
    }

    /**
     * Returns the strength of a keep-with-next currently pending.
     * @return the keep-with-next strength
     */
    public Keep getKeepWithNextPending() {
        return this.pendingKeepWithNext;
    }

    /**
     * Returns the strength of a keep-with-previous currently pending.
     * @return the keep-with-previous strength
     */
    public Keep getKeepWithPreviousPending() {
        return this.pendingKeepWithPrevious;
    }

    /**
     * Clears any pending keep-with-next strength.
     */
    public void clearKeepWithNextPending() {
        this.pendingKeepWithNext = Keep.KEEP_AUTO;
    }

    /**
     * Clears any pending keep-with-previous strength.
     */
    public void clearKeepWithPreviousPending() {
        this.pendingKeepWithPrevious = Keep.KEEP_AUTO;
    }

    /**
     * Clears both keep-with-previous and keep-with-next strengths.
     */
    public void clearKeepsPending() {
        clearKeepWithPreviousPending();
        clearKeepWithNextPending();
    }

    /**
     * Updates the currently pending keep-with-next strength.
     * @param keep the new strength to consider
     */
    public void updateKeepWithNextPending(Keep keep) {
        this.pendingKeepWithNext = this.pendingKeepWithNext.compare(keep);
    }

    /**
     * Updates the currently pending keep-with-previous strength.
     * @param keep the new strength to consider
     */
    public void updateKeepWithPreviousPending(Keep keep) {
        this.pendingKeepWithPrevious = this.pendingKeepWithPrevious.compare(keep);
    }

    /**
     * Indicates whether a keep-with-next constraint is pending.
     * @return true if a keep-with-next constraint is pending
     */
    public boolean isKeepWithNextPending() {
        return !getKeepWithNextPending().isAuto();
    }

    /**
     * Indicates whether a keep-with-previous constraint is pending.
     * @return true if a keep-with-previous constraint is pending
     */
    public boolean isKeepWithPreviousPending() {
        return !getKeepWithPreviousPending().isAuto();
    }

    /** @param space leading space */
    public void setLeadingSpace(SpaceSpecifier space) {
        leadingSpace = space;
    }

    /** @return leading space */
    public SpaceSpecifier getLeadingSpace() {
        return leadingSpace;
    }

    /** @return true if resolve leading space is set */
    public boolean resolveLeadingSpace() {
        return ((this.flags & RESOLVE_LEADING_SPACE) != 0);
    }

    /** @param space trailing space */
    public void setTrailingSpace(SpaceSpecifier space) {
        trailingSpace = space;
    }

    /** @return trailing space */
    public SpaceSpecifier getTrailingSpace() {
        return trailingSpace;
    }

    /**
     * Adds a border or padding element to the pending list which will be used to generate
     * the right element list for break possibilities. Conditionality resolution will be done
     * elsewhere.
     * @param element the border, padding or space element
     */
    public void addPendingAfterMark(UnresolvedListElementWithLength element) {
        if (this.pendingAfterMarks == null) {
            this.pendingAfterMarks = new java.util.ArrayList();
        }
        this.pendingAfterMarks.add(element);
    }

    /**
     * @return the pending border and padding elements at the after edge
     * @see #addPendingAfterMark(UnresolvedListElementWithLength)
     */
    public List getPendingAfterMarks() {
        if (this.pendingAfterMarks != null) {
            return Collections.unmodifiableList(this.pendingAfterMarks);
        } else {
            return null;
        }
    }

    /**
     * Clears all pending marks on the LayoutContext.
     */
    public void clearPendingMarks() {
        this.pendingBeforeMarks = null;
        this.pendingAfterMarks = null;
    }

    /**
     * Adds a border or padding element to the pending list which will be used to generate
     * the right element list for break possibilities. Conditionality resolution will be done
     * elsewhere.
     * @param element the border, padding or space element
     */
    public void addPendingBeforeMark(UnresolvedListElementWithLength element) {
        if (this.pendingBeforeMarks == null) {
            this.pendingBeforeMarks = new java.util.ArrayList();
        }
        this.pendingBeforeMarks.add(element);
    }

    /**
     * @return the pending border and padding elements at the before edge
     * @see #addPendingBeforeMark(UnresolvedListElementWithLength)
     */
    public List getPendingBeforeMarks() {
        if (this.pendingBeforeMarks != null) {
            return Collections.unmodifiableList(this.pendingBeforeMarks);
        } else {
            return null;
        }
    }

    /**
     * Sets the stack limit in block-progression-dimension.
     * @param limit the stack limit
     */
    public void setStackLimitBP(MinOptMax limit) {
        stackLimitBP = limit;
    }

    /**
     * Returns the stack limit in block-progression-dimension.
     * @return the stack limit
     */
    public MinOptMax getStackLimitBP() {
        return stackLimitBP;
    }

    /**
     * Sets the inline-progression-dimension of the nearest ancestor reference area.
     * @param ipd of nearest ancestor reference area
     */
    public void setRefIPD(int ipd) {
        refIPD = ipd;
    }

    /**
     * Returns the inline-progression-dimension of the nearest ancestor reference area.
     *
     * @return the inline-progression-dimension of the nearest ancestor reference area
     */
    public int getRefIPD() {
        return refIPD;
    }

    /** @param hyph a hyphenation context */
    public void setHyphContext(HyphContext hyph) {
        hyphContext = hyph;
    }

    /** @return hyphenation context */
    public HyphContext getHyphContext() {
        return hyphContext;
    }

    /**
     * Sets the currently applicable alignment in BP direction.
     * @param alignment one of EN_START, EN_JUSTIFY etc.
     */
    public void setBPAlignment(int alignment) {
        this.bpAlignment = alignment;
    }

    /** @return the currently applicable alignment in BP direction (EN_START, EN_JUSTIFY...) */
    public int getBPAlignment() {
        return this.bpAlignment;
    }

    /** @param adjust space adjustment */
    public void setSpaceAdjust(double adjust) {
        dSpaceAdjust = adjust;
    }

    /** @return space adjustment */
    public double getSpaceAdjust() {
        return dSpaceAdjust;
    }

    /** @param ipdA ipd adjustment */
    public void setIPDAdjust(double ipdA) {
        ipdAdjust = ipdA;
    }

    /** @return ipd adjustment */
    public double getIPDAdjust() {
        return ipdAdjust;
    }

    /** @param alignmentContext alignment context */
    public void setAlignmentContext(AlignmentContext alignmentContext) {
        this.alignmentContext = alignmentContext;
    }

    /** @return alignment context */
    public AlignmentContext getAlignmentContext() {
        return this.alignmentContext;
    }

    /**
     * Reset alignment context.
     */
    public void resetAlignmentContext() {
        if (this.alignmentContext != null) {
            this.alignmentContext = this.alignmentContext.getParentAlignmentContext();
        }
    }

    /**
     * Get the width to be reserved for border and padding at the start of the line.
     * @return the width to be reserved
     */
    public int getLineStartBorderAndPaddingWidth() {
        return lineStartBorderAndPaddingWidth;
    }

    /**
     * Set the width to be reserved for border and padding at the start of the line.
     * @param lineStartBorderAndPaddingWidth the width to be reserved
     */
    public void setLineStartBorderAndPaddingWidth(int lineStartBorderAndPaddingWidth) {
        this.lineStartBorderAndPaddingWidth = lineStartBorderAndPaddingWidth;
    }

    /**
     * Get the width to be reserved for border and padding at the end of the line.
     * @return the width to be reserved
     */
    public int getLineEndBorderAndPaddingWidth() {
        return lineEndBorderAndPaddingWidth;
    }

    /**
     * Set the width to be reserved for border and padding at the end of the line.
     * @param lineEndBorderAndPaddingWidth the width to be reserved
     */
    public void setLineEndBorderAndPaddingWidth(int lineEndBorderAndPaddingWidth) {
        this.lineEndBorderAndPaddingWidth = lineEndBorderAndPaddingWidth;
    }

    /**
     * @return one of: {@link Constants#NOT_SET}, {@link Constants#EN_NONE}
     *                  {@link Constants#EN_ALL}
     */
    public int getNextSpan() {
        return nextSpan;
    }

    /**
     * @return one of: {@link Constants#NOT_SET}, {@link Constants#EN_NONE}
     *                  {@link Constants#EN_ALL}
     */
    public int getCurrentSpan() {
        return (currentSpan == Constants.NOT_SET)
                ? Constants.EN_NONE : currentSpan;
    }

    /**
     * Used to signal the PSLM that the element list ends early because of a span change in
     * multi-column layout.
     * @param span the new span value (legal values: NOT_SET, EN_NONE, EN_ALL)
     */
    public void signalSpanChange(int span) {
        switch (span) {
        case Constants.NOT_SET:
        case Constants.EN_NONE:
        case Constants.EN_ALL:
            this.currentSpan = this.nextSpan;
            this.nextSpan = span;
            break;
        default:
            assert false;
            throw new IllegalArgumentException("Illegal value on signalSpanChange() for span: "
                    + span);
        }
    }

    /**
     * Get the writing mode of the relevant reference area.
     * @return the applicable writing mode
     */
    public WritingMode getWritingMode() {
        return writingMode;
    }

    /**
     * Set the writing mode.
     * @param writingMode the writing mode
     */
    public void setWritingMode(WritingMode writingMode) {
        this.writingMode = writingMode;
    }

    /**
     * Get the current amount of space before / start
     * @return the space before / start amount
     */
    public int getSpaceBefore() {
        return spaceBefore;
    }

    /**
     * Set the amount of space before / start
     * @param spaceBefore the amount of space before / start
     */
    public void setSpaceBefore(int spaceBefore) {
        this.spaceBefore = spaceBefore;
    }

    /**
     * Get the current amount of space after / end
     * @return the space after / end amount
     */
    public int getSpaceAfter() {
        return spaceAfter;
    }

    /**
     * Set the amount of space after / end
     * @param spaceAfter the amount of space after / end
     */
    public void setSpaceAfter(int spaceAfter) {
        this.spaceAfter = spaceAfter;
    }

    /**
     * Returns the value of the break before the element whose
     * {@link LayoutManager#getNextKnuthElements(LayoutContext, int)} method has just been
     * called.
     *
     * @return one of {@link Constants#EN_AUTO}, {@link Constants#EN_COLUMN},
     * {@link Constants#EN_PAGE}, {@link Constants#EN_EVEN_PAGE}, or
     * {@link Constants#EN_ODD_PAGE}
     */
    public int getBreakBefore() {
        return breakBefore;
    }

    /**
     * Sets the value of the break before the current element.
     *
     * @param breakBefore the value of the break-before
     * @see #getBreakBefore()
     */
    public void setBreakBefore(int breakBefore) {
        this.breakBefore = breakBefore;
    }

    /**
     * Returns the value of the break after the element whose
     * {@link LayoutManager#getNextKnuthElements(LayoutContext, int)} method has just been
     * called.
     *
     * @return one of {@link Constants#EN_AUTO}, {@link Constants#EN_COLUMN},
     * {@link Constants#EN_PAGE}, {@link Constants#EN_EVEN_PAGE}, or
     * {@link Constants#EN_ODD_PAGE}
     */
    public int getBreakAfter() {
        return breakAfter;
    }


    /**
     * Sets the value of the break after the current element.
     *
     * @param breakAfter the value of the break-after
     * @see #getBreakAfter()
     */
    public void setBreakAfter(int breakAfter) {
        this.breakAfter = breakAfter;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Layout Context:"
                + "\nStack Limit BPD: \t"
                + (getStackLimitBP() == null ? "null" : getStackLimitBP().toString())
                + "\nTrailing Space: \t"
                + (getTrailingSpace() == null ? "null" : getTrailingSpace().toString())
                + "\nLeading Space: \t"
                + (getLeadingSpace() == null ? "null" : getLeadingSpace().toString())
                + "\nReference IPD: \t" + getRefIPD()
                + "\nSpace Adjust: \t" + getSpaceAdjust()
                + "\nIPD Adjust: \t" + getIPDAdjust()
                + "\nResolve Leading Space: \t" + resolveLeadingSpace()
                + "\nSuppress Break Before: \t" + suppressBreakBefore()
                + "\nIs First Area: \t" + isFirstArea()
                + "\nStarts New Area: \t" + startsNewArea()
                + "\nIs Last Area: \t" + isLastArea()
                + "\nKeeps: \t[keep-with-next=" + getKeepWithNextPending()
                + "][keep-with-previous=" + getKeepWithPreviousPending() + "] pending"
                + "\nBreaks: \tforced [" + (breakBefore != Constants.EN_AUTO ? "break-before" : "") + "]["
                + (breakAfter != Constants.EN_AUTO ? "break-after" : "") + "]";
    }

    /**
     * Returns whether the column balancer should be disabled before a spanning block
     *
     * @return one of {@link Constants#EN_TRUE}, {@link Constants#EN_FALSE}
     */
    public int getDisableColumnBalancing() {
        return disableColumnBalancing;
    }

    /**
     * Sets whether the column balancer should be disabled before a spanning block
     *
     * @param disableColumnBalancing the value of the fox:disable-column-balancing property
     * @see #getDisableColumnBalancing()
     */
    public void setDisableColumnBalancing(int disableColumnBalancing) {
        this.disableColumnBalancing = disableColumnBalancing;
    }

    public boolean treatAsArtifact() {
        return (flags & TREAT_AS_ARTIFACT) != 0;
    }

    public void setTreatAsArtifact(boolean treatAsArtifact) {
        setFlags(TREAT_AS_ARTIFACT, treatAsArtifact);
    }
}

