//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M e a s u r e S t a c k                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Trimmable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.Marshaller;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code MeasureStack} represents a vertical stack of {@link Measure} instances,
 * embracing all parts of a system.
 * <p>
 * This approach is convenient to use vertical alignments when dealing with time slots.
 * <p>
 * How is measure ID handled:
 * <ol>
 * <li>IDs are recorded as page-based integer values.</li>
 * <li>Initial values start from 1 and are incremented for each following raw measure.</li>
 * <li>Once measure role is refined, its ID may change:
 * <ul>
 * <li>A pickup measure has ID 0, instead of 1.</li>
 * <li>A special value (Xn) is used for second half repeats, when first half repeat is n.</li>
 * <li>Courtesy measures that may be found at end of system (containing just cautionary key or time
 * signatures) have an ID with C suffix (nC).</li>
 * </ul>
 * <li>IDs, as exported in MusicXML, combine the page-based IDs to provide score-based (absolute)
 * IDs.</li>
 * </ol>
 *
 * @see Measure
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "stack")
public class MeasureStack
{

    private static final Logger logger = LoggerFactory.getLogger(MeasureStack.class);

    /** String prefix for a second half id: {@value}. */
    public static final String SECOND_HALF_PREFIX = "X";

    /** String suffix for a cautionary id: {@value}. */
    public static final String CAUTIONARY_SUFFIX = "C";

    // Persistent data
    //----------------
    //
    /** (Page-based) measure Id. */
    @XmlAttribute
    private Integer id;

    /** Minimum left abscissa, across all staves. */
    @XmlAttribute
    private int left;

    /** Maximum right abscissa, across all staves. */
    @XmlAttribute
    private int right;

    /** Sequence of time slots within the measure, from left to right. */
    @XmlElementRef
    private final List<Slot> slots = new ArrayList<>();

    /** Indication for special measure stack. */
    @XmlAttribute
    private Special special;

    /** Repeat sign on either side of the measure stack. */
    @XmlList
    @XmlAttribute(name = "repeat")
    private Set<HorizontalSide> repeats;

    /** Theoretical measure stack duration, based on current time signature. */
    @XmlAttribute(name = "expected")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational expectedDuration;

    /**
     * Actual measure stack duration, based on durations of contained chords.
     * If the stack contains no note (head or rest), actual duration is ZERO.
     * If the stack contains only whole rest(s), actual duration is given by current time signature.
     * Otherwise, duration is computed from contained slots and voices.
     * If stack timing fails for whatever reason, actual duration may be left as null.
     */
    @XmlAttribute(name = "duration")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational actualDuration;

    /** Excess measure stack duration, if any. */
    @XmlAttribute(name = "excess")
    @XmlJavaTypeAdapter(Rational.Adapter.class)
    private Rational excess;

    /**
     * Anomaly detected, if any.
     * Deprecated since individual measures can now be flagged as abnormal.
     */
    @Deprecated
    @XmlAttribute(name = "abnormal")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean abnormal;

    /** Still unassigned tuplets within stack. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "tuplets")
    @Trimmable.Collection
    private final LinkedHashSet<TupletInter> stackTuplets = new LinkedHashSet<>();

    // Transient data
    //---------------
    //
    /** The containing system. */
    @Navigable(false)
    private SystemInfo system;

    /** Vertical sequence of (Part) measures, from top to bottom. */
    private final List<Measure> measures = new ArrayList<>();

    /**
     * Creates a new {@code MeasureStack} object.
     *
     * @param system containing system
     */
    public MeasureStack (SystemInfo system)
    {
        this.system = system;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private MeasureStack ()
    {
        this.system = null;
    }

    //----------//
    // addInter //
    //----------//
    /**
     * Add an inter to stack.
     *
     * @param inter the inter to add
     */
    public void addInter (Inter inter)
    {
        final Part part = inter.getPart();

        if (part != null) {
            Measure measure = getMeasureAt(part);
            measure.addInter(inter);

            if (inter instanceof TupletInter) {
                stackTuplets.remove(inter);
            }
        } else if (inter instanceof TupletInter) {
            stackTuplets.add((TupletInter) inter);
        } else {
            logger.debug("No part yet for {}", inter);
        }
    }

    //------------//
    // addMeasure //
    //------------//
    /**
     * Append a measure in stack.
     *
     * @param measure the measure to append
     */
    public void addMeasure (Measure measure)
    {
        if (measures.isEmpty()) {
            left = Integer.MAX_VALUE;
            right = 0;
        }

        measures.add(measure);

        for (Staff staff : measure.getPart().getStaves()) {
            left = Math.min(left, measure.getAbscissa(LEFT, staff));
            right = Math.max(right, measure.getAbscissa(RIGHT, staff));
        }
    }

    //------------//
    // addMeasure //
    //------------//
    /**
     * Append a measure in stack at provided index
     *
     * @param index   index where measure is to be inserted
     * @param measure the measure to append
     */
    public void addMeasure (int index,
                            Measure measure)
    {
        measures.add(index, measure);
    }

    //-----------//
    // addRepeat //
    //-----------//
    /**
     * Add a repeat indication on the provided horizontal side.
     *
     * @param side provided side of the measure
     */
    public void addRepeat (HorizontalSide side)
    {
        if (repeats == null) {
            repeats = EnumSet.noneOf(HorizontalSide.class);
        }

        repeats.add(side);
    }

    //------------------//
    // addTimeSignature //
    //------------------//
    /**
     * Add a time signature.
     *
     * @param ts the time signature to add
     */
    public void addTimeSignature (AbstractTimeInter ts)
    {
        // Populate (part) measure with provided time signature
        Point center = ts.getCenter();
        Staff staff = ts.getStaff();

        if (staff == null) {
            ts.setStaff(staff = system.getStaffAtOrAbove(center));
        }

        Measure measure = staff.getPart().getMeasureAt(center);
        measure.addInter(ts);
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     *
     * @param system the containing system
     */
    public void afterReload (SystemInfo system)
    {
        try {
            this.system = system;

            // Fill measures
            final int im = system.getStacks().indexOf(this);

            for (Part part : system.getParts()) {
                Measure measure = part.getMeasures().get(im);
                measure.setStack(this);
                measures.add(measure);
            }

            // Forward to slots
            for (Slot slot : slots) {
                slot.afterReload();
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //---------------//
    // checkDuration //
    //---------------//
    /**
     * Check the duration as computed in this stack from its contained voices,
     * compared to its theoretical duration.
     */
    public void checkDuration ()
    {
        for (Measure measure : measures) {
            measure.checkDuration();
        }
    }

    //----------------//
    // computeRepeats //
    //----------------//
    /**
     * Compute the possible repeat indications (left, right) for this stack.
     * <p>
     * All staves of the stack have to be checked because the repeat check may correct the
     * current style of the barline, in case thin and thick barlines were not correctly recognized.
     */
    public void computeRepeats ()
    {
        repeats = null;

        for (Measure measure : measures) {
            checkRepeats(measure.getPartBarlineOn(LEFT), LEFT);
            checkRepeats(measure.getMidPartBarline(), LEFT);
            checkRepeats(measure.getPartBarlineOn(RIGHT), RIGHT);
        }
    }

    //--------------//
    // checkRepeats //
    //--------------//
    /**
     * Check every staff of this measure for a repeat sign at the provided partBarline.
     *
     * @param partBarline the provided PartBarline
     * @param side        repeat side: LEFT or RIGHT
     */
    private void checkRepeats (final PartBarline partBarline,
                               final HorizontalSide side)
    {
        if (partBarline == null) {
            return;
        }

        final List<StaffBarlineInter> bars = partBarline.getStaffBarlines();

        for (StaffBarlineInter sbl : bars) {
            if (side == LEFT) {
                if (sbl.isLeftRepeat()) {
                    addRepeat(LEFT);
                }
            } else {
                if (sbl.isRightRepeat()) {
                    addRepeat(RIGHT);
                }
            }
        }
    }

    //-------------------//
    // connectTiedVoices //
    //-------------------//
    /**
     * Make sure that notes tied across measures stacks keep the same voice.
     * This is performed for all ties in this part.
     */
    public void connectTiedVoices ()
    {
        //        for (TreeNode tn : getSlurs()) {
        //            Slur slur = (Slur) tn;
        //
        //            if (!slur.isTie()) {
        //                continue;
        //            }
        //
        //            // Voice on left (perhaps in a previous measure / system / page)
        //            Note leftNote = slur.getLeftNote();
        //
        //            if (leftNote == null) {
        //                Slur leftExtension = slur.getLeftExtension();
        //
        //                if (leftExtension == null) {
        //                    continue;
        //                }
        //
        //                leftNote = leftExtension.getLeftNote();
        //
        //                if (leftNote == null) {
        //                    continue;
        //                }
        //            }
        //
        //            AbstractChordInter leftChord = leftNote.getChord();
        //            OldVoice leftVoice = leftChord.getVoice();
        //
        //            // Voice on right
        //            Note rightNote = slur.getRightNote();
        //
        //            if (rightNote == null) {
        //                continue;
        //            }
        //
        //            AbstractChordInter rightChord = rightNote.getChord();
        //            OldVoice rightVoice = rightChord.getVoice();
        //
        //            if (leftVoice.getId() != rightVoice.getId()) {
        //                logger.debug("Tie to map {} and {}", leftChord, rightChord);
        //                rightChord.getMeasure().swapVoiceId(rightVoice, leftVoice.getId());
        //            }
        //        }
    }

    //----------//
    // contains //
    //----------//
    /**
     * Tell whether the stack contains the provided point.
     *
     * @param point the provided point
     * @return true if so
     */
    public boolean contains (Point2D point)
    {
        return system.getStackAt(point) == this;
    }

    //--------//
    // filter //
    //--------//
    /**
     * From the provided list of system inters, keep only the ones that are located
     * within this measure stack.
     *
     * @param systemInters the provided list of inters at system level
     * @return the inters kept
     */
    public List<Inter> filter (Collection<Inter> systemInters)
    {
        List<Inter> kept = new ArrayList<>();

        for (Inter inter : systemInters) {
            Point center = inter.getCenter();

            // Rough abscissa limits
            if ((center.x < left) || (center.x > right)) {
                continue;
            }

            Staff staff = inter.getStaff();
            final Measure measure;

            if (staff != null) {
                Part part = staff.getPart();
                measure = part.getMeasureAt(center);
            } else {
                List<Staff> stavesAround = system.getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);
                measure = getMeasureAt(staff);
            }

            // Precise abscissa limits
            if ((measure.getAbscissa(LEFT, staff) <= center.x) && (center.x <= measure.getAbscissa(
                    RIGHT,
                    staff))) {
                kept.add(inter);
            }
        }

        return kept;
    }

    //-------------------//
    // getActualDuration //
    //-------------------//
    /**
     * Report the duration of this measure stack , as computed from its contained voices.
     *
     * @return the (actual) measure stack duration, 0 if no rest / note exists in this stack,
     *         null if it could not be computed.
     */
    public Rational getActualDuration ()
    {
        return actualDuration;
    }

    //-------------------//
    // setActualDuration //
    //-------------------//
    /**
     * Register in this measure stack its actual duration.
     *
     * @param actualDuration the duration value
     */
    public void setActualDuration (Rational actualDuration)
    {
        this.actualDuration = actualDuration;
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * Among a provided Chord collection, report the chord at smallest euclidian
     * distance from the provided point.
     *
     * @param chords the collection of chords to browse
     * @param point  the reference point
     * @return the euclidian-wise closest chord
     */
    public AbstractChordInter getClosestChord (Collection<AbstractChordInter> chords,
                                               Point2D point)
    {
        AbstractChordInter bestChord = null;
        double bestDsq = Double.MAX_VALUE;

        for (AbstractChordInter chord : chords) {
            Rectangle chordBox = chord.getBounds();
            double dsq = GeoUtil.ptDistanceSq(chordBox, point.getX(), point.getY());

            if (dsq < bestDsq) {
                bestDsq = dsq;
                bestChord = chord;
            }
        }

        return bestChord;
    }

    //----------------//
    // getClosestSlot //
    //----------------//
    /**
     * Report the time slot which has the closest abscissa to a provided point.
     *
     * @param point the reference point
     * @return the abscissa-wise closest slot
     */
    public Slot getClosestSlot (Point2D point)
    {
        final double xOffset = getXOffset(point);
        Slot bestSlot = null;
        double bestDx = Double.MAX_VALUE;

        for (Slot slot : slots) {
            double dx = Math.abs(slot.getXOffset() - xOffset);

            if (dx < bestDx) {
                bestDx = dx;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    //-------------------------//
    // getCurrentTimeSignature //
    //-------------------------//
    /**
     * Report the time signature which applies in this stack, whether a time signature
     * actually starts this stack in whatever staff, or whether a time signature was
     * found in a previous stack, even in preceding pages.
     * <p>
     * <b>NOTA</b>This method looks up for time sig in preceding pages as well
     * </p>
     *
     * @return the current time signature, or null if not found at all
     */
    public AbstractTimeInter getCurrentTimeSignature ()
    {
        // Backward from this measure to the beginning of the score
        MeasureStack stack = this;
        Page page = system.getPage();

        while (stack != null) {
            // Check in the measure stack
            AbstractTimeInter ts = stack.getFirstMeasure().getTimeSignature();

            if (ts != null) {
                return ts;
            }

            // Move to preceding measure stack
            stack = stack.getPrecedingInPage();

            //
            //            if (measure == null) {
            //                page = page.getPrecedingInScore();
            //
            //                if (page == null) {
            //                    return null;
            //                } else {
            //                    measure = page.getLastSystem().getLastPart().getLastMeasure();
            //                }
            //            }
        }

        return null; // Not found !!!
    }

    //---------------//
    // getEventChord //
    //---------------//
    /**
     * Retrieve the most suitable chord to connect the event point to.
     *
     * @param point  the system-based location
     * @param xRange required abscissa range, or null
     * @return the most suitable chord, or null
     */
    public AbstractChordInter getEventChord (Point2D point,
                                             Rectangle xRange)
    {
        // First, try staff just above
        AbstractChordInter above = getStandardChordAbove(point, xRange);

        if (above != null) {
            return above;
        }

        // Second, try staff just below
        return getStandardChordBelow(point, xRange);
    }

    //-----------//
    // getExcess //
    //-----------//
    /**
     * Report the excess duration of this stack, if any.
     *
     * @return the duration in excess, or null
     */
    public Rational getExcess ()
    {
        return excess;
    }

    //-----------//
    // setExcess //
    //-----------//
    /**
     * Assign an excess duration for this stack.
     *
     * @param excess the duration in excess
     */
    public void setExcess (Rational excess)
    {
        this.excess = excess;
    }

    //---------------------//
    // getExpectedDuration //
    //---------------------//
    /**
     * Report the theoretical duration of this stack.
     *
     * @return the expected measure stack duration
     */
    public Rational getExpectedDuration ()
    {
        return expectedDuration;
    }

    //---------------------//
    // setExpectedDuration //
    //---------------------//
    /**
     * Set measure expected duration.
     *
     * @param expectedDuration the expected duration
     */
    public void setExpectedDuration (Rational expectedDuration)
    {
        this.expectedDuration = expectedDuration;
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    /**
     * Report the top measure in stack.
     *
     * @return top measure
     */
    public Measure getFirstMeasure ()
    {
        if (measures.isEmpty()) {
            return null;
        }

        return measures.get(0);
    }

    //--------------------//
    // getFollowingInPage //
    //--------------------//
    /**
     * Report the following measure stack of this one, either in this system, or in the
     * following system, but still in the same page.
     *
     * @return the following measure stack, or null if not found in the page
     */
    public MeasureStack getFollowingInPage ()
    {
        // Look in current part
        MeasureStack nextStack = getNextSibling();

        if (nextStack != null) {
            return nextStack;
        }

        SystemInfo nextSystem = system.getFollowingInPage();

        if (nextSystem != null) {
            return nextSystem.getFirstStack();
        } else {
            return null;
        }
    }

    //---------------//
    // getHeadChords //
    //---------------//
    /**
     * Report all head chords in stack.
     *
     * @return stack head chords
     * @see #getStandardHeadChords()
     */
    public Set<HeadChordInter> getHeadChords ()
    {
        Set<HeadChordInter> headChords = new LinkedHashSet<>();

        for (Measure measure : measures) {
            headChords.addAll(measure.getHeadChords());
        }

        return headChords;
    }

    //------------//
    // getIdValue //
    //------------//
    /**
     * Report the numeric value of the measure id.
     * Note that first (n) and second (Xn) measure halves share the same numeric value n.
     *
     * @return the numeric value of measure id
     */
    public int getIdValue ()
    {
        return id;
    }

    //------------//
    // setIdValue //
    //------------//
    /**
     * Assign the proper page-based id value to this measure stack.
     *
     * @param id the proper page-based measure stack id value
     */
    public void setIdValue (int id)
    {
        this.id = id;
    }

    //-------------//
    // getLastSlot //
    //-------------//
    /**
     * Report the last slot in stack.
     *
     * @return the last slot or null if none
     */
    public Slot getLastSlot ()
    {
        if (slots.isEmpty()) {
            return null;
        }

        return slots.get(slots.size() - 1);
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * @return the left
     */
    public int getLeft ()
    {
        return left;
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure at provided staff
     *
     * @param staff the provided staff
     * @return the measure that contains the provided staff
     */
    public Measure getMeasureAt (Staff staff)
    {
        for (Measure measure : measures) {
            if (measure.getPart().getStaves().contains(staff)) {
                return measure;
            }
        }

        return null;
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure at provided part
     *
     * @param part the provided part
     * @return the measure that contains the provided part
     */
    public Measure getMeasureAt (Part part)
    {
        for (Measure measure : measures) {
            if (measure.getPart() == part) {
                return measure;
            }
        }

        return null;
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the vertical sequence of measures in this stack.
     *
     * @return measures in stack
     */
    public List<Measure> getMeasures ()
    {
        return measures;
    }

    //----------------//
    // getNextSibling //
    //----------------//
    /**
     * Return the next measure stack within the same system.
     *
     * @return previous sibling measure stack in system, or null
     */
    public MeasureStack getNextSibling ()
    {
        final List<MeasureStack> stacks = system.getStacks();
        final int index = stacks.indexOf(this);

        if (index < (stacks.size() - 1)) {
            return stacks.get(index + 1);
        }

        return null;
    }

    //-----------//
    // getPageId //
    //-----------//
    /**
     * Report the page-based measure id string.
     *
     * @return the page-based measure id string
     */
    public String getPageId ()
    {
        if (id != null) {
            return ((special == Special.SECOND_HALF) ? SECOND_HALF_PREFIX : "") + id
                           + (isCautionary() ? CAUTIONARY_SUFFIX : "");
        }

        // No id defined yet
        StringBuilder sb = new StringBuilder();
        sb.append("S").append(system.getId());
        sb.append("M").append(1 + system.getStacks().indexOf(this));

        return sb.toString();
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the preceding measure stack of this one, either in this system, or in the
     * preceding system, but still in the same page.
     *
     * @return the preceding measure stack, or null if not found in the page
     */
    public MeasureStack getPrecedingInPage ()
    {
        // Look in current part
        MeasureStack prevStack = getPreviousSibling();

        if (prevStack != null) {
            return prevStack;
        }

        SystemInfo precedingSystem = system.getPrecedingInPage();

        if (precedingSystem != null) {
            return precedingSystem.getLastStack();
        } else {
            return null;
        }
    }

    //--------------------//
    // getPreviousSibling //
    //--------------------//
    /**
     * Return the preceding measure stack within the same system.
     *
     * @return previous sibling measure stack in system, or null
     */
    public MeasureStack getPreviousSibling ()
    {
        final List<MeasureStack> stacks = system.getStacks();
        final int index = stacks.indexOf(this);

        if (index > 0) {
            return stacks.get(index - 1);
        }

        return null;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * @return the right
     */
    public int getRight ()
    {
        return right;
    }

    //------------//
    // getScoreId //
    //------------//
    /**
     * Report the character string of the score-based measure id.
     *
     * @param score the containing score
     * @return the (absolute) score-based measure id string
     */
    public String getScoreId (Score score)
    {
        return getScoreId(score.getMeasureIdOffset(system.getPage()));
    }

    //------------//
    // getScoreId //
    //------------//
    /**
     * Report the character string of the score-based measure id.
     *
     * @param pageMeasureIdOffset the measure ID offset for containing page
     * @return the (absolute) score-based measure id string
     */
    public String getScoreId (int pageMeasureIdOffset)
    {
        if (id == null) {
            return null;
        }

        return ((special == Special.SECOND_HALF) ? SECOND_HALF_PREFIX : "")
                       + (pageMeasureIdOffset + id);
    }

    //----------//
    // getSlots //
    //----------//
    /**
     * Report the ordered collection of slots.
     *
     * @return the collection of slots
     */
    public List<Slot> getSlots ()
    {
        return slots;
    }

    //------------------//
    // getSlotsDuration //
    //------------------//
    /**
     * Report the duration for this measure, as computed from content in terms of slots
     * and chords.
     * <p>
     * <b>NOTA</b>: if measure has no slot (case of a whole rest or case of an empty stack),
     * result is ZERO.
     *
     * @return the measure duration as computed on slots and chords
     */
    public Rational getSlotsDuration ()
    {
        Rational measureDur = Rational.ZERO;

        // Whole/multi rests are handled outside of slots
        for (Slot slot : slots) {
            if (slot.getTimeOffset() != null) {
                for (AbstractChordInter chord : slot.getChords()) {
                    Rational chordEnd = slot.getTimeOffset().plus(chord.getDuration());

                    if (chordEnd.compareTo(measureDur) > 0) {
                        measureDur = chordEnd;
                    }
                }
            }
        }

        return measureDur;
    }

    //-----------------------//
    // getStandardChordAbove //
    //-----------------------//
    /**
     * Retrieve the closest chord (head or rest) within staff above.
     *
     * @param point  the system-based location
     * @param xRange required abscissa range, or null
     * @return the most suitable chord, or null
     */
    public AbstractChordInter getStandardChordAbove (Point2D point,
                                                     Rectangle xRange)
    {
        Collection<AbstractChordInter> aboves = getStandardChordsAbove(point, xRange);

        if (!aboves.isEmpty()) {
            return getClosestChord(aboves, point);
        }

        return null;
    }

    //-----------------------//
    // getStandardChordBelow //
    //-----------------------//
    /**
     * Retrieve the closest chord (head or rest) within staff below.
     *
     * @param point  the system-based location
     * @param xRange required abscissa range, or null
     * @return the most suitable chord, or null
     */
    public AbstractChordInter getStandardChordBelow (Point2D point,
                                                     Rectangle xRange)
    {
        Collection<AbstractChordInter> belows = getStandardChordsBelow(point, xRange);

        if (!belows.isEmpty()) {
            return getClosestChord(belows, point);
        }

        return null;
    }

    //-------------------//
    // getStandardChords //
    //-------------------//
    /**
     * Report all the standard (i.e."non-small") chords in the stack.
     *
     * @return all standard chords in stack
     */
    public Set<AbstractChordInter> getStandardChords ()
    {
        Set<AbstractChordInter> stdChords = new LinkedHashSet<>();

        for (Measure measure : measures) {
            stdChords.addAll(measure.getStandardChords());
        }

        return stdChords;
    }

    //------------------------//
    // getStandardChordsAbove //
    //------------------------//
    /**
     * Report the set of standard chords whose 'head' is located in the staff above the
     * provided point.
     *
     * @param point  the provided point
     * @param xRange required abscissa range, or null
     * @return the (perhaps empty) set of chords
     */
    public Set<AbstractChordInter> getStandardChordsAbove (Point2D point,
                                                           Rectangle xRange)
    {
        Staff desiredStaff = getSystem().getStaffAtOrAbove(point);
        Set<AbstractChordInter> found = new LinkedHashSet<>();
        Measure measure = getMeasureAt(desiredStaff);

        if (measure != null) {
            for (AbstractChordInter chord : measure.getStandardChords()) {
                if (chord.getBottomStaff() == desiredStaff) {
                    if ((xRange == null) || (GeoUtil.xOverlap(chord.getBounds(), xRange) > 0)) {
                        Point head = chord.getHeadLocation();

                        if ((head != null) && (head.y < point.getY())) {
                            found.add(chord);
                        }
                    }
                }
            }
        }

        return found;
    }

    //------------------------//
    // getStandardChordsBelow //
    //------------------------//
    /**
     * Report the set of standard chords whose 'head' is located in the staff below the
     * provided point.
     *
     * @param point  the provided point
     * @param xRange required abscissa range, or null
     * @return the (perhaps empty) collection of chords
     */
    public Set<AbstractChordInter> getStandardChordsBelow (Point2D point,
                                                           Rectangle xRange)
    {
        Staff desiredStaff = getSystem().getStaffAtOrBelow(point);
        Set<AbstractChordInter> found = new LinkedHashSet<>();
        Measure measure = getMeasureAt(desiredStaff);

        if (measure != null) {
            for (AbstractChordInter chord : measure.getStandardChords()) {
                if (chord.getTopStaff() == desiredStaff) {
                    if ((xRange == null) || (GeoUtil.xOverlap(chord.getBounds(), xRange) > 0)) {
                        Point head = chord.getHeadLocation();

                        if ((head != null) && (head.y > point.getY())) {
                            found.add(chord);
                        }
                    }
                }
            }
        }

        return found;
    }

    //-----------------------//
    // getStandardHeadChords //
    //-----------------------//
    /**
     * Report all standard (not small) head chords in this stack.
     *
     * @return all non-small head chords in stack
     * @see #getHeadChords()
     */
    public Set<HeadChordInter> getStandardHeadChords ()
    {
        Set<HeadChordInter> headChords = new LinkedHashSet<>();

        for (Measure measure : measures) {
            headChords.addAll(measure.getStandardHeadChords());
        }

        return headChords;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * @return the system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    //------------------//
    // getTimeSignature //
    //------------------//
    /**
     * Report the potential time signature in this stack (whatever the staff).
     *
     * @return the stack time signature, or null if not found
     */
    public AbstractTimeInter getTimeSignature ()
    {
        for (Measure measure : measures) {
            AbstractTimeInter ts = measure.getTimeSignature();

            if (ts != null) {
                return ts;
            }
        }

        return null; // Not found
    }

    //------------//
    // getTuplets //
    //------------//
    /**
     * Report all tuplet candidates within the stack.
     * <p>
     * A containing part cannot be safely assigned to a tuplet before its embraced chords are known,
     * therefore tuplet candidates may be kept in stack for a while before being dispatched to their
     * precise measure.
     *
     * @return all tuplet candidates
     */
    public Set<TupletInter> getTuplets ()
    {
        Set<TupletInter> all = new LinkedHashSet<>();

        for (Measure measure : measures) {
            all.addAll(measure.getTuplets());
        }

        // Add the non-measure-assigned stackTuplets
        all.addAll(stackTuplets);

        return all;
    }

    //-----------//
    // getVoices //
    //-----------//
    /**
     * Report the sequence of voices in stack
     *
     * @return the stack voices
     */
    public List<Voice> getVoices ()
    {
        List<Voice> stackVoices = new ArrayList<>();

        for (Measure measure : measures) {
            stackVoices.addAll(measure.getVoices());
        }

        return Collections.unmodifiableList(stackVoices);
    }

    //--------------------//
    // getWholeRestChords //
    //--------------------//
    /**
     * Report all whole rest-chords in stack.
     *
     * @return all whole rest chords in stack
     */
    public Set<AbstractChordInter> getWholeRestChords ()
    {
        final Set<AbstractChordInter> set = new LinkedHashSet<>();

        for (Measure measure : measures) {
            for (RestChordInter chord : measure.getRestChords()) {
                final List<Inter> members = chord.getMembers();

                if (!members.isEmpty() && (members.get(0).getShape() == Shape.WHOLE_REST)) {
                    set.add(chord);
                }
            }
        }

        return set;
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the abscissa offset since stack left border of the provided point.
     *
     * @param point the provided point
     * @return xOffset of the point WRT stack start
     */
    public double getXOffset (Point2D point)
    {
        return getXOffset(point, system.getStavesAround(point));
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the precise abscissa offset since stack left border of the provided point.
     *
     * @param point the provided point
     * @param staff the staff of reference
     * @return xOffset of the point WRT stack left side
     */
    public double getXOffset (Point2D point,
                              Staff staff)
    {
        // Extrapolate, using skew, from single staff
        final Skew skew = system.getSkew();
        final Measure measure = getMeasureAt(staff);
        final Point2D left = measure.getSidePoint(HorizontalSide.LEFT, staff);

        return skew.deskewed(point).getX() - skew.deskewed(left).getX();
    }

    //------------//
    // getXOffset //
    //------------//
    /**
     * Report the precise abscissa offset since stack left border of the provided point.
     *
     * @param point        the provided point
     * @param stavesAround one or two staves that surround the provided point
     * @return xOffset of the point WRT stack left side
     */
    public double getXOffset (Point2D point,
                              List<Staff> stavesAround)
    {
        Staff staff1 = stavesAround.get(0);
        Measure measure1 = getMeasureAt(staff1);
        Point2D left1 = measure1.getSidePoint(HorizontalSide.LEFT, staff1);

        if (stavesAround.size() > 1) {
            // Interpolate between staff above & staff below
            LineInfo line1 = staff1.getLines().get(staff1.getLines().size() / 2);
            double y1 = line1.yAt(point.getX());
            double offset1 = point.getX() - left1.getX();

            Staff staff2 = stavesAround.get(1);
            Measure measure2 = getMeasureAt(staff2);
            Point2D left2 = measure2.getSidePoint(HorizontalSide.LEFT, staff2);
            LineInfo line2 = staff2.getLines().get(staff2.getLines().size() / 2);
            double y2 = line2.yAt(point.getX());
            double offset2 = point.getX() - left2.getX();

            return offset1 + (((offset2 - offset1) * (point.getY() - y1)) / (y2 - y1));
        } else {
            return getXOffset(point, staff1);
        }
    }

    //------------//
    // isAbnormal //
    //------------//
    /**
     * Report whether this stack is abnormal.
     *
     * @return the abnormal status
     */
    public boolean isAbnormal ()
    {
        return abnormal;
    }

    //-------------//
    // setAbnormal //
    //-------------//
    /**
     * Mark this stack as being abnormal or not.
     *
     * @param abnormal new value
     */
    public void setAbnormal (boolean abnormal)
    {
        this.abnormal = abnormal;
    }

    //--------------//
    // isCautionary //
    //--------------//
    /**
     * Report whether this measure stack is a cautionary stack (just changes in CKT).
     *
     * @return true if measure is cautionary
     */
    public boolean isCautionary ()
    {
        return special == Special.CAUTIONARY;
    }

    //-------------//
    // isFirstHalf //
    //-------------//
    /**
     * Report whether this measure stack is a repeat first half.
     *
     * @return true if measure is firstHalf
     */
    public boolean isFirstHalf ()
    {
        return special == Special.FIRST_HALF;
    }

    //------------//
    // isImplicit //
    //------------//
    /**
     * Report whether this measure stack is implicit (pickup or repeat last half).
     *
     * @return true if measure is pickup or secondHalf
     */
    public boolean isImplicit ()
    {
        return (special == Special.PICKUP) || (special == Special.SECOND_HALF);
    }

    //----------//
    // isRepeat //
    //----------//
    /**
     * Tell whether the stack has a repeat sign on provided side.
     *
     * @param side horizontal side
     * @return true if so
     */
    public boolean isRepeat (HorizontalSide side)
    {
        return (repeats != null) && repeats.contains(side);
    }

    //----------------//
    // mergeWithBelow //
    //----------------//
    /**
     * Merge this stack with the stack below (due to system merge).
     *
     * @param stackBelow the measure stack below (in the former system below)
     * @see #unmergeWith(MeasureStack)
     */
    public void mergeWithBelow (MeasureStack stackBelow)
    {
        // left, right
        left = Math.min(left, stackBelow.left);
        right = Math.max(right, stackBelow.right);

        // special?
        //
        // repeats
        for (HorizontalSide side : HorizontalSide.values()) {
            if (stackBelow.isRepeat(side)) {
                addRepeat(side);
            }
        }

        // measures
        for (Measure measure : stackBelow.measures) {
            measure.setStack(this);
        }

        measures.addAll(stackBelow.measures);

        // stackTuplets
        stackTuplets.addAll(stackBelow.stackTuplets);

        // slots?
        // expectedDuration?
        // duration?
        // excess?
        // abnormal?
        // (no, done by reprocessPageRhythm)
    }

    //----------------//
    // mergeWithRight //
    //----------------//
    /**
     * Merge this stack with the content of the following stack on the right.
     *
     * @param rightStack the following measure stack
     */
    public void mergeWithRight (MeasureStack rightStack)
    {
        // Merge the measures, part by part
        for (int ip = 0; ip < rightStack.measures.size(); ip++) {
            measures.get(ip).mergeWithRight(rightStack.measures.get(ip));
        }

        // Merge the stacks data
        right = rightStack.right;

        if (rightStack.actualDuration != null) {
            actualDuration = (actualDuration == null) ? rightStack.actualDuration
                    : actualDuration.plus(rightStack.actualDuration);
        }

        // Merge the repeat info
        if (rightStack.isRepeat(RIGHT)) {
            this.addRepeat(RIGHT);
        }

        // Beware, merged slots must have their stack & xOffset updated accordingly
        slots.addAll(rightStack.slots);

        for (Slot slot : slots) {
            slot.setStack(this);
        }

        //TODO: what about the now "inside" barline (which may have a repeat sign) ???
    }

    //------------//
    // printVoices//
    //------------//
    /**
     * Print the voices of this measure stack on standard output.
     *
     * @param title a potential title for this printout, or null
     */
    public void printVoices (String title)
    {
        try {
            StringBuilder sb = new StringBuilder();

            // Title
            if (title != null) {
                sb.append(title);
            }

            // Measure
            sb.append(this);

            // Slot headers
            if (!slots.isEmpty()) {
                sb.append("\n    ");

                for (Slot slot : slots) {
                    if (slot.getTimeOffset() != null) {
                        sb.append("|").append(String.format("%-8s", slot.getTimeOffset()));
                    }
                }

                sb.append("|").append(getSlotsDuration());
            }

            for (Measure measure : measures) {
                sb.append("\n--- P").append(measure.getPart().getId());
                List<Voice> voices = new ArrayList<>(measure.getVoices());
                Collections.sort(voices, Voices.byId);

                for (Voice voice : voices) {
                    sb.append("\n").append(voice.toStrip());
                }
            }

            logger.info(sb.toString());
        } catch (Exception ex) {
            logger.warn("Error printing voices for {} {}", this, ex.toString(), ex);
        }
    }

    //-------------//
    // removeInter //
    //-------------//
    /**
     * Remove an inter from the stack.
     *
     * @param inter the inter to remove
     */
    public void removeInter (Inter inter)
    {
        if (inter.isVip()) {
            logger.info("VIP removeInter {} from [}", inter, this);
        }

        final Part part = inter.getPart();

        if (part != null) {
            int partIndex = system.getParts().indexOf(part);
            Measure measure = measures.get(partIndex);
            measure.removeInter(inter);
        }

        if (inter instanceof TupletInter) {
            stackTuplets.remove((TupletInter) inter);
        }
    }

    //---------------//
    // removeMeasure //
    //---------------//
    /**
     * Remove a measure from the stack
     *
     * @param measure the measure to remove
     */
    public void removeMeasure (Measure measure)
    {
        measures.remove(measure);
    }

    //------------//
    // renderArea //
    //------------//
    /**
     * Render the measure stack area with provided color.
     *
     * @param g     graphics context
     * @param color provided color
     */
    public void renderArea (Graphics2D g,
                            Color color)
    {
        // Save some drawing
        final Rectangle clip = g.getClipBounds();
        final Rectangle systemRect = system.getBounds();

        if ((clip != null) && !clip.intersects(systemRect)) {
            return;
        }

        g.setColor(color);

        Sheet sheet = system.getSheet();

        // Most inters from first measure
        Staff firstStaff = system.getFirstStaff();
        Measure firstMeasure = getMeasureAt(firstStaff);
        LineInfo firstLine = firstStaff.getFirstLine();
        int top = Math.min(firstLine.yAt(left), firstLine.yAt(right));

        for (Inter inter : firstMeasure.getTimingInters()) {
            Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                top = Math.min(top, bounds.y);
            }
        }

        // Most inters from last measure
        Staff lastStaff = system.getLastStaff();
        Measure lastMeasure = getMeasureAt(lastStaff);
        LineInfo lastLine = lastStaff.getLastLine();
        int bottom = Math.max(lastLine.yAt(left), lastLine.yAt(right));

        for (Inter inter : lastMeasure.getTimingInters()) {
            Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                bottom = Math.max(bottom, bounds.y + bounds.height);
            }
        }

        // Inters from stack tuplets
        for (Inter inter : stackTuplets) {
            Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                top = Math.min(top, bounds.y);
                bottom = Math.max(bottom, bounds.y + bounds.height);
            }
        }

        g.fill(new Rectangle(left, top, right - left + 1, bottom - top + 1));
    }

    //-------------//
    // resetRhythm //
    //-------------//
    /**
     * Reset rhythm info at stack level.
     * <p>
     * This does not reset rhythm info at measure level.
     *
     * @see Measure#resetRhythm()
     */
    public void resetRhythm ()
    {
        setAbnormal(false);
        excess = null;
        slots.clear();
        setActualDuration(null);
    }

    //---------------//
    // setCautionary //
    //---------------//
    /**
     * Flag stack as cautionary.
     */
    public void setCautionary ()
    {
        special = Special.CAUTIONARY;
    }

    //--------------//
    // setFirstHalf //
    //--------------//
    /**
     * Flag stack as first half.
     */
    public void setFirstHalf ()
    {
        special = Special.FIRST_HALF;
    }

    //-----------//
    // setPickup //
    //-----------//
    /**
     * Flag stack as pickup.
     */
    public void setPickup ()
    {
        special = Special.PICKUP;
    }

    //---------------//
    // setSecondHalf //
    //---------------//
    /**
     * Flag stack as second half.
     */
    public void setSecondHalf ()
    {
        special = Special.SECOND_HALF;
    }

    //---------//
    // shorten //
    //---------//
    /**
     * Flag this measure stack as partial (shorter than expected duration).
     *
     * @param shortening how much the measure stack duration is to be reduced
     */
    public void shorten (Rational shortening)
    {
        //        // Remove any final forward mark consistent with the shortening
        //        for (Voice voice : voices) {
        //            Rational duration = voice.getTermination();
        //
        //            if (duration != null) {
        //                //                if (duration.equals(shortening)) {
        //                //                    if (!voice.isWhole()) {
        //                //                        // Remove the related mark
        //                //                        AbstractChordInter chord = voice.getLastChord();
        //                //
        //                //                        if (chord != null) {
        //                //                            int nbMarks = chord.getMarks().size();
        //                //
        //                //                            if (nbMarks > 0) {
        //                //                                Mark mark = chord.getMarks().get(nbMarks - 1);
        //                //                                logger.debug(
        //                //                                        "{} Removing final forward: {}",
        //                //                                        getContextString(),
        //                //                                        (Rational) mark.getData());
        //                //                                chord.getMarks().remove(mark);
        //                //                            } else {
        //                //                                chord.addError("No final mark to remove in a partial measure");
        //                //
        //                //                                return;
        //                //                            }
        //                //                        } else {
        //                //                            addError("No final chord in " + voice);
        //                //
        //                //                            return;
        //                //                        }
        //                //                    }
        //                //                } else {
        //                //                    addError(
        //                //                            "Non consistent partial measure shortening:" + shortening.opposite() + " "
        //                //                            + voice + ": " + duration.opposite());
        //                //
        //                //                    return;
        //                //                }
        //            }
        //        }
    }

    //----------------//
    // splitAtBarline //
    //----------------//
    /**
     * Split this stack at the provided system-level barlines.
     * <p>
     * We create a new stack on left side of old stack which will become the left stack.
     * We update old (right) stack on its left side.
     *
     * @param systemBarline column of PartBarline, one per system part
     * @return the newly created stack on the left
     */
    public MeasureStack splitAtBarline (List<PartBarline> systemBarline)
    {
        final int stackIndex = system.getStacks().indexOf(this);
        final MeasureStack leftStack = new MeasureStack(system);
        leftStack.left = this.left;
        leftStack.right = 0;
        this.left = Integer.MAX_VALUE;

        final List<Part> systemParts = system.getParts();

        for (int partIndex = 0; partIndex < systemParts.size(); partIndex++) {
            final Part part = systemParts.get(partIndex);
            final PartBarline partBarline = systemBarline.get(partIndex);
            Map<Staff, Integer> xRefs = new HashMap<>();

            for (Staff staff : part.getStaves()) {
                final int xRef = partBarline.getRightX(part, staff);
                xRefs.put(staff, xRef);
                leftStack.right = Math.max(leftStack.right, xRef);
                this.left = Math.min(this.left, xRef);
            }

            final Measure measure = getMeasureAt(part);
            final Measure leftMeasure = measure.splitAt(xRefs);
            leftMeasure.setStack(leftStack);
            leftMeasure.setRightPartBarline(partBarline);
            leftStack.measures.add(leftMeasure);

            // Insert leftMeasure into part, just before the old (right) measure
            part.addMeasure(stackIndex, leftMeasure);
        }

        // Stack tuplets (not in any measure yet)
        if (!stackTuplets.isEmpty()) {
            for (Iterator<TupletInter> it = stackTuplets.iterator(); it.hasNext();) {
                TupletInter tuplet = it.next();

                if (tuplet.getCenter().x <= leftStack.right) {
                    leftStack.addInter(tuplet);
                    it.remove();
                }
            }
        }

        // Insert leftStack into system, just before this old (right) stack
        system.addStack(stackIndex, leftStack);

        return leftStack;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(getPageId());

        return sb.toString();
    }

    //-------------//
    // unmergeWith //
    //-------------//
    /**
     * Un-merge this stack with the provided stack below (due to undo of system merge).
     *
     * @param stackBelow the measure stack below (in the former system below)
     * @see #mergeWithBelow(MeasureStack)
     */
    public void unmergeWith (MeasureStack stackBelow)
    {
        // measures
        measures.removeAll(stackBelow.measures);

        for (Measure measure : stackBelow.measures) {
            measure.setStack(stackBelow);
        }

        // left, right
        left = Integer.MAX_VALUE;
        right = Integer.MIN_VALUE;

        for (Measure measure : measures) {
            for (Staff staff : measure.getPart().getStaves()) {
                left = Math.min(left, measure.getAbscissa(LEFT, staff));
                right = Math.max(right, measure.getAbscissa(RIGHT, staff));
            }
        }

        // special?
        //
        // repeats
        computeRepeats();

        // stackTuplets
        stackTuplets.removeAll(stackBelow.stackTuplets);

        // slots?
        // expectedDuration?
        // duration?
        // excess?
        // abnormal?
        // (no, done by reprocessPageRhythm)
    }

    /**
     * All special kinds of measures.
     */
    public enum Special
    {

        PICKUP,
        FIRST_HALF,
        SECOND_HALF,
        CAUTIONARY
    }

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        try {
            Trimmable.afterMarshal(this);
        } catch (Exception ex) {
            logger.error("Error afterMarshal", ex);
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        try {
            Trimmable.beforeMarshal(this);
        } catch (Exception ex) {
            logger.error("Error beforeMarshal", ex);
        }
    }
}
