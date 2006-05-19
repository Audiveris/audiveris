//-----------------------------------------------------------------------//
//                                                                       //
//                             M e a s u r e                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.glyph.Shape;
import omr.lag.Lag;
import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;
import omr.util.Dumper;
import omr.util.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import omr.util.TreeNode;

/**
 * Class <code>Measure</code> handles a measure of a staff.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Measure
    extends StaffNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Measure.class);

    //~ Instance variables ------------------------------------------------

    // Ending bar line
    private Barline barline;

    // For measure with no physical ending bar line
    private boolean lineinvented;

    // Measure Id
    private int id = 0;

    // Left abscissa (in units) of this measure
    private Integer leftX;

    // Specific children
    private ClefList clefs;
//     private KeysigList keysigs;
//     private ChordList chords;


    //~ Constructors ------------------------------------------------------

    //---------//
    // Measure //
    //---------//
    /**
     * Default constructor (needed by XML Binder)
     */
    public Measure ()
    {
        super(null, null);
        allocateChildren();
    }

    //---------//
    // Measure //
    //---------//
    /**
     * Create a measure with the specified parameters
     *
     *
     * @param staff        the containing staff
     * @param barline the ending bar line
     * @param lineinvented flag an artificial ending bar line if none existed
     */
    public Measure (Staff   staff,
                    Barline barline,
                    boolean lineinvented)
    {
        super(staff, staff);

        this.barline = barline;
        this.lineinvented = lineinvented;

        allocateChildren();

        if (logger.isFineEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods -----------------------------------------------------------

    //-------//
    // reset //
    //-------//
    /**
     * Reset the coordinates of the measure, they will be lazily recomputed
     * when needed
     */
    public void reset()
    {
        leftX = null;
        barline.reset();
    }

    //------------//
    // getBarline //
    //------------//
    /**
     * Report the ending bar line
     *
     * @return the ending bar line
     */
    public Barline getBarline ()
    {
        return barline;
    }

    //------------//
    // setBarline //
    //------------//
    /**
     * Set the ending bar line
     *
     *
     * @param barline the ending bar line
     */
    public void setBarline (Barline barline)
    {
        this.barline = barline;
    }

    //----------//
    // getClefs //
    //----------//
    /**
     * Report the collection of clefs
     *
     * @return the list of clefs
     */
    public List<TreeNode> getClefs ()
    {
        return clefs.getChildren();
    }

    //--------------//
    // colorizeNode //
    //--------------//
    /**
     * Colorize the physical information of this measure
     *
     * @param lag       the lag to be colorized
     * @param viewIndex the provided lag view index
     * @param color     the color to be used
     *
     * @return true if processing must continue
     */
    @Override
    protected boolean colorizeNode (Lag lag,
                                    int viewIndex,
                                    Color color)
    {
        // Set color for the sections of the ending bar lines
        barline.colorize(lag, viewIndex, color);

        return true;
    }

    //-------------//
    // computeNode //
    //-------------//
    /**
     * Overriding definition, so that computations specific to a measure
     * are performed
     *
     * @return true, so that processing continues
     */
    @Override
        protected boolean computeNode ()
    {
        super.computeNode();

        // Fix the staff reference
        setStaff((Staff) container.getContainer());

        // First/Last measure ids
        staff.incrementLastMeasureId();
        id = staff.getLastMeasureId();

        return true;
    }

    //-----------//
    // paintNode //
    //-----------//
    @Override
        protected boolean paintNode (Graphics  g,
                                     Zoom      zoom,
                                     Component comp)
    {
        Point origin = getOrigin();

        // Draw the bar line symbol at the end of the measure
        barline.paintItem(g, zoom, comp);

        // Draw the measure id, if on the first staff only
        if (staff.getStafflink() == 0) {
            g.setColor(Color.lightGray);
            g.drawString(Integer.toString(id),
                         zoom.scaled(origin.x + getLeftX()) - 5,
                         zoom.scaled(origin.y) - 15);
        }

        return true;
    }

    //------------//
    // renderNode //
    //------------//
    /**
     * Render the physical information of this measure
     *
     * @param g the graphics context
     * @param z the display zoom
     *
     * @return true if rendered
     */
    @Override
    protected boolean renderNode (Graphics g,
                                  Zoom z)
    {
        barline.render(g, z);

        return true;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on main members
     */
    @Override
    public String toString ()
    {
        return "{Measure id=" + id + " bar=" + barline + "}";
    }

    //~ Methods private ---------------------------------------------------

    //----------//
    // getLeftX //
    //----------//
    private int getLeftX()
    {
        if (leftX == null) {

            // Start of the measure
            Measure prevMeasure = (Measure) getPreviousSibling();

            if (prevMeasure == null) { // Very first measure in the staff
                leftX = 0;
            } else {
                leftX = prevMeasure.getBarline().getCenter().x;
            }
        }

        return leftX;

    }

    //------------------//
    // allocateChildren //
    //------------------//
    private void allocateChildren ()
    {
        // Allocate specific children lists
        clefs = new ClefList(this, staff);
//         chords = new ChordList(this, staff);
//         keysigs = new KeysigList(this, staff);
    }

    //~ Classes -----------------------------------------------------------

    //----------//
    // ClefList //
    //----------//
    private static class ClefList
        extends StaffNode
    {
        ClefList (StaffNode container,
                Staff staff)
        {
            super(container, staff);
        }
    }
}
