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

import omr.score.visitor.Visitor;

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
    extends ScoreNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemPart.class);

    //~ Instance fields --------------------------------------------------------

    /** Id of this part within the system, starting at 1 */
    private final int id;

    /** Specific child : sequence of staves that belong to this system */
    private final StaffList staves;

    /** Specific child : sequence of measures that compose this system part */
    private final MeasureList measures;

    /** Specific child : list of slurs */
    private final SlurList slurs;

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
    public System getSystem ()
    {
        return (System) getContainer();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
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
            node.setContainer(staves);
        } else if (node instanceof Measure) {
            measures.addChild(node);
            node.setContainer(measures);
        } else if (node instanceof Slur) {
            slurs.addChild(node);
            node.setContainer(slurs);
        } else {
            super.addChild(node);
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding center
     * within the containing system part
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param shape the shape whose icon must be painted
     * @param center part-based bounding center in units
     */
    public void paintSymbol (Graphics    g,
                             Zoom        zoom,
                             Shape       shape,
                             SystemPoint center)
    {
        if (shape == null) {
            logger.warning("No shape to paint");
        } else {
            SymbolIcon icon = (SymbolIcon) shape.getIcon();

            if (icon == null) {
                logger.warning("No icon defined for shape " + shape);
            } else if (center == null) {
                logger.warning("No center defined for " + shape);
            } else {
                ScorePoint origin = getSystem()
                                        .getDisplayOrigin();
                g.drawImage(
                    icon.getImage(),
                    zoom.scaled(origin.x + center.x) -
                    (icon.getActualWidth() / 2),
                    zoom.scaled(origin.y + center.y) -
                    (icon.getIconHeight() / 2),
                    null);
            }
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding center
     * within the containing system part, forcing adjacency with provided chord
     * stem.
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param shape the shape whose icon must be painted
     * @param center part-based bounding center in units
     * @param chord the chord stem to attach the symbol to
     */
    public void paintSymbol (Graphics    g,
                             Zoom        zoom,
                             Shape       shape,
                             SystemPoint center,
                             Chord       chord)
    {
        if (shape == null) {
            logger.warning("No shape to paint");
        } else {
            SymbolIcon icon = (SymbolIcon) shape.getIcon();

            if (icon == null) {
                logger.warning("No icon defined for shape " + shape);
            } else if (center == null) {
                logger.warning("No center defined for " + shape);
            } else if (chord == null) {
                logger.warning("No chord defined for " + shape);
            } else {
                ScorePoint origin = getSystem()
                                        .getDisplayOrigin();

                // Position of symbol wrt stem
                int stemX = chord.getTailLocation().x;
                int iconX = zoom.scaled(origin.x + stemX);

                if (center.x < stemX) {
                    // Symbol is on left side of stem (-1 is for stem width)
                    iconX -= icon.getActualWidth() -1;
                }

                g.drawImage(
                    icon.getImage(),
                    iconX,
                    zoom.scaled(origin.y + center.y) -
                    (icon.getIconHeight() / 2),
                    null);
            }
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol using its pitch position for ordinate in the containing
     * staff
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param shape the shape whose icon must be painted
     * @param center part-based coordinates of bounding center in units (only
     *               abscissa is actually used)
     * @param staff the related staff
     * @param pitchPosition staff-based ordinate in step lines
     */
    public void paintSymbol (Graphics    g,
                             Zoom        zoom,
                             Shape       shape,
                             SystemPoint center,
                             Staff       staff,
                             double      pitchPosition)
    {
        if (shape == null) {
            logger.warning("No shape to paint");
        } else {
            SymbolIcon icon = (SymbolIcon) shape.getIcon();

            if (icon == null) {
                logger.warning("No icon defined for shape " + shape);
            } else if (center == null) {
                logger.warning("No center defined for " + shape);
            } else if (staff == null) {
                logger.warning("No staff defined for " + shape);
            } else {
                ScorePoint origin = staff.getDisplayOrigin();
                int        dy = staff.pitchToUnit(pitchPosition);
                Point      refPoint = icon.getRefPoint();
                int        refY = (refPoint == null) ? icon.getCentroid().y
                                  : refPoint.y;
                g.drawImage(
                    icon.getImage(),
                    zoom.scaled(origin.x + center.x) -
                    (icon.getActualWidth() / 2),
                    zoom.scaled(origin.y + dy) - refY,
                    null);
            }
        }
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
            sb.append(staff.getStaffIndex() + " ");
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
}
