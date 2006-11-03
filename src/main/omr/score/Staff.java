//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t a f f                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Shape;

import omr.lag.Lag;
import static omr.score.ScoreConstants.*;
import omr.score.visitor.Visitor;

import omr.sheet.PixelPoint;
import omr.sheet.StaffInfo;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.List;

/**
 * Class <code>Staff</code> handles a staff in a system.
 *
 * <p/> It comprises measures, lyric info, text and dynamic indications. Each
 * kind of these elements is kept in a dedicated collection, so the direct
 * children of a staff are in fact these 4 collections. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Staff
    extends MusicNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Staff.class);

    //~ Instance fields --------------------------------------------------------

    /** Starting bar line (the others are linked to measures) */
    private Barline startingBarline;

    /** List of dynamics if any */
    private DynamicList dynamics;

    /** List of lyrics if any */
    private LyricList lyriclines;

    /** Specific children */
    private MeasureList measures;

    /** Top left corner of the staff (relative to the system top left corner) */
    private PagePoint topLeft;

    /** Actual cached display origin */
    private ScorePoint origin;

    /** Related info from sheet analysis */
    private StaffInfo info;

    /** List of tests if any */
    private TextList texts;

    /** Id of first measure */
    private int firstMeasureId;

    /** Id of last measure */
    private int lastMeasureId;

    /** Index of staff in containing system */
    private int stafflink;

    /** Staff height (units) */
    private int size;

    /** Staff width (units) */
    private int width;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Staff //
    //-------//
    /**
     * Default constructor (needed by XML binder)
     */
    public Staff ()
    {
        super(null);
        allocateChildren();

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Construction");
        }
    }

    //-------//
    // Staff // Building this Staff
    //-------//
    /**
     * Build a staff, given all its parameters
     *
     * @param info the physical information read from the sheet
     * @param system the containing system
     * @param topLeft the coordinate,in units, wrt to the score upper left
     *                  corner, of the upper left corner of this staff
     * @param width the staff width, in units
     * @param size the staff height, in units ??? TBD ???
     * @param stafflink the index of the staff in the containing system,
     *                  starting at 0
     */
    public Staff (StaffInfo info,
                  System    system,
                  PagePoint topLeft,
                  int       width,
                  int       size,
                  int       stafflink)
    {
        super(system);
        allocateChildren();

        this.info = info;
        this.topLeft = topLeft;
        this.width = width;
        this.size = size;
        this.stafflink = stafflink;

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // setDynamics //
    //-------------//
    /**
     * Set the dynamics collection
     *
     * @param dynamics the dynamics collection
     */
    public void setDynamics (List<TreeNode> dynamics)
    {
        this.dynamics = new DynamicList(this);
        getDynamics()
            .addAll(dynamics);
    }

    //-------------//
    // getDynamics //
    //-------------//
    /**
     * Report the dynamics collection
     *
     * @return the dynamics collection
     */
    public List<TreeNode> getDynamics ()
    {
        return dynamics.getChildren();
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    /**
     * Report the first measure in this staff
     *
     * @return the first measure entity
     */
    public Measure getFirstMeasure ()
    {
        return (Measure) getMeasures()
                             .get(0);
    }

    //-------------------//
    // setFirstMeasureId //
    //-------------------//
    /**
     * Assign id of first measure
     *
     * @param firstMeasureId first measure id
     */
    public void setFirstMeasureId (int firstMeasureId)
    {
        this.firstMeasureId = firstMeasureId;
    }

    //-------------------//
    // getFirstMeasureId //
    //-------------------//
    /**
     * Report the id (0 is the very first measure id in the score) of the first
     * measure of the staff
     *
     * @return the measure id of the first measure
     */
    public int getFirstMeasureId ()
    {
        return firstMeasureId;
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the physical information retrieved from the sheet
     *
     * @return the info entity for this staff
     */
    public StaffInfo getInfo ()
    {
        return info;
    }

    //----------------//
    // getLastMeasure //
    //----------------//
    /**
     * Report the last measure in the staff
     *
     * @return the laste measure entity
     */
    public Measure getLastMeasure ()
    {
        return (Measure) getMeasures()
                             .get(getMeasures().size() - 1);
    }

    //------------------//
    // setLastMeasureId //
    //------------------//
    /**
     * Assign id of last measure
     *
     * @param lastMeasureId last measure id
     */
    public void setLastMeasureId (int lastMeasureId)
    {
        this.lastMeasureId = lastMeasureId;
    }

    //------------------//
    // getLastMeasureId //
    //------------------//
    /**
     * Report the id of the last measure in the staff
     *
     * @return the measure id f the last measure
     */
    public int getLastMeasureId ()
    {
        return lastMeasureId;
    }

    //---------------//
    // setLyriclines //
    //---------------//
    /**
     * Set the collection of lyrics
     *
     * @param lyriclines the collection of lyrics
     */
    public void setLyriclines (List<TreeNode> lyriclines)
    {
        this.lyriclines = new LyricList(this);
        getLyriclines()
            .addAll(lyriclines);
    }

    //---------------//
    // getLyriclines //
    //---------------//
    /**
     * Report the collection of lyrics
     *
     * @return the lyrics collection, which may be empty
     */
    public List<TreeNode> getLyriclines ()
    {
        return lyriclines.getChildren();
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure that contains a given point (assumed to be in the
     * containing staff)
     *
     * @param staffPoint staff-based coordinates of the given point
     * @return the containing measure
     */
    public Measure getMeasureAt (StaffPoint staffPoint)
    {
        Measure measure = null;

        for (TreeNode node : getMeasures()) {
            measure = (Measure) node;

            if (staffPoint.x <= measure.getBarline()
                                       .getRightX()) {
                return measure;
            }
        }

        return measure;
    }

    //-------------//
    // setMeasures //
    //-------------//
    /**
     * Set the collection of measures
     *
     * @param measures the collection of measures
     */
    public void setMeasures (List<TreeNode> measures)
    {
        if (getMeasures() != measures) {
            getMeasures()
                .clear();
            getMeasures()
                .addAll(measures);
        }
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the collection of measures
     *
     * @return the list of measures
     */
    public List<TreeNode> getMeasures ()
    {
        return measures.getChildren();
    }

    //-----------//
    // getOrigin //
    //-----------//
    /**
     * Report the display origin in the score display of this staff
     *
     * @return the origin
     */
    public ScorePoint getOrigin ()
    {
        return origin;
    }

    //---------//
    // setSize //
    //---------//
    /**
     * Set the height of the staff
     *
     * @param size height in units
     */
    public void setSize (int size)
    {
        this.size = size;
    }

    //---------//
    // getSize //
    //---------//
    /**
     * Report the height of the staff
     *
     * @return height in units (to be confirmed TBD)
     */
    public int getSize ()
    {
        return size;
    }

    //--------------//
    // setStafflink //
    //--------------//
    /**
     * Set the staff index in the containing system
     *
     * @param stafflink system relative index, starting from 0
     */
    public void setStafflink (int stafflink)
    {
        this.stafflink = stafflink;
    }

    //--------------//
    // getStafflink //
    //--------------//
    /**
     * Report the staff index within the containing system
     *
     * @return the index, counting from 0
     */
    public int getStafflink ()
    {
        return stafflink;
    }

    //----------------//
    // setStartingBar //
    //----------------//
    /**
     * Set the bar line that starts the staff
     *
     * @param startingBarline the starting bar line
     */
    public void setStartingBar (Barline startingBarline)
    {
        this.startingBarline = startingBarline;
    }

    //--------------------//
    // getStartingBarline //
    //--------------------//
    /**
     * Get the barline that starts the staff
     *
     * @return barline the starting bar line (which may be null)
     */
    public Barline getStartingBarline ()
    {
        return startingBarline;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return the system entity
     */
    public System getSystem ()
    {
        // Beware, staff is not a direct child of System, there is an
        // intermediate StaffList to skip
        return (System) container.getContainer();
    }

    //------------//
    // setTopLeft //
    //------------//
    /**
     * Set the coordinates of the top left point in the score
     *
     * @param topLeft coordinates in units, wrt system top left corner
     */
    public void setTopLeft (PagePoint topLeft)
    {
        this.topLeft = topLeft;

        // Invalidate the coordinates of all contained staff-based nodes
        for (TreeNode treeNode : children) {
            StaffNode node = (StaffNode) treeNode;
            node.setStaff(this);
        }
    }

    //------------//
    // getTopLeft //
    //------------//
    /**
     * Report the coordinates of the top left corner of the staff, wrt to the
     * score
     *
     * @return the top left coordinates
     */
    public PagePoint getTopLeft ()
    {
        return topLeft;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set the staff width
     *
     * @param width width in units f the staff
     */
    public void setWidth (int width)
    {
        this.width = width;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the staff
     *
     * @return the width in units
     */
    public int getWidth ()
    {
        return width;
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
    // addChild // Overrides normal behavior
    //----------//
    /**
     * Override normal behavior, so that a given child is store in its proper
     * type collection (measure to measure list, lyrics to lyrics list, etc...)
     *
     * @param node the child to insert in the staff
     */
    @Override
    public void addChild (TreeNode node)
    {
        if (node instanceof Measure) {
            measures.addChild(node);
            node.setContainer(measures);

            //      } else if (node instanceof Lyricline) {
            //          lyriclines.addChild (node);
            //          node.setContainer (lyriclines);
            //      } else if (node instanceof Text) {
            //          texts.addChild (node);
            //          node.setContainer (texts);
            //      } else if (node instanceof Dynamic) {
            //          dynamics.addChild (node);
            //          node.setContainer (dynamics);
        } else if (node instanceof TreeNode) {
            // Meant for the 4 lists
            children.add(node);
            node.setContainer(this);
        } else {
            // Programming error
            Dumper.dump(node);
            logger.severe("Staff node not known");
        }
    }

    //---------------//
    // barlineExists //
    //---------------//
    /**
     * Check whether there is a measure with leftlinex = x (within dx error).
     * This is used in Bars retrieval, to check that the bar candidate is
     * actually a bar, since there is a measure starting at this abscissa in
     * each staff of the system (this test is relevant only for systems with
     * more than one staff).
     *
     * @param x  starting abscissa of the desired measure
     * @param dx tolerance in abscissa
     *
     * @return true if bar line found within tolerance, false otherwise
     */
    public boolean barlineExists (int x,
                                  int dx)
    {
        logger.fine("x=" + x + " dx=" + dx);

        for (TreeNode node : getMeasures()) {
            Measure measure = (Measure) node;

            logger.fine("center.x =" + measure.getBarline().getCenter().x);

            if (Math.abs(measure.getBarline().getCenter().x - x) <= dx) {
                logger.fine("found");

                return true;
            }
        }

        logger.fine("noLine");

        return false; // Not found
    }

    //------------------------//
    // incrementLastMeasureId //
    //------------------------//
    /**
     * Methd called to signal a new measure built in this staff
     */
    public void incrementLastMeasureId ()
    {
        getSystem()
            .setLastMeasureId(++lastMeasureId);
    }

    //-------------//
    // pitchToUnit //
    //-------------//
    /**
     * Compute the ordinate Y (counted in units and measured from staff origin)
     * that corresponds to a given step line
     *
     * @param pitchPosition the pitch position (-4 for top line, +4 for bottom
     *                      line)
     * @return the ordinate in pixels, counted from staff origin (upper line),
     * so top line is 0px and bottom line is 64px (with an inter line of 16).
     */
    public static int pitchToUnit (double pitchPosition)
    {
        return (int) Math.rint(((pitchPosition + 4) * INTER_LINE) / 2.0);
    }

    //--------------//
    // toStaffPoint //
    //--------------//
    /**
     * Report the staff-based coordinates (wrt staff top left corner) of a point
     * with page-based coordinates
     *
     * @param pagePoint the coordinates within page
     * @return coordinates within the containing staff
     */
    public StaffPoint toStaffPoint (PagePoint pagePoint)
    {
        return new StaffPoint(pagePoint.x - topLeft.x, pagePoint.y - topLeft.y);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based of the location and size of the staff
     */
    public String toString ()
    {
        return "{Staff" + " topLeft=" + topLeft + " width=" + width + " size=" +
               size + " origin=" + origin + "}";
    }

    //-------------//
    // unitToPitch //
    //-------------//
    /**
     * Compute the pitch position of a given ordinate Y (counted in units and
     * measured from staff origin)
     *
     * @param unit the ordinate in pixel units, counted from staff origin (upper
     * line), so top line is 0px and bottom line is 64px (with an inter line of
     * 16).
     * @return the pitch position (-4 for top line, +4 for bottom line)
     */
    public static int unitToPitch (int unit)
    {
        return (int) Math.rint(((2D * unit) - (4D * INTER_LINE)) / INTER_LINE);
    }

    //-----------//
    // setOrigin //
    //-----------//
    /**
     * Assign proper staff display origin
     *
     * @param origin staff display origin
     */
    public void setOrigin (ScorePoint origin)
    {
        this.origin = origin;
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol using its pitch position for ordinate in the containing
     * staff
     *
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param shape the shape whose icon must be painted
     * @param center staff-based coordinates of bounding center in units (only
     *               abscissa is actually used)
     * @param pitchPosition staff-based ordinate in step lines
     */
    public void paintSymbol (Graphics   g,
                             Zoom       zoom,
                             Shape      shape,
                             StaffPoint center,
                             double     pitchPosition)
    {
        if (shape == null) {
            logger.warning("No shape to paint");
        } else {
            SymbolIcon icon = (SymbolIcon) shape.getIcon();

            if (icon == null) {
                logger.warning("No icon defined for shape " + shape);
            } else if (center == null) {
                logger.warning("Need bounding center for " + icon.getName());
            } else {
                ScorePoint origin = getOrigin();
                int        dy = pitchToUnit(pitchPosition);
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

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding center
     * within the containing staff
     *
     * @param g graphical context
     * @param zoom display zoom
     * @param shape the shape whose icon must be painted
     * @param center staff-based bounding center in units
     */
    public void paintSymbol (Graphics   g,
                             Zoom       zoom,
                             Shape      shape,
                             StaffPoint center)
    {
        if (shape == null) {
            logger.warning("No shape to paint");
        } else {
            SymbolIcon icon = (SymbolIcon) shape.getIcon();

            if (icon == null) {
                logger.warning("No icon defined for shape " + shape);
            } else if (center == null) {
                logger.warning("Need area center for " + icon.getName());
            } else {
                ScorePoint origin = getOrigin();
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

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute the pitch position of a pixel point
     *
     * @param pt the pixel point
     * @return the pitch position
     */
    public double pitchPositionOf (PixelPoint pt)
    {
        return info.pitchPositionOf(pt);
    }

    //------------------//
    // allocateChildren //
    //------------------//
    private void allocateChildren ()
    {
        // Allocate specific children lists
        measures = new MeasureList(this);
        lyriclines = new LyricList(this);
        texts = new TextList(this);
        dynamics = new DynamicList(this);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // DynamicList //
    //-------------//
    private static class DynamicList
        extends StaffNode
    {
        DynamicList (Staff staff)
        {
            super(staff, staff);
        }
    }

    //-----------//
    // LyricList //
    //-----------//
    private static class LyricList
        extends StaffNode
    {
        LyricList (Staff staff)
        {
            super(staff, staff);
        }
    }

    //-------------//
    // MeasureList //
    //-------------//
    private static class MeasureList
        extends StaffNode
    {
        MeasureList (Staff staff)
        {
            super(staff, staff);
        }
    }

    //----------//
    // TextList //
    //----------//
    private static class TextList
        extends StaffNode
    {
        TextList (Staff staff)
        {
            super(staff, staff);
        }
    }
}
