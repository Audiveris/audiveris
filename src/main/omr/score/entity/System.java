//----------------------------------------------------------------------------//
//                                                                            //
//                                S y s t e m                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.score.Score;
import omr.score.common.PagePoint;
import omr.score.common.PageRectangle;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.ScorePoint;
import omr.score.common.ScoreRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.common.UnitDimension;
import static omr.score.ui.ScoreConstants.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class <code>System</code> encapsulates a system in a score.
 *
 * <p>A system contains only one kind direct children : SystemPart instances
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class System
    extends SystemNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(System.class);

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

    /** Top left corner of the system in the containing page */
    private PagePoint topLeft;

    /** Actual display origin in the score view */
    private ScorePoint displayOrigin;

    /** Related info from sheet analysis */
    private SystemInfo info;

    /** System dimensions, expressed in units */
    private UnitDimension dimension;

    /** Start time of this system since beginning of the score */
    private Integer startTime;

    /** Duration of this system */
    private Integer actualDuration;

    /** Flag the fact that data has been modified and view must be updated */
    private AtomicBoolean systemDirty = new AtomicBoolean(false);

    /** Contour of all system entities to be displayed, origin being topLeft */
    private volatile SystemRectangle systemContour;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // System //
    //--------//
    /**
     * Create a system with all needed parameters
     *
     * @param info      the physical information retrieved from the sheet
     * @param score     the containing score
     * @param topLeft   the coordinate, in units, of the upper left point of the
     *                  system in its containing score
     * @param dimension the dimension of the system, expressed in units
     */
    public System (SystemInfo    info,
                   Score         score,
                   PagePoint     topLeft,
                   UnitDimension dimension)
    {
        super(score);

        this.info = info;
        this.topLeft = topLeft;
        this.dimension = dimension;

        id = getParent()
                 .getChildren()
                 .indexOf(this) + 1;

        cleanupNode();
    }

    //--------//
    // System //
    //--------//
    /**
     * Default constructor (needed by XML binder) Still needed? TBD...
     */
    private System ()
    {
        this(null, null, null, null);
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

    //------------------//
    // getAndResetDirty //
    //------------------//
    public boolean getAndResetDirty ()
    {
        return systemDirty.getAndSet(false);
    }

    //------------//
    // setContour //
    //------------//
    public void setContour (SystemRectangle systemContour)
    {
        this.systemContour = systemContour;
    }

    //------------//
    // getContour //
    //------------//
    public SystemRectangle getContour ()
    {
        return systemContour;
    }

    //--------------//
    // setDimension //
    //--------------//
    /**
     * Set the system dimension.
     *
     * <p>Width is the distance, in units, between left edge and right edge.
     *
     * <p>Height is the distance, in units, from top of first staff, down to
     * top (and not bottom) of last staff.
     * Nota: It does not count the height of the last staff
     *
     * @param dimension system dimension, in units
     */
    public void setDimension (UnitDimension dimension)
    {
        this.dimension = dimension;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the system dimension.
     *
     * @return the system dimension, in units
     * @see #setDimension
     */
    public UnitDimension getDimension ()
    {
        return dimension;
    }

    //----------//
    // setDirty //
    //----------//
    public void setDirty ()
    {
        systemDirty.set(true);
    }

    //------------------//
    // setDisplayOrigin //
    //------------------//
    /**
     * Assign origin for score display
     *
     * @param origin display origin for this system
     */
    public void setDisplayOrigin (ScorePoint origin)
    {
        this.displayOrigin = origin;
    }

    //------------------//
    // getDisplayOrigin //
    //------------------//
    /**
     * Report the origin for this system, in the horizontal score display
     *
     * @return the display origin
     */
    @Override
    public ScorePoint getDisplayOrigin ()
    {
        return displayOrigin;
    }

    //--------------//
    // getFirstPart //
    //--------------//
    /**
     * Report the first part in this system
     *
     * @return the first part entity
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
     * Report the first non artificial part in this system
     *
     * @return the real first part entity
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

    //------------------//
    // getRightPosition //
    //------------------//
    /**
     * Return the actual display position of the right side.
     *
     * @return the display abscissa of the right system edge
     */
    public int getRightPosition ()
    {
        return (displayOrigin.x + dimension.width) - 1;
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
                int   dy = sysPt.y - staff.getTopLeft().y + getTopLeft().y;

                if (dy >= 0) {
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
                    int   midY = (staff.getTopLeft().y +
                                 (staff.getHeight() / 2)) -
                                 this.getTopLeft().y;
                    int   dy = Math.abs(sysPt.y - midY);

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

        if (pagPt.y < firstStaff.getTopLeft().y) {
            return StaffPosition.above;
        }

        Staff lastStaff = getLastPart()
                              .getLastStaff();

        if (pagPt.y > (lastStaff.getTopLeft().y + lastStaff.getHeight())) {
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
            System prevSystem = (System) getPreviousSibling();

            if (prevSystem == null) {
                startTime = 0;
            } else {
                startTime = prevSystem.getStartTime() +
                            prevSystem.getActualDuration();
            }
        }

        return startTime;
    }

    //------------//
    // setTopLeft //
    //------------//
    /**
     * Set the coordinates of the top left cormer of the system in the score
     *
     * @param topLeft the upper left point, with coordinates in units, in
     * virtual score page
     */
    public void setTopLeft (PagePoint topLeft)
    {
        this.topLeft = topLeft;
    }

    //------------//
    // getTopLeft //
    //------------//
    /**
     * Report the coordinates of the upper left corner of this system in its
     * containing score
     *
     * @return the top left corner
     * @see #setTopLeft
     */
    public PagePoint getTopLeft ()
    {
        return topLeft;
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
                sortPartsOnId();
            }
        }
    }

    //--------//
    // locate //
    //--------//
    /**
     * Return the position of given ScorePoint, relative to the system.
     *
     * @param scrPt the ScorePoint in the score display
     *
     * @return -1 for left, 0 for middle, +1 for right
     */
    public int locate (ScorePoint scrPt)
    {
        if (scrPt.x < displayOrigin.x) {
            return -1;
        }

        if (scrPt.x > getRightPosition()) {
            return +1;
        }

        return 0;
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

        if (pagPt.y > (topLeft.y + dimension.height + STAFF_HEIGHT)) {
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

    //---------------//
    // sortPartsOnId //
    //---------------//
    /**
     * Sort the parts of this system according to their part id
     */
    public void sortPartsOnId ()
    {
        Collections.sort(
            getParts(),
            new Comparator<TreeNode>() {
                    public int compare (TreeNode o1,
                                        TreeNode o2)
                    {
                        SystemPart p1 = (SystemPart) o1;
                        SystemPart p2 = (SystemPart) o2;

                        return Integer.signum(p1.getId() - p2.getId());
                    }
                });
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Compute the point in the sheet that corresponds to a given point in the
     * score display
     *
     * @param scrPt the point in the score display
     * @return the corresponding page point
     * @see #toScorePoint
     */
    public PagePoint toPagePoint (ScorePoint scrPt)
    {
        return new PagePoint(
            (topLeft.x + scrPt.x) - displayOrigin.x,
            (topLeft.y + scrPt.y) - displayOrigin.y);
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

    //--------------//
    // toScorePoint //
    //--------------//
    /**
     * Compute the score display point that correspond to a given sheet point,
     * since systems are displayed horizontally in the score display, while they
     * are located one under the other in a sheet.
     *
     * @param pagPt the point in the sheet
     * @return the score point
     * @see #toPagePoint
     */
    public ScorePoint toScorePoint (PagePoint pagPt)
    {
        return new ScorePoint(
            displayOrigin.x + (pagPt.x - topLeft.x),
            displayOrigin.y + (pagPt.y - topLeft.y));
    }

    //--------------//
    // toScorePoint //
    //--------------//
    /**
     * Compute the score display point that correspond to a given sheet point,
     * since systems are displayed horizontally in the score display, while they
     * are located one under the other in a sheet.
     *
     * @param sysPt the point in the system
     * @return the score point
     * @see #toPagePoint
     */
    public ScorePoint toScorePoint (SystemPoint sysPt)
    {
        return toScorePoint(toPagePoint(sysPt));
    }

    //------------------//
    // toScoreRectangle //
    //------------------//
    public ScoreRectangle toScoreRectangle (SystemRectangle sysRect)
    {
        ScorePoint org = toScorePoint(new SystemPoint(sysRect.x, sysRect.y));

        return new ScoreRectangle(org.x, org.y, sysRect.width, sysRect.height);
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

        if (dimension != null) {
            sb.append(" dimension=")
              .append(dimension.width)
              .append("x")
              .append(dimension.height);
        }

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
}
