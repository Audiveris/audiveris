//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m P a r t                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.visitor.ScoreVisitor;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.List;

/**
 * Class <code>SystemPart</code> handles the various parts found in one system,
 * since the layout of parts may vary from system to system
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SystemPart
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemPart.class);

    //~ Instance fields --------------------------------------------------------

    /** Id of this part within the system, starting at 1 */
    private final int id;

    /** The corresponding ScorePart */
    private ScorePart scorePart;

    /** Specific child : sequence of staves that belong to this system */
    private final StaffList staves;

    /** Specific child : sequence of measures that compose this system part */
    private final MeasureList measures;

    /** Specific child : list of slurs */
    private final SlurList slurs;

    /** Specific child : list of wedges */
    private final WedgeList wedges;

    /** Lonesome child : Starting barline (the others are linked to measures) */
    private Barline startingBarline;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemPart //
    //------------//
    /**
     * Create a new instance of SystemPart
     *
     * @param system the containing system
     * @param id the part id within the system
     */
    public SystemPart (System system,
                       int    id)
    {
        super(system);
        this.id = id;

        // Allocate specific children
        staves = new StaffList(this);
        measures = new MeasureList(this);
        slurs = new SlurList(this);
        wedges = new WedgeList(this);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    /**
     * Report the first measure in this system part
     *
     * @return the first measure entity
     */
    public Measure getFirstMeasure ()
    {
        return (Measure) getMeasures()
                             .get(0);
    }

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * Report the first staff in this system aprt
     *
     * @return the first staff entity
     */
    public Staff getFirstStaff ()
    {
        return (Staff) getStaves()
                           .get(0);
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the part id within the containing system, starting at 1
     *
     * @return the part id
     */
    public int getId ()
    {
        return id;
    }

    //----------------//
    // getLastMeasure //
    //----------------//
    /**
     * Report the last measure in the system part
     *
     * @return the last measure entity
     */
    public Measure getLastMeasure ()
    {
        return (Measure) getMeasures()
                             .get(getMeasures().size() - 1);
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * Report the last staff in this system part
     *
     * @return the last staff entity
     */
    public Staff getLastStaff ()
    {
        return (Staff) getStaves()
                           .get(getStaves().size() - 1);
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure that contains a given point (assumed to be in the
     * containing system part)
     *
     * @param systemPoint system-based coordinates of the given point
     * @return the containing measure
     */
    public Measure getMeasureAt (SystemPoint systemPoint)
    {
        Measure measure = null;

        for (TreeNode node : getMeasures()) {
            measure = (Measure) node;

            if (systemPoint.x <= measure.getBarline()
                                        .getRightX()) {
                return measure;
            }
        }

        return measure;
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the collection of measures
     *
     * @return the measure list, which may be empty but not null
     */
    public List<TreeNode> getMeasures ()
    {
        return measures.getChildren();
    }

    //--------------//
    // setScorePart //
    //--------------//
    public void setScorePart (ScorePart scorePart)
    {
        this.scorePart = scorePart;
    }

    //--------------//
    // getScorePart //
    //--------------//
    public ScorePart getScorePart ()
    {
        return scorePart;
    }

    //----------//
    // getSlurs //
    //----------//
    /**
     * Report the collection of slurs
     *
     * @return the slur list, which may be empty but not null
     */
    public List<TreeNode> getSlurs ()
    {
        return slurs.getChildren();
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff nearest (in ordinate) to a provided page point
     *
     * @param point the provided page point
     *
     * @return the nearest staff
     */
    public Staff getStaffAt (PagePoint point)
    {
        int   minDy = Integer.MAX_VALUE;
        Staff best = null;

        for (TreeNode node : getStaves()) {
            Staff staff = (Staff) node;
            int   midY = staff.getTopLeft().y + (staff.getHeight() / 2);
            int   dy = Math.abs(point.y - midY);

            if (dy < minDy) {
                minDy = dy;
                best = staff;
            }
        }

        return best;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff nearest (in ordinate) to a provided system point
     *
     * @param point the provided system point
     *
     * @return the nearest staff
     */
    public Staff getStaffAt (SystemPoint point)
    {
        return getStaffAt(getSystem().toPagePoint(point));
    }

    //--------------------//
    // setStartingBarline //
    //--------------------//
    /**
     * Set the barline that starts the part
     *
     * @param startingBarline the starting barline
     */
    public void setStartingBarline (Barline startingBarline)
    {
        this.startingBarline = startingBarline;
    }

    //--------------------//
    // getStartingBarline //
    //--------------------//
    /**
     * Get the barline that starts the part
     *
     * @return barline the starting bar line (which may be null)
     */
    public Barline getStartingBarline ()
    {
        return startingBarline;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the ordered list of staves that belong to this system part
     *
     * @return the list of staves
     */
    public List<TreeNode> getStaves ()
    {
        return staves.getChildren();
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return the containing system
     */
    @Override
    public System getSystem ()
    {
        return (System) getParent();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Overrides normal behavior, to deal with the separation of specific children
     *
     * @param node the node to insert
     */
    @Override
    public void addChild (TreeNode node)
    {
        // Specific children lists
        if (node instanceof Staff) {
            staves.addChild(node);
        } else if (node instanceof Measure) {
            measures.addChild(node);
        } else if (node instanceof Slur) {
            slurs.addChild(node);
        } else if (node instanceof Wedge) {
            wedges.addChild(node);
        } else {
            super.addChild(node);
        }
    }

    //---------------//
    // barlineExists //
    //---------------//
    public boolean barlineExists (int x,
                                  int maxShiftDx)
    {
        for (TreeNode node : getMeasures()) {
            Measure measure = (Measure) node;

            if (Math.abs(measure.getBarline().getCenter().x - x) <= maxShiftDx) {
                return true;
            }
        }

        return false; // Not found
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{SystemPart #")
          .append(getId())
          .append(" [");

        for (TreeNode node : getStaves()) {
            Staff staff = (Staff) node;
            sb.append(staff.getId() + " ");
        }

        sb.append("]}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // MeasureList //
    //-------------//
    private static class MeasureList
        extends ScoreNode
    {
        MeasureList (SystemPart container)
        {
            super(container);
        }
    }

    //----------//
    // SlurList //
    //----------//
    private static class SlurList
        extends ScoreNode
    {
        SlurList (SystemPart container)
        {
            super(container);
        }
    }

    //-----------//
    // StaffList //
    //-----------//
    private static class StaffList
        extends ScoreNode
    {
        StaffList (SystemPart container)
        {
            super(container);
        }
    }

    //-----------//
    // WedgeList //
    //-----------//
    private static class WedgeList
        extends ScoreNode
    {
        WedgeList (SystemPart container)
        {
            super(container);
        }
    }
}
