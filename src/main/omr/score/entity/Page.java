//----------------------------------------------------------------------------//
//                                                                            //
//                                  P a g e                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.Scale.InterlineFraction;
import omr.sheet.Sheet;

import omr.step.StepException;

import omr.util.TreeNode;

import java.awt.image.RenderedImage;
import java.util.List;

/**
 * Class {@code Page} represents a page in the score hierarchy, and corresponds
 * to a {@link Sheet} with its specific scale, skew, dimension, etc.
 * Page instances compose a {@link Score}.
 *
 * @author Hervé Bitteur
 */
public class Page
    extends PageNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Page.class);

    //~ Instance fields --------------------------------------------------------

    /** Index of page */
    private final int index;

    /** Page ID */
    private final String id;

    /** Link with image */
    private Sheet sheet;

    /** Page global scale */
    private Scale scale;

    /** Sheet skew angle in radians */
    private double skewAngle;

    /** The page slot policy */
    private SlotPolicy slotPolicy;

    /** The page slot horizontal margin, expressed in interline fraction */
    private InterlineFraction slotMargin;

    /** Average beam thickness, if known */
    private Integer beamThickness;

    /** ScorePart list for the page */
    private List<ScorePart> partList;

    /** Number of measures in this page */
    private Integer measureCount;

    /** Progression of measure id within this page */
    private Integer deltaMeasureId;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Page //
    //------//
    /**
     * Creates a new Page object.
     *
     * @param score the containing score
     */
    public Page (Score         score,
                 int           index,
                 RenderedImage image)
        throws StepException
    {
        super(score);
        this.index = index;

        if (score.isMultiPage()) {
            id = score.getRadix() + "#" + index;
        } else {
            id = score.getRadix();
        }

        sheet = new Sheet(this, image);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // setBeamThickness //
    //------------------//
    public void setBeamThickness (int beamThickness)
    {
        this.beamThickness = beamThickness;
    }

    //------------------//
    // getBeamThickness //
    //------------------//
    public Integer getBeamThickness ()
    {
        return beamThickness;
    }

    //----------------------//
    // setDefaultSlotMargin //
    //----------------------//
    /**
     * Assign the default slot margin
     * @param fraction the horizontal margin, expressed in interline fraction
     */
    public static void setDefaultSlotMargin (double fraction)
    {
        constants.defaultSlotMargin.setValue(fraction);
    }

    //----------------------//
    // getDefaultSlotMargin //
    //----------------------//
    /**
     * Report the default horizontal Slot margin
     * @return the slotMargin (in interline fraction)
     */
    public static InterlineFraction getDefaultSlotMargin ()
    {
        return constants.defaultSlotMargin.getWrappedValue();
    }

    //----------------------//
    // setDefaultSlotPolicy //
    //----------------------//
    /**
     * Assign the default slot policy
     * @param slotPolicy the slot policy
     */
    public static void setDefaultSlotPolicy (SlotPolicy slotPolicy)
    {
        constants.defaultSlotPolicy.setValue(slotPolicy);
    }

    //----------------------//
    // getDefaultSlotPolicy //
    //----------------------//
    /**
     * Report the default policy to be used for retrieval of time slots
     * @return the default time slot policy
     */
    public static SlotPolicy getDefaultSlotPolicy ()
    {
        return constants.defaultSlotPolicy.getValue();
    }

    //-------------------//
    // setDeltaMeasureId //
    //-------------------//
    /**
     * Assign the progression of measure IDs within this page
     * @param deltaMeasureId the deltaMeasureId to set
     */
    public void setDeltaMeasureId (Integer deltaMeasureId)
    {
        this.deltaMeasureId = deltaMeasureId;
    }

    //-------------------//
    // getDeltaMeasureId //
    //-------------------//
    /**
     * Report the progression of measure IDs within this page
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
     * Report the dimension of the sheet/page
     * @return the page/sheet dimension in pixels
     */
    public PixelDimension getDimension ()
    {
        return sheet.getDimension();
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

    //----------//
    // getIndex //
    //----------//
    /**
     * @return the page index
     */
    public int getIndex ()
    {
        return index;
    }

    //--------------------//
    // getMeanStaffHeight //
    //--------------------//
    /**
     * Report the mean staff height based on page interline. This should be
     * refined per system, if not per staff
     * @return the page-based average value of staff heights
     */
    public int getMeanStaffHeight ()
    {
        return (Score.LINE_NB - 1) * scale.interline();
    }

    //-----------------//
    // getMeasureCount //
    //-----------------//
    /**
     * Report the number of (vertical) measures in this page
     * @return the number of page measures
     */
    public int getMeasureCount ()
    {
        return measureCount;
    }

    //-------------------//
    // getMinSlotSpacing //
    //-------------------//
    /**
     * Report the minimum acceptable spacing between slots
     * @return the minimum spacing (in interline fraction)
     */
    public static InterlineFraction getMinSlotSpacing ()
    {
        return constants.minSlotSpacing.getWrappedValue();
    }

    //----------------//
    // getFirstSystem //
    //----------------//
    /**
     * Report the first system in the page
     *
     * @return the first system
     */
    public ScoreSystem getFirstSystem ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (ScoreSystem) children.get(0);
        }
    }

    //---------------//
    // getLastSystem //
    //---------------//
    /**
     * Report the last system in the page
     *
     * @return the last system
     */
    public ScoreSystem getLastSystem ()
    {
        if (children.isEmpty()) {
            return null;
        } else {
            return (ScoreSystem) children.get(children.size() - 1);
        }
    }

    //-------------//
    // setPartList //
    //-------------//
    /**
     * Assign a part list valid for the page
     *
     * @param partList the list of parts
     */
    public void setPartList (List<ScorePart> partList)
    {
        this.partList = partList;
    }

    //-------------//
    // getPartList //
    //-------------//
    /**
     * Report the global list of parts
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
        return (Page) getPreviousSibling();
    }

    //----------//
    // setScale //
    //----------//
    /**
     * Assign proper scale for this page
     * @param scale the general scale for the page
     */
    public void setScale (Scale scale)
    {
        this.scale = scale;
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the scale of the page
     *
     * @return the page scale (basically: number of pixels for main interline)
     */
    @Override
    public Scale getScale ()
    {
        return scale;
    }

    //----------//
    // setSheet //
    //----------//

    /**
     * Register the name of the corresponding sheet entity
     *
     * @param sheet the related sheet entity
     */
    public void setSheet (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the related sheet entity
     *
     * @return the related sheet, or null if none
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //--------------//
    // setSkewAngle //
    //--------------//
    /**
     * Assign score global skew angle
     * @param skewAngle the detected skew angle, in radians, clockwise
     */
    public void setSkewAngle (double skewAngle)
    {
        this.skewAngle = skewAngle;
    }

    //--------------//
    // getSkewAngle //
    //--------------//
    /**
     * Report the score skew angle
     *
     * @return skew angle, in radians, clock-wise
     */
    public double getSkewAngle ()
    {
        return skewAngle;
    }

    //---------------//
    // setSlotMargin //
    //---------------//
    /**
     * Assign the slot margin for this score
     * @param fraction the horizontal margin, expressed in interline fraction
     */
    public void setSlotMargin (InterlineFraction fraction)
    {
        this.slotMargin = new InterlineFraction(fraction.doubleValue());
    }

    //---------------//
    // getSlotMargin //
    //---------------//
    /**
     * Report the current horizontal Slot margin
     * If the value is not yet set, it is set to the default value and returned.
     * @return the slotMargin (in interline fraction)
     */
    public InterlineFraction getSlotMargin ()
    {
        if (!hasSlotMargin()) {
            slotMargin = getDefaultSlotMargin();
        }

        return slotMargin;
    }

    //---------------//
    // setSlotPolicy //
    //---------------//
    /**
     * Assign the slot policy for this score
     * @param slotPolicy the policy for determining slots
     */
    public void setSlotPolicy (SlotPolicy slotPolicy)
    {
        this.slotPolicy = slotPolicy;
    }

    //---------------//
    // getSlotPolicy //
    //---------------//
    /**
     * Report the policy used for retrieval of time slots in this score
     * @return the score time slot policy
     */
    public SlotPolicy getSlotPolicy ()
    {
        return slotPolicy;
    }

    //---------------//
    // getSystemById //
    //---------------//
    /**
     * Report the system for which id is provided
     * @param id id of desired system
     * @return the desired system
     */
    public ScoreSystem getSystemById (int id)
    {
        return (ScoreSystem) getSystems()
                                 .get(id - 1);
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report the collection of systems in that score
     *
     * @return the systems
     */
    public List<TreeNode> getSystems ()
    {
        return getChildren();
    }

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
     * Compute the number of (vertical) measures in the page
     */
    public void computeMeasureCount ()
    {
        int count = 0;

        for (TreeNode sn : getSystems()) {
            ScoreSystem system = (ScoreSystem) sn;
            count += system.getFirstPart()
                           .getMeasures()
                           .size();
        }

        measureCount = count;
    }

    //-------------------//
    // dumpMeasureCounts //
    //-------------------//
    /**
     * Log the detailed number of measures in the score
     */
    public void dumpMeasureCounts ()
    {
        int           count = 0;
        StringBuilder sb = new StringBuilder();

        for (TreeNode node : getSystems()) {
            ScoreSystem sys = (ScoreSystem) node;
            SystemPart  part = sys.getLastPart();

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(part.getMeasures().size())
              .append(" in System#")
              .append(sys.getId());
            count += part.getMeasures()
                         .size();
        }

        StringBuilder prefix = new StringBuilder();
        prefix.append(count);
        prefix.append(" raw measure");

        if (count > 1) {
            prefix.append('s');
        }

        prefix.append(": [")
              .append(sb)
              .append("]");

        logger.info(sheet.getLogPrefix() + prefix.toString());
    }

    //---------------//
    // hasSlotMargin //
    //---------------//
    /**
     * Check whether slotMargin is defined for this score
     * @return true if slotMargin is defined
     */
    public boolean hasSlotMargin ()
    {
        return slotMargin != null;
    }

    //---------------//
    // hasSlotPolicy //
    //---------------//
    public boolean hasSlotPolicy ()
    {
        return slotPolicy != null;
    }

    //--------------//
    // resetSystems //
    //--------------//
    /**
     * Reset the systems collection of a score entity
     */
    public void resetSystems ()
    {
        // Discard systems
        getSystems()
            .clear();

        // Discard partlists
        if (partList != null) {
            partList.clear();
        }

        //        // Discard cached recent system
        //        recentSystemRef = null;
        //
        //        // Discard current score location event (which contains a system id)
        //        if (Main.getGui() != null) {
        //            getSheet()
        //                .getSelectionService()
        //                .publish(new ScoreLocationEvent(this, null, null, null));
        //        }
    }

    //----------//
    // systemAt //
    //----------//
    /**
     * Retrieve which system contains the provided point
     *
     * @param point the point in the <b>SHEET</b> display
     *
     * @return the nearest system.
     */
    public ScoreSystem systemAt (PixelPoint point)
    {
        return getSheet()
                   .getSystemOf(point)
                   .getScoreSystem();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Page " + id + "}";
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Default slot policy */
        SlotPolicy.Constant defaultSlotPolicy = new SlotPolicy.Constant(
            SlotPolicy.HEAD_BASED,
            "Default policy for determining time slots (HEAD_BASED or SLOT_BASED)");

        /**
         * Default horizontal margin between a slot and a glyph candidate
         */
        Scale.Fraction defaultSlotMargin = new Scale.Fraction(
            0.5,
            "Default horizontal margin between a slot and a glyph candidate");

        /** Minimum spacing between slots before alerting user */
        private final Scale.Fraction minSlotSpacing = new Scale.Fraction(
            1.1d,
            "Minimum spacing between slots before alerting user");
    }
}
