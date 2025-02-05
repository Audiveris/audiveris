//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a g e                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.score;

import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.SlurInter;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Class <code>Page</code> represents a page in the score hierarchy, and corresponds to a
 * {@link Sheet} or a portion of it.
 * <p>
 * One or several Page instances compose a {@link Score}.
 * <p>
 * NOTA: During the transcription process, a page has no fixed link to its containing score since
 * the set of scores may evolve while sheets/pages are being transcribed in any order.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Page
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Page.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** This is the rank of this page, counted from 1 in the containing sheet. */
    @XmlAttribute(name = "id")
    private final int id;

    /** Number of measures counted in this page. */
    @XmlAttribute(name = "measure-count")
    private Integer measureCount;

    /** This is the (sub)list of systems for this page, within the sheet systems. */
    @XmlElement(name = "system")
    private List<SystemInfo> systems;

    // Transient data
    //---------------

    /** Soft reference to this page. */
    private PageRef pageRef;

    /** Containing (physical) sheet. */
    @Navigable(false)
    private Sheet sheet;

    /** Containing (logical) score. */
    @Navigable(false)
    private Score score;

    /** Id of page first system in sheet. */
    private Integer firstSystemId;

    /** Id of page last system in sheet. */
    private Integer lastSystemId;

    /** Greatest duration divisor (in this page). */
    private Integer durationDivisor;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private Page ()
    {
        id = 0;
    }

    /**
     * Creates a new Page object.
     *
     * @param sheet         containing sheet
     * @param id            id for the page (1-based number within sheet)
     * @param firstSystemId id of first system in sheet, if any
     */
    public Page (Sheet sheet,
                 int id,
                 Integer firstSystemId)
    {
        this.id = id;
        this.firstSystemId = firstSystemId;

        initTransients(sheet);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------------//
    // checkPageCrossTies //
    //--------------------//
    /**
     * Within the systems of this page, check tie status for cross-system slurs.
     */
    public void checkPageCrossTies ()
    {
        for (SystemInfo system : systems.subList(1, systems.size())) {
            // Examine every part in sequence
            for (Part part : system.getParts()) {
                final List<SlurInter> leftExtended = part.getSlurs(SlurInter.isBeginningExtended);

                for (SlurInter slur : leftExtended) {
                    final SlurInter prevSlur = slur.getExtension(LEFT);

                    if (!prevSlur.isRemoved()) {
                        slur.checkCrossTie(prevSlur);
                    }
                }
            }
        }
    }

    //------------------------//
    // computeDurationDivisor //
    //------------------------//
    /**
     * Browse this page to determine the global page duration divisor.
     * <p>
     * TODO: Here we retrieve divisor for the page. We could work on each part only.
     *
     * @return the page duration divisor
     */
    private int computeDurationDivisor ()
    {
        try {
            final SortedSet<Rational> durations = new TreeSet<>();

            // Collect duration values for each standard chord in this page
            for (SystemInfo system : getSystems()) {
                for (MeasureStack stack : system.getStacks()) {
                    for (AbstractChordInter chord : stack.getStandardChords()) {
                        try {
                            final Rational duration = chord.isMeasureRest() ? stack
                                    .getExpectedDuration() : chord.getDuration();

                            if (duration != null) {
                                durations.add(duration);
                            }
                        } catch (Exception ex) {
                            logger.warn(
                                    getClass().getSimpleName() + " Error visiting " + chord,
                                    ex);
                        }
                    }
                }
            }

            // Compute greatest duration divisor for the page
            Rational[] durationArray = durations.toArray(new Rational[durations.size()]);
            Rational divisor = Rational.gcd(durationArray);
            logger.debug("durations={} divisor={}", Arrays.deepToString(durationArray), divisor);

            return divisor.den;
        } catch (Exception ex) {
            logger.warn(getClass().getSimpleName() + " Error visiting " + this, ex);

            return 0;
        }
    }

    //---------------------//
    // computeMeasureCount //
    //---------------------//
    /**
     * Compute the number of (vertical) measures in the page.
     */
    public void computeMeasureCount ()
    {
        int count = 0;

        for (SystemInfo system : systems) {
            count += system.getFirstPart().getMeasures().size();

            if (system.getLastStack().isCautionary()) {
                count--;
            }
        }

        measureCount = count;
    }

    //--------------------//
    // connectOrphanSlurs //
    //--------------------//
    /**
     * Within the systems of this page, retrieve the connections between the orphan
     * slurs at the beginning of each system and the orphan slurs at the end of the
     * previous system if any.
     * <p>
     * Orphan slurs that don't connect (and are not manual) are removed from their SIG.
     */
    public void connectOrphanSlurs ()
    {
        for (SystemInfo system : systems.subList(1, systems.size())) {
            // Examine every part in sequence
            for (Part part : system.getParts()) {
                final List<SlurInter> orphans = part.getSlurs(SlurInter.isBeginningOrphan);

                // Connect to ending orphans in preceding system/part (if such part exists)
                final Part precPart = part.getPrecedingInPage();

                if (precPart != null) {
                    final List<SlurInter> precOrphans = precPart.getSlurs(SlurInter.isEndingOrphan);

                    // Links: Slur -> prevSlur
                    final Map<SlurInter, SlurInter> links = part.getCrossSlurLinks(precPart);

                    // Apply the links possibilities
                    for (Map.Entry<SlurInter, SlurInter> entry : links.entrySet()) {
                        final SlurInter slur = entry.getKey();
                        final SlurInter prevSlur = entry.getValue();

                        slur.setExtension(LEFT, prevSlur);
                        prevSlur.setExtension(RIGHT, slur);
                    }

                    orphans.removeAll(links.keySet());
                    precOrphans.removeAll(links.values());
                    SlurInter.discardOrphans(precOrphans, RIGHT);
                }

                SlurInter.discardOrphans(orphans, LEFT);
            }
        }
    }

    //-------------------//
    // dumpMeasureCounts //
    //-------------------//
    /**
     * Log the detailed number of measures in the page.
     */
    public void dumpMeasureCounts ()
    {
        int count = 0;
        StringBuilder sb = new StringBuilder();

        for (SystemInfo sys : systems) {
            Part part = sys.getFirstPart();

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(part.getMeasures().size()).append(" in system#").append(sys.getId());
            count += part.getMeasures().size();
        }

        StringBuilder msg = new StringBuilder();
        msg.append(count);
        msg.append(" raw measure");

        if (count > 1) {
            msg.append('s');
        }

        msg.append(": [").append(sb).append("]");

        logger.info("{}", msg.toString());
    }

    //--------------------//
    // getDurationDivisor //
    //--------------------//
    /**
     * Report the common divisor used for this page when simplifying the durations.
     *
     * @return the computed divisor (GCD), or null if not computable
     */
    public Integer getDurationDivisor ()
    {
        if (durationDivisor == null) {
            durationDivisor = computeDurationDivisor();
        }

        return durationDivisor;
    }

    //----------------//
    // getFirstSystem //
    //----------------//
    /**
     * Report the first system in the page.
     *
     * @return the first system
     */
    public SystemInfo getFirstSystem ()
    {
        if (systems.isEmpty()) {
            return null;
        }

        return systems.get(0);
    }

    /**
     * @return the firstSystemId
     */
    public Integer getFirstSystemId ()
    {
        return firstSystemId;
    }

    //---------------------//
    // getFollowingInScore //
    //---------------------//
    /**
     * Report the following page of this one within the score.
     *
     * @param score the containing score
     * @return the following page, or null if none
     */
    public Page getFollowingInScore (Score score)
    {
        if (score != null) {
            return score.getFollowingPage(this);
        }

        return null;
    }

    /**
     * Report the page id, which is the rank of this page, counted from 1 in containing sheet.
     *
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //---------------//
    // getLastSystem //
    //---------------//
    /**
     * Report the last system in the page.
     *
     * @return the last system
     */
    public SystemInfo getLastSystem ()
    {
        if (systems.isEmpty()) {
            return null;
        }

        return systems.get(systems.size() - 1);
    }

    /**
     * @return the lastSystemId
     */
    public Integer getLastSystemId ()
    {
        return lastSystemId;
    }

    //-----------------//
    // getMeasureCount //
    //-----------------//
    /**
     * Report the number of (vertical) measures in this page.
     *
     * @return the number of page measures
     */
    public int getMeasureCount ()
    {
        return measureCount;
    }

    //---------------------//
    // getPrecedingInScore //
    //---------------------//
    /**
     * Report the preceding page of this one within the score.
     *
     * @param score the containing score
     * @return the preceding page, or null if none
     */
    public Page getPrecedingInScore (Score score)
    {
        if (score != null) {
            return score.getPrecedingPage(this);
        }

        return null;
    }

    //--------//
    // getRef //
    //--------//
    /**
     * Report the soft reference to this page.
     *
     * @return the corresponding PageRef
     */
    public PageRef getRef ()
    {
        if (pageRef == null) {
            pageRef = sheet.getStub().getPageRefs().get(id - 1);
        }

        return pageRef;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the containing score if any.
     *
     * @return the containing score, perhaps still null
     */
    public Score getScore ()
    {
        if (score == null) {
            score = sheet.getStub().getBook().getScore(this);
        }

        return score;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the related sheet entity.
     *
     * @return the related sheet, or null if none
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //--------------------//
    // getSystemPartsById //
    //--------------------//
    /**
     * Report the list of (system physical) parts that exhibit the desired ID.
     *
     * @param id the desired ID
     * @return the parts with this ID
     */
    public List<Part> getSystemPartsById (int id)
    {
        List<Part> parts = new ArrayList<>();

        for (SystemInfo system : getSystems()) {
            for (Part part : system.getParts()) {
                if (part.getId() == id) {
                    parts.add(part);

                    break;
                }
            }
        }

        return parts;
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report the sequence of systems in that page.
     *
     * @return the list of systems
     */
    public List<SystemInfo> getSystems ()
    {
        return systems;
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize transient data after construction or unmarshalling.
     *
     * @param sheet the containing sheet
     */
    public final void initTransients (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //-----------------//
    // isMovementStart //
    //-----------------//
    /**
     * Report whether this page starts a movement.
     *
     * @return true if so
     */
    public boolean isMovementStart ()
    {
        return getRef().isMovementStart();
    }

    //----------------//
    // numberMeasures //
    //----------------//
    /**
     * Assign a very basic sequential id to each contained stack.
     */
    public void numberMeasures ()
    {
        int systemOffset = 0;

        for (SystemInfo system : systems) {
            List<MeasureStack> stacks = system.getStacks();

            for (int im = 0; im < stacks.size(); im++) {
                int mid = systemOffset + im + 1;
                MeasureStack stack = stacks.get(im);
                stack.setIdValue(mid);
            }

            systemOffset += stacks.size();
        }

        // Very temporary raw values
        setDeltaMeasureId(systemOffset);
        computeMeasureCount();
    }

    //-------------//
    // numberParts //
    //-------------//
    /**
     * (Re-)assign sequential numbers to parts within each system.
     */
    public void numberParts ()
    {
        for (SystemInfo system : systems) {
            system.numberParts();
        }
    }

    //--------------//
    // removeSystem //
    //--------------//
    /**
     * Remove the provided system from this page.
     *
     * @param system the system to remove
     * @see #unremoveSystem
     */
    public void removeSystem (SystemInfo system)
    {
        systems.remove(system);

        if (!systems.isEmpty()) {
            firstSystemId = getFirstSystem().getId();
            lastSystemId = getLastSystem().getId();
        }
    }

    //----------------------//
    // resetDurationDivisor //
    //----------------------//
    /**
     * Nullify the duration divisor (before a re-computation).
     */
    public void resetDurationDivisor ()
    {
        durationDivisor = null;
    }

    //-------------------//
    // setDeltaMeasureId //
    //-------------------//
    /**
     * Assign the progression of measure IDs within this page.
     *
     * @param deltaMeasureId the deltaMeasureId to set
     */
    public void setDeltaMeasureId (Integer deltaMeasureId)
    {
        getRef().setDeltaMeasureId(deltaMeasureId);
    }

    //------------------//
    // setFirstSystemId //
    //------------------//
    /**
     * @param firstSystemId the firstSystemId to set
     */
    public void setFirstSystemId (Integer firstSystemId)
    {
        this.firstSystemId = firstSystemId;
    }

    //-----------------//
    // setLastSystemId //
    //-----------------//
    /**
     * @param lastSystemId the lastSystemId to set
     */
    public void setLastSystemId (Integer lastSystemId)
    {
        this.lastSystemId = lastSystemId;
    }

    //---------------------//
    // setLastTimeRational //
    //---------------------//
    /**
     * Assign the last time rational value within this page.
     *
     * @param lastTimeRational the last time rational value to set
     */
    public void setLastTimeRational (TimeRational lastTimeRational)
    {
        getRef().setLastTimeRational(lastTimeRational);
    }

    //----------//
    // setScore //
    //----------//
    /**
     * Assign the containing score.
     *
     * @param score the score to set
     */
    public void setScore (Score score)
    {
        this.score = score;
    }

    //----------------//
    // setSystemsFrom //
    //----------------//
    /**
     * Using IDs of first and last page systems if any, register the proper (sub-)list
     * of systems.
     *
     * @param sheetSystems the sheet whole list of systems
     */
    public void setSystemsFrom (List<SystemInfo> sheetSystems)
    {
        // Define proper indices
        int first = (firstSystemId != null) ? (firstSystemId - 1) : 0;
        int last = (lastSystemId != null) ? (lastSystemId - 1) : (sheetSystems.size() - 1);
        systems = new ArrayList<>(sheetSystems.subList(first, last + 1));
    }

    //------------------//
    // simpleDurationOf //
    //------------------//
    /**
     * Export a duration to its simplest form, based on the greatest duration divisor of
     * the page.
     *
     * @param value the raw duration
     * @return the simple duration expression, in the param of proper divisions
     */
    public int simpleDurationOf (Rational value)
    {
        return value.num * (getDurationDivisor() / value.den);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder("{Page");

        if ((sheet != null) && (sheet.getStub() != null)) {
            sb.append('#').append(sheet.getStub().getNumber());
        }

        if (id != 0) {
            sb.append('.').append(id);
        }

        sb.append('}');

        return sb.toString();
    }

    //----------------//
    // unremoveSystem //
    //----------------//
    /**
     * Un-remove the provided system into this page.
     *
     * @param system the system to re-insert
     * @see #removeSystem
     */
    public void unremoveSystem (SystemInfo system)
    {
        boolean inserted = false;

        for (int i = 0; i < systems.size(); i++) {
            SystemInfo s = systems.get(i);

            if (s.getId() == system.getId()) {
                systems.add(i, system);
                inserted = true;

                break;
            }
        }

        if (!inserted) {
            systems.add(system);
        }

        firstSystemId = getFirstSystem().getId();
        lastSystemId = getLastSystem().getId();
    }
}
