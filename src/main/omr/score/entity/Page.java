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
package omr.score.entity;

import omr.score.Score;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Measure;
import omr.sheet.MeasureStack;
import omr.sheet.Part;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Navigable;
import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.util.List;

/**
 * Class {@code Page} represents a page in the score hierarchy, and corresponds to a
 * (part of) {@link Sheet}.
 * <p>
 * One or several Page instances compose a {@link Score}.
 *
 * @author Hervé Bitteur
 */
public class Page
        extends PageNode
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Page.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing (physical) sheet. */
    private final Sheet sheet;

    /** Containing (logical) score. */
    @Navigable(false)
    private final Score score;

    /** Page ID. */
    private String id;

    /** ScorePart list for the page. */
    private List<ScorePart> partList;

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

    //~ Constructors -------------------------------------------------------------------------------
    //------//
    // Page //
    //------//
    /**
     * Creates a new Page object.
     *
     * @param score         the containing score
     * @param sheet         the containing sheet
     * @param firstSystemId id of first system in sheet, if any
     */
    public Page (Score score,
                 Sheet sheet,
                 Integer firstSystemId)
    {
        super(null);

        this.sheet = sheet;
        this.score = score;
        this.firstSystemId = firstSystemId;

        // Define id
        computeId();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
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
            count += system.getLastRealPart().getMeasures().size();
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
            Part part = sys.getLastRealPart();

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
     *
     * @return the page/sheet dimension in pixels
     */
    public Dimension getDimension ()
    {
        return sheet.getDimension();
    }

    //----------------//
    // getFirstScoreSystem //
    //----------------//
    @Deprecated
    public ScoreSystem getFirstScoreSystem ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (ScoreSystem) children.get(0);
        }
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
    // getLastScoreSystem //
    //---------------//
    @Deprecated
    public ScoreSystem getLastScoreSystem ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (ScoreSystem) children.get(children.size() - 1);
        }
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

    //--------------------//
    // getMeanStaffHeight //
    //--------------------//
    /**
     * Report the mean staff height based on page interline.
     * This should be refined per system, if not per staff
     *
     * @return the page-based average value of staff heights
     */
    public int getMeanStaffHeight ()
    {
        return (Score.LINE_NB - 1) * getScale().getInterline();
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

    //-------------//
    // getPartList //
    //-------------//
    /**
     * Report the global list of parts.
     *
     * @return partList the list of parts
     */
    public List<ScorePart> getPartList ()
    {
        return partList;
    }

    //---------------------//
    // getPrecedingInScore //
    //---------------------//
    /**
     * Report the preceding page of this one within the score.
     *
     * @return the preceding page, or null if none
     */
    public Page getPrecedingInScore ()
    {
        ///return (Page) getPreviousSibling();
        Score score = getScore();

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
    // getScale //
    //----------//
    /**
     * Report the scale of the page.
     *
     * @return the page scale (basically: number of pixels for main interline)
     */
    @Override
    public Scale getScale ()
    {
        return sheet.getScale();
    }

    //----------//
    // getScore //
    //----------//
    @Override
    public Score getScore ()
    {
        return score;
    }

    //------------//
    // getScoreSystems //
    //------------//
    @Deprecated
    public List<TreeNode> getScoreSystems ()
    {
        return getChildren();
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

    //----------------//
    // numberMeasures //
    //----------------//
    /**
     * Assign a very basic sequential id to each contained stack & measure.
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

                for (Measure measure : stack.getMeasures()) {
                    measure.setIdValue(mid);
                }
            }

            systemOffset += system.getMeasureStacks().size();
        }

        // Very temporary raw values
        setDeltaMeasureId(systemOffset);
        computeMeasureCount();
    }

    //--------------//
    // resetScoreSystems //
    //--------------//
    /**
     * Reset the systems collection of a score entity.
     */
    @Deprecated
    public void resetScoreSystems ()
    {
        // Discard systems
        getScoreSystems().clear();

        // Discard partlists
        if (partList != null) {
            partList.clear();
        }
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

    //-------------//
    // setPartList //
    //-------------//
    /**
     * Assign a part list valid for the page.
     *
     * @param partList the list of parts
     */
    public void setPartList (List<ScorePart> partList)
    {
        this.partList = partList;
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Page " + id + "}";
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
