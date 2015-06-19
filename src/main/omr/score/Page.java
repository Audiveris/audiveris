//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a g e                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.math.Rational;

import omr.sheet.Part;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.MeasureStack;

import omr.sig.inter.ChordInter;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code Page} represents a page in the score hierarchy, and corresponds to a
 * (part of) {@link Sheet}.
 * <p>
 * One or several Page instances compose a {@link Score}. But a page has no fixed link to its
 * containing score since the set of scores may evolve while sheets/pages are transcribed in any
 * order.
 *
 * @author Hervé Bitteur
 */
public class Page
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Page.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing (physical) sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Page ID. */
    private String id;

    /** Does this page start a movement?. */
    private boolean movementStart;

    /** LogicalPart list for the page. */
    private List<LogicalPart> logicalParts;

    /** Number of measures in this page. */
    private Integer measureCount;

    /** Progression of measure id within this page. */
    private Integer deltaMeasureId;

    /** Id of first system in sheet, if any. */
    private Integer firstSystemId;

    /** Id of last system in sheet, if any. */
    private Integer lastSystemId;

    /** (Sub)list of systems, within sheet systems. */
    private List<SystemInfo> systems;

    /** Greatest duration divisor (in this page). */
    private Integer durationDivisor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Page object.
     *
     * @param sheet         the containing sheet
     * @param firstSystemId id of first system in sheet, if any
     */
    public Page (Sheet sheet,
                 Integer firstSystemId)
    {
        this.sheet = sheet;
        this.firstSystemId = firstSystemId;

        // Define id
        computeId();
    }

    //~ Methods ------------------------------------------------------------------------------------
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

            if (system.getLastMeasureStack().isCautionary()) {
                count--;
            }
        }

        measureCount = count;
    }

    //-------------------//
    // dumpMeasureCounts //
    //-------------------//
    /**
     * Log the detailed number of measures in the score.
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

            sb.append(part.getMeasures().size()).append(" in ").append(sys.idString());
            count += part.getMeasures().size();
        }

        StringBuilder msg = new StringBuilder();
        msg.append(count);
        msg.append(" raw measure");

        if (count > 1) {
            msg.append('s');
        }

        msg.append(": [").append(sb).append("]");

        logger.info("{}{}", sheet.getLogPrefix(), msg.toString());
    }

    //-------------------//
    // getDeltaMeasureId //
    //-------------------//
    /**
     * Report the progression of measure IDs within this page.
     *
     * @return the deltaMeasureId
     */
    public Integer getDeltaMeasureId ()
    {
        return deltaMeasureId;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the dimension of the sheet/page.
     * TODO: check whether this is relevant (used only in PartwiseBuilder)
     *
     * @return the page/sheet dimension in pixels
     */
    @Deprecated
    public Dimension getDimension ()
    {
        return new Dimension(sheet.getWidth(), sheet.getHeight());
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

    //-------//
    // getId //
    //-------//
    /**
     * @return the id
     */
    public String getId ()
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
    // getLogicalParts //
    //-----------------//
    /**
     * Report the page list of logical parts.
     *
     * @return partList the list of parts
     */
    public List<LogicalPart> getLogicalParts ()
    {
        return logicalParts;
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
            List<Page> pages = score.getPages();
            int index = pages.indexOf(this);

            if (index > 0) {
                return pages.get(index - 1);
            }
        }

        return null;
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

    //-----------------//
    // isMovementStart //
    //-----------------//
    /**
     * @return the movementStart
     */
    public boolean isMovementStart ()
    {
        return movementStart;
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
            List<MeasureStack> stacks = system.getMeasureStacks();

            for (int im = 0; im < stacks.size(); im++) {
                int mid = systemOffset + im + 1;
                MeasureStack stack = stacks.get(im);
                stack.setIdValue(mid);
            }

            systemOffset += system.getMeasureStacks().size();
        }

        // Very temporary raw values
        setDeltaMeasureId(systemOffset);
        computeMeasureCount();
    }

    //----------------------//
    // resetDurationDivisor //
    //----------------------//
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
        this.deltaMeasureId = deltaMeasureId;
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
        computeId();
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
        computeId();
    }

    //-----------------//
    // setLogicalParts //
    //-----------------//
    /**
     * Assign a part list valid for the page.
     *
     * @param logicalParts the list of logical parts
     */
    public void setLogicalParts (List<LogicalPart> logicalParts)
    {
        this.logicalParts = logicalParts;
    }

    //------------------//
    // setMovementStart //
    //------------------//
    /**
     * @param movementStart the movementStart to set
     */
    public void setMovementStart (boolean movementStart)
    {
        this.movementStart = movementStart;
    }

    //------------//
    // setSystems //
    //------------//
    /**
     * Using IDs of first and last page systems if any, register the proper (sub-)list
     * of systems.
     *
     * @param sheetSystems the sheet whole list of systems
     */
    public void setSystems (List<SystemInfo> sheetSystems)
    {
        // Define proper indices
        int first = (firstSystemId != null) ? (firstSystemId - 1) : 0;
        int last = (lastSystemId != null) ? (lastSystemId - 1) : (sheetSystems.size() - 1);
        systems = sheetSystems.subList(first, last + 1);
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
        return "{Page " + id + "}";
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
            final SortedSet<Rational> durations = new TreeSet<Rational>();

            // Collect duration values for each chord in this page
            for (SystemInfo system : getSystems()) {
                for (MeasureStack stack : system.getMeasureStacks()) {
                    for (ChordInter chord : stack.getChords()) {
                        try {
                            final Rational duration = chord.isWholeRest()
                                    ? stack.getExpectedDuration()
                                    : chord.getDuration();

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

    //-----------//
    // computeId //
    //-----------//
    /**
     * Build the ID string for this page.
     */
    private void computeId ()
    {
        StringBuilder sb = new StringBuilder(sheet.getId());

        if (firstSystemId != null) {
            sb.append("-F").append(firstSystemId);
        }

        if (lastSystemId != null) {
            sb.append("-L").append(lastSystemId);
        }

        id = sb.toString();
    }
}
