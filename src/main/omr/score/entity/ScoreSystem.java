//----------------------------------------------------------------------------//
//                                                                            //
//                           S c o r e S y s t e m                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.text.TextRole;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PagePoint;
import omr.score.common.PageRectangle;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.common.UnitDimension;
import static omr.score.ui.ScoreConstants.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.SystemInfo;

import omr.util.TreeNode;

import java.util.Collections;
import java.util.List;

/**
 * Class <code>ScoreSystem</code> encapsulates a system in a score.
 *
 * <p>A system contains only one kind of direct children : SystemPart instances
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreSystem
    extends SystemNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreSystem.class);

    //~ Enumerations -----------------------------------------------------------

    /** Relative vertical position with respect to the system staves */
    public enum StaffPosition {
        //~ Enumeration constant initializers ----------------------------------


        /** Above the first staff of the first (real)part */
        above,
        /** Somewhere within the staves of this system */
        within, 
        /** Below the last staff of the last part */
        below;
    }

    //~ Instance fields --------------------------------------------------------

    /** Id for debug */
    private final int id;

    /**
     * Top left corner of the system in the containing page. This points to the
     * first real staff, and does not count the preceding dummy staves if any.
     */
    private final PagePoint topLeft;

    /** Related info from sheet analysis */
    private final SystemInfo info;

    /** Start time of this system since beginning of the score */
    private Integer startTime;

    /** Duration of this system */
    private Integer actualDuration;

    /** Contour of all system entities to be displayed, origin being topLeft */
    private volatile SystemRectangle displayContour;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ScoreSystem //
    //-------------//
    /**
     * Create a system with all needed parameters
     *
     * @param info      the physical information retrieved from the sheet
     * @param score     the containing score
     * @param topLeft   the coordinate, in units, of the upper left point of the
     *                  system in its containing score
     * @param dimension the dimension of the system, expressed in units
     */
    public ScoreSystem (SystemInfo    info,
                        Score         score,
                        PagePoint     topLeft,
                        UnitDimension dimension)
    {
        super(score);

        this.info = info;
        this.topLeft = topLeft;

        setBox(new SystemRectangle(0, 0, dimension.width, dimension.height));
        getCenter();

        id = getParent()
                 .getChildren()
                 .indexOf(this) + 1;

        cleanupNode();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getActualDuration //
    //-------------------//
    public int getActualDuration ()
    {
        if (actualDuration == null) {
            SystemPart part = getFirstPart();
            actualDuration = 0;

            for (TreeNode m : part.getMeasures()) {
                Measure measure = (Measure) m;
                actualDuration += measure.getActualDuration();
            }
        }

        return actualDuration;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the system dimension.
     * <p>Width is the distance, in units, between left edge and right edge.
     * <p>Height is the distance, in units, from top of first staff, down to
     * top (and not bottom) of last staff.
     * Nota: It does not count the height of the last staff
     *
     * @return the system dimension, in units
     */
    public UnitDimension getDimension ()
    {
        return new UnitDimension(getBox().width, getBox().height);
    }

    //------------//
    // setContour //
    //------------//
    /**
     * Define the new display contour for the system
     * @param systemContour the new display contour
     */
    public void setDisplayContour (SystemRectangle systemContour)
    {
        this.displayContour = systemContour;
    }

    //-------------------//
    // getDisplayContour //
    //-------------------//
    /**
     * Return a copy of the system display contour, which includes the system
     * staves plus the satellite text entities
     * @return a COPY of the system display contour
     */
    public SystemRectangle getDisplayContour ()
    {
        if (displayContour == null) {
            return null;
        } else {
            return (SystemRectangle) displayContour.clone();
        }
    }

    //----------------//
    // getDummyOffset //
    //----------------//
    /**
     * Report the vertical offset of the first part wrt the system, which is 0
     * for a standard system, and a positive value when the system begins with
     * a dummy part.
     * @return the positive vertical offset (in unit) of the dummy part, or 0
     * if none
     */
    public int getDummyOffset ()
    {
        return getTopLeft().y -
               getFirstPart()
                   .getFirstStaff()
                   .getPageTopLeft().y;
    }

    //--------------//
    // getFirstPart //
    //--------------//
    /**
     * Report the very first part in this system, which may be a dummy one.
     * Use {@link #getFirstRealPart} instead to point to the first real part.
     *
     * @return the first part entity
     * @see #getFirstRealPart()
     */
    public SystemPart getFirstPart ()
    {
        return (SystemPart) getParts()
                                .get(0);
    }

    //------------------//
    // getFirstRealPart //
    //------------------//
    /**
     * Report the first non dummy part in this system
     *
     * @return the real first part entity
     * @see #getFirstPart()
     */
    public SystemPart getFirstRealPart ()
    {
        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            if (!part.isDummy()) {
                return part;
            }
        }

        return null;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the physical information retrieved from the sheet for this system
     *
     * @return the information entity
     */
    public SystemInfo getInfo ()
    {
        return info;
    }

    //-------------//
    // getLastPart //
    //-------------//
    /**
     * Report the last part in this system
     *
     * @return the last part entity
     */
    public SystemPart getLastPart ()
    {
        return (SystemPart) getParts()
                                .get(getParts().size() - 1);
    }

    //------------------//
    // getLastSoundTime //
    //------------------//
    /**
     * Report the time, counted from beginning of this system, when sound stops,
     * which means that ending rests are not counted.
     *
     * @param measureId potential constraint on measure id,
     * null for no constraint
     * @return the relative time of last Midi "note off" in this part
     */
    public int getLastSoundTime (Integer measureId)
    {
        int lastTime = 0;

        // Take the latest sound among all parts
        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;
            int        time = part.getLastSoundTime(measureId);

            if (time > lastTime) {
                lastTime = time;
            }
        }

        return lastTime;
    }

    //----------------//
    // isLeftOfStaves //
    //----------------//
    /**
     * Report whether the provided system point is on the left side of the
     * staves (on left of the starting barline)
     * @param sysPt the system point to check
     * @return true if on left
     */
    public boolean isLeftOfStaves (SystemPoint sysPt)
    {
        return sysPt.x < 0;
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the part with the provided id, if any
     *
     * @param id the id of the desired part
     * @return the part found or null
     */
    public SystemPart getPart (int id)
    {
        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            if (part.getId() == id) {
                return part;
            }
        }

        return null;
    }

    //-----------//
    // getPartAt //
    //-----------//
    /**
     * Determine the part which contains the given system point
     *
     * @param sysPt the given system point
     * @return the containing part
     */
    public SystemPart getPartAt (SystemPoint sysPt)
    {
        return getStaffAt(sysPt)
                   .getPart();
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Report the parts for this system
     *
     * @return the ordered parts
     */
    public List<TreeNode> getParts ()
    {
        return getChildren();
    }

    //---------------//
    // getStaffAbove //
    //---------------//
    /**
     * Determine the staff which is just above the given system point
     *
     * @param sysPt the given system point
     * @return the staff above
     */
    public Staff getStaffAbove (SystemPoint sysPt)
    {
        Staff best = null;

        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            for (TreeNode n : part.getStaves()) {
                Staff staff = (Staff) n;

                if (staff.getCenter().y < sysPt.y) {
                    best = staff;
                }
            }
        }

        return best;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Determine the staff which is closest to the given system point
     *
     * @param sysPt the given system point
     * @return the closest staff
     */
    public Staff getStaffAt (SystemPoint sysPt)
    {
        int   minDy = Integer.MAX_VALUE;
        Staff best = null;

        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            if (!part.isDummy()) {
                for (TreeNode n : part.getStaves()) {
                    Staff staff = (Staff) n;
                    int   dy = Math.abs(sysPt.y - staff.getCenter().y);

                    if (dy < minDy) {
                        minDy = dy;
                        best = staff;
                    }
                }
            }
        }

        return best;
    }

    //------------------//
    // getStaffPosition //
    //------------------//
    /**
     * Report the vertical position of the provided system point with respect to
     * the system staves
     * @param sysPt the system point whose ordinate is to be checked
     * @return the StaffPosition value
     */
    public StaffPosition getStaffPosition (SystemPoint sysPt)
    {
        Staff     firstStaff = getFirstRealPart()
                                   .getFirstStaff();

        PagePoint pagPt = toPagePoint(sysPt);

        if (pagPt.y < firstStaff.getPageTopLeft().y) {
            return StaffPosition.above;
        }

        Staff lastStaff = getLastPart()
                              .getLastStaff();

        if (pagPt.y > (lastStaff.getPageTopLeft().y + lastStaff.getHeight())) {
            return StaffPosition.below;
        } else {
            return StaffPosition.within;
        }
    }

    //--------------//
    // getStartTime //
    //--------------//
    /**
     * Report the start time of this system, with respect to the beginning of
     * the score.
     * @return the system start time
     */
    public int getStartTime ()
    {
        if (startTime == null) {
            ScoreSystem prevSystem = (ScoreSystem) getPreviousSibling();

            if (prevSystem == null) {
                startTime = 0;
            } else {
                startTime = prevSystem.getStartTime() +
                            prevSystem.getActualDuration();
            }
        }

        return startTime;
    }

    //--------------//
    // getTextStaff //
    //--------------//
    /**
     * Report the related staff for a text at the provided point, since some
     * texts (direction, lyrics) are preferably assigned to the staff above if
     * any
     * @param role the precise role of text glyph
     * @param sysPt the provided point
     * @return the preferred staff
     */
    public Staff getTextStaff (TextRole    role,
                               SystemPoint sysPt)
    {
        Staff staff = null;

        if ((role == TextRole.Direction) || (role == TextRole.Lyrics)) {
            staff = getStaffAbove(sysPt);
        }

        if (staff == null) {
            staff = getStaffAt(sysPt);
        }

        return staff;
    }

    //------------//
    // getTopLeft //
    //------------//
    /**
     * Report the coordinates of the upper left corner of this system in its
     * containing score (not counting preceding dummy staves if any).
     * To point to the upper left corner, including dummy staves, use
     * getTopLeft() + getDummyOffset();
     *
     * @return the top left corner
     * @see #getDummyOffset
     */
    public PagePoint getTopLeft ()
    {
        return topLeft;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the system width.
     * @param unitWidth the system width, in units
     */
    public void setWidth (int unitWidth)
    {
        SystemRectangle newBox = getBox();
        reset();

        newBox.width = unitWidth;
        setBox(newBox);
        getCenter();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-------------//
    // cleanupNode //
    //-------------//
    public void cleanupNode ()
    {
        actualDuration = null;
        startTime = null;
    }

    //------------------//
    // fillMissingParts //
    //------------------//
    /**
     * Check for missing parts in this system, and if needed create dummy parts
     * filled with whole rests
     */
    public void fillMissingParts ()
    {
        // Check we have all the defined parts in this system
        for (ScorePart scorePart : getScore()
                                       .getPartList()) {
            if (getPart(scorePart.getId()) == null) {
                getFirstRealPart()
                    .createDummyPart(scorePart.getId());
                Collections.sort(getParts(), SystemPart.idComparator);
            }
        }
    }

    //--------//
    // locate //
    //--------//
    /**
     * Return the position of given PagePoint, relative to the system
     *
     * @param pagPt the given PagePoint
     *
     * @return -1 for above, 0 for middle, +1 for below
     */
    public int locate (PagePoint pagPt)
    {
        if (pagPt.y < topLeft.y) {
            return -1;
        }

        if (pagPt.y > (topLeft.y + getBox().height + STAFF_HEIGHT)) {
            return +1;
        }

        return 0;
    }

    //-------------------------//
    // recomputeActualDuration //
    //-------------------------//
    /**
     * Force recomputation of the system cached actual duration
     */
    public void recomputeActualDuration ()
    {
        actualDuration = null;
        getActualDuration();
    }

    //--------------------//
    // recomputeStartTime //
    //--------------------//
    /**
     * Force recomputation of the system cached start time
     */
    public void recomputeStartTime ()
    {
        startTime = null;
        getStartTime();
    }

    //----------------------//
    // refineLyricSyllables //
    //----------------------//
    public void refineLyricSyllables ()
    {
        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;
            part.refineLyricSyllables();
        }
    }

    //-------------------------//
    // retrieveSlurConnections //
    //-------------------------//
    /**
     * Retrieve the connections between the (orphan) slurs at the beginning of
     * this system and the (orphan) slurs at the end of the previous system
     */
    public void retrieveSlurConnections ()
    {
        if (getPreviousSibling() != null) {
            // Examine every part in sequence
            for (TreeNode pNode : getParts()) {
                SystemPart part = (SystemPart) pNode;
                part.retrieveSlurConnections();
            }
        }
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Compute the pagepoint that correspond to a given systempoint, which is
     * basically a translation using the coordinates of the system topLeft
     * corner.
     *
     * @param sysPt the point in the system
     * @return the page point
     */
    public PagePoint toPagePoint (SystemPoint sysPt)
    {
        return new PagePoint(sysPt.x + topLeft.x, sysPt.y + topLeft.y);
    }

    //-----------------//
    // toPageRectangle //
    //-----------------//
    /**
     * Compute the page rectangle that corresonds to a given page rectangle,
     * which boils down to a simple translation using the coordinates of the
     * ystem topLeft corner.
     * @param sysRect the rectangle in the system
     * @return the page rectangle
     */
    public PageRectangle toPageRectangle (SystemRectangle sysRect)
    {
        return new PageRectangle(
            sysRect.x + topLeft.x,
            sysRect.y + topLeft.y,
            sysRect.width,
            sysRect.height);
    }

    //--------------//
    // toPixelPoint //
    //--------------//
    /**
     * Compute the pixel point that correspond to a given system point, which is
     * basically a translation using the coordinates of the system topLeft
     * corner, then a scaling.
     *
     * @param sysPt the point in the system
     * @return the pixel point
     */
    public PixelPoint toPixelPoint (SystemPoint sysPt)
    {
        return getScale()
                   .toPixelPoint(toPagePoint(sysPt));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{System#")
          .append(id);

        if (topLeft != null) {
            sb.append(" topLeft=[")
              .append(topLeft.x)
              .append(",")
              .append(topLeft.y)
              .append("]");
        }

        sb.append(" dimension=")
          .append(getBox().width)
          .append("x")
          .append(getBox().height);

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // toSystemPoint //
    //---------------//
    /**
     * Compute the system point that correspond to a given page point, which is
     * basically a translation using the coordinates of the system topLeft
     * corner.
     *
     * @param pagPt the point in the sheet
     * @return the system point
     */
    public SystemPoint toSystemPoint (PagePoint pagPt)
    {
        return new SystemPoint(pagPt.x - topLeft.x, pagPt.y - topLeft.y);
    }

    //---------------//
    // toSystemPoint //
    //---------------//
    /**
     * Compute the system point that correspond to a given pixel point, which is
     * basically a scaling plus a translation using the coordinates of the
     * system topLeft corner.
     *
     * @param pixPt the point in the sheet
     * @return the system point
     */
    public SystemPoint toSystemPoint (PixelPoint pixPt)
    {
        PagePoint pagPt = getScale()
                              .toPagePoint(pixPt);

        return new SystemPoint(pagPt.x - topLeft.x, pagPt.y - topLeft.y);
    }

    //-------------------//
    // toSystemRectangle //
    //-------------------//
    /**
     * Compute the system rectangle that correspond to a given pixel rectangle,
     * which is basically a scaling plus a translation using the coordinates of
     * the system topLeft corner.
     *
     * @param pixRect the rectangle in the sheet
     * @return the system rectangle
     */
    public SystemRectangle toSystemRectangle (PixelRectangle pixRect)
    {
        PageRectangle pagRect = getScale()
                                    .toUnits(pixRect);

        return toSystemRectangle(pagRect);
    }

    //-------------------//
    // toSystemRectangle //
    //-------------------//
    /**
     * Compute the system rectangle that correspond to a given page rectangle,
     * which is basically a translation using the coordinates of the system
     * topLeft corner.
     *
     * @param pagRect the rectangle in the page
     * @return the system rectangle
     */
    public SystemRectangle toSystemRectangle (PageRectangle pagRect)
    {
        return new SystemRectangle(
            pagRect.x - topLeft.x,
            pagRect.y - topLeft.y,
            pagRect.width,
            pagRect.height);
    }

    //-------//
    // reset //
    //-------//
    @Override
    protected void reset ()
    {
        super.reset();

        cleanupNode();
    }
}
