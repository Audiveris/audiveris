//----------------------------------------------------------------------------//
//                                                                            //
//                           S c o r e S y s t e m                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.SystemInfo;

import omr.text.TextRole;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code ScoreSystem} encapsulates a system in a score.
 *
 * <p>A system contains only one kind of direct children : SystemPart instances
 *
 * @author Hervé Bitteur
 */
public class ScoreSystem
        extends SystemNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScoreSystem.class);

    //~ Instance fields --------------------------------------------------------
    /** Id for debug */
    private final int id;

    /**
     * Top left corner of the system in the containing page.
     * This points to the first real staff, and does not count the preceding
     * dummy staves if any.
     */
    private final Point topLeft;

    /** Related info from sheet analysis */
    private final SystemInfo info;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // ScoreSystem //
    //-------------//
    /**
     * Create a system with all needed parameters.
     *
     * @param info      the physical information retrieved from the sheet
     * @param page      the containing page
     * @param topLeft   the coordinates of the upper left point of the system
     *                  in its containing page
     * @param dimension the dimension of the system
     */
    public ScoreSystem (SystemInfo info,
                        Page page,
                        Point topLeft,
                        Dimension dimension)
    {
        super(page);

        this.info = info;
        this.topLeft = topLeft;

        id = 1 + getChildIndex();

        setBox(
                new Rectangle(
                topLeft.x,
                topLeft.y,
                dimension.width,
                dimension.height));
        getCenter();
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-------------------------//
    // connectPageInitialSlurs //
    //-------------------------//
    /**
     * For this system, retrieve the connections between the (orphan)
     * slurs at the beginning of this page and the (orphan) slurs at the
     * end of the previous page.
     */
    public void connectPageInitialSlurs ()
    {
        // Safer: check we are the very first system in page
        if (getChildIndex() != 0) {
            throw new IllegalArgumentException(
                    "connectSlursAcrossPages called for non-first system");
        }

        // If very first page, we are done
        if (getPage()
                .getChildIndex() == 0) {
            return;
        }

        ScoreSystem precedingSystem = getPage()
                .getPrecedingInScore()
                .getLastSystem();

        if (precedingSystem != null) {
            // Examine every part in sequence
            for (TreeNode pNode : getParts()) {
                SystemPart part = (SystemPart) pNode;

                // Find out the proper preceding part (across pages)
                SystemPart precedingPart = precedingSystem.getPart(
                        part.getId());

                // Ending orphans in preceding system/part (if such part exists)
                part.connectSlursWith(precedingPart);
            }
        }
    }

    //---------------------------//
    // connectSystemInitialSlurs //
    //---------------------------//
    /**
     * Retrieve the connections between the (orphan) slurs at the
     * beginning of this system and the (orphan) slurs at the end of the
     * previous system.
     */
    public void connectSystemInitialSlurs ()
    {
        if (getPreviousSibling() != null) {
            // Examine every part in sequence
            for (TreeNode pNode : getParts()) {
                SystemPart part = (SystemPart) pNode;
                // Ending orphans in preceding system/part (if such part exists)
                part.connectSlursWith(part.getPrecedingInPage());
            }
        }
    }

    //-------------------//
    // connectTiedVoices //
    //-------------------//
    /**
     * Make sure that notes tied across measures keep the same voice.
     * This is performed for all measures in this system.
     */
    public void connectTiedVoices ()
    {
        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;
            part.connectTiedVoices();
        }
    }

    //------------------//
    // fillMissingParts //
    //------------------//
    /**
     * Check for missing parts in this system, and if needed create
     * dummy parts filled with whole rests.
     */
    public void fillMissingParts ()
    {
        // Check we have all the defined parts in this system
        for (ScorePart scorePart : getPage()
                .getPartList()) {
            if (getPart(scorePart.getId()) == null) {
                getFirstRealPart()
                        .createDummyPart(scorePart.getId());
                Collections.sort(getParts(), SystemPart.idComparator);
            }
        }
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the system dimension.
     * <p>Width is the distance, in pixels, between left edge and right edge.
     * <p>Height is the distance, in pixels, from top of first staff, down to
     * top (and not bottom) of last staff.
     * Nota: It does not count the height of the last staff
     *
     * @return the system dimension
     */
    @Override
    public Dimension getDimension ()
    {
        return super.getDimension();
    }

    //--------------//
    // getFirstPart //
    //--------------//
    /**
     * Report the very first part in this system, which may be a dummy
     * one.
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
     * Report the first non dummy part in this system.
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
     * Report the physical information retrieved from the sheet for this
     * system.
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
     * Report the last part in this system.
     *
     * @return the last part entity
     */
    public SystemPart getLastPart ()
    {
        return (SystemPart) getParts()
                .get(getParts().size() - 1);
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the part with the provided id, if any.
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

        logger.debug("{} No part {} found", getContextString(), id);

        return null;
    }

    //--------------//
    // getPartAbove //
    //--------------//
    /**
     * Determine the part which is above the given point.
     *
     * @param point the given point
     * @return the part above
     */
    public SystemPart getPartAbove (Point point)
    {
        Staff staff = getStaffAbove(point);

        if (staff == null) {
            return getFirstRealPart();
        } else {
            return staff.getPart();
        }
    }

    //-----------//
    // getPartAt //
    //-----------//
    /**
     * Determine the part which relates to the given point.
     *
     * @param point the given point
     * @return the containing part
     */
    public SystemPart getPartAt (Point point)
    {
        Staff staff = getStaffAt(point);

        if (staff == null) {
            return getFirstPart();
        } else {
            return staff.getPart();
        }
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Report the parts for this system.
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
     * Determine the real staff which is just above the given point.
     *
     * @param sysPt the given point
     * @return the staff above
     */
    public Staff getStaffAbove (Point sysPt)
    {
        Staff best = null;

        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            if (!part.isDummy()) {
                for (TreeNode n : part.getStaves()) {
                    Staff staff = (Staff) n;

                    if (staff.getCenter().y < sysPt.y) {
                        best = staff;
                    }
                }
            }
        }

        return best;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Determine the real staff which is closest to the given point.
     *
     * @param sysPt the given point
     * @return the closest staff
     */
    public Staff getStaffAt (Point sysPt)
    {
        int bestDy = Integer.MAX_VALUE;
        Staff best = null;

        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            if (!part.isDummy()) {
                for (TreeNode n : part.getStaves()) {
                    Staff staff = (Staff) n;
                    int dy = Math.abs(sysPt.y - staff.getCenter().y);

                    if (dy < bestDy) {
                        bestDy = dy;
                        best = staff;
                    }
                }
            }
        }

        return best;
    }

    //---------------//
    // getStaffBelow //
    //---------------//
    /**
     * Determine the staff which is just below the given system point.
     *
     * @param sysPt the given system point
     * @return the staff below
     */
    public Staff getStaffBelow (Point sysPt)
    {
        for (TreeNode node : getParts()) {
            SystemPart part = (SystemPart) node;

            for (TreeNode n : part.getStaves()) {
                Staff staff = (Staff) n;

                if (staff.getCenter().y > sysPt.y) {
                    return staff;
                }
            }
        }

        return null;
    }

    //------------------//
    // getStaffPosition //
    //------------------//
    /**
     * Report the vertical position of the provided point with respect
     * to the system staves.
     *
     * @param point the point whose ordinate is to be checked
     * @return the StaffPosition value
     */
    public StaffPosition getStaffPosition (Point point)
    {
        Staff firstStaff = getFirstRealPart()
                .getFirstStaff();

        if (point.y < firstStaff.getTopLeft().y) {
            return StaffPosition.ABOVE_STAVES;
        }

        Staff lastStaff = getLastPart()
                .getLastStaff();

        if (point.y > (lastStaff.getTopLeft().y + lastStaff.getHeight())) {
            return StaffPosition.BELOW_STAVES;
        } else {
            return StaffPosition.WITHIN_STAVES;
        }
    }

    //--------------//
    // getTextStaff //
    //--------------//
    /**
     * Report the related staff for a text at the provided point,
     * since some texts (direction, lyrics) are preferably assigned to
     * the staff above if any.
     *
     * @param role  the precise role of text glyph
     * @param point the provided point
     * @return the preferred staff
     */
    public Staff getTextStaff (TextRole role,
                               Point point)
    {
        Staff staff = null;

        if ((role == TextRole.Direction) || (role == TextRole.Lyrics)) {
            staff = getStaffAbove(point);
        }

        if (staff == null) {
            staff = getStaffAt(point);
        }

        return staff;
    }

    //------------//
    // getTopLeft //
    //------------//
    /**
     * Report the coordinates of the upper left corner of this system
     * in its containing score (not counting preceding dummy staves if
     * any).
     *
     * @return the top left corner
     */
    public Point getTopLeft ()
    {
        return topLeft;
    }

    //----------------//
    // isLeftOfStaves //
    //----------------//
    /**
     * Report whether the provided system point is on the left side of
     * the staves (on left of the starting barline).
     *
     * @param sysPt the system point to check
     * @return true if on left
     */
    public boolean isLeftOfStaves (Point sysPt)
    {
        return sysPt.x < topLeft.x;
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

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the system width.
     *
     * @param unitWidth the system width, in units
     */
    public void setWidth (int unitWidth)
    {
        Rectangle newBox = getBox();
        reset();

        newBox.width = unitWidth;
        setBox(newBox);
        getCenter();
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
}
