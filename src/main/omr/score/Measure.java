//-----------------------------------------------------------------------//
//                                                                       //
//                             M e a s u r e                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.score;

import omr.lag.Lag;
import omr.sheet.BarInfo;
import omr.util.Dumper;
import omr.util.Logger;
import omr.ui.Zoom;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Measure</code> handles a measure of a staff.
 */
public class Measure
        extends StaveNode
{
    //~ Static variables/initializers ----------------------------------------

    private static final Logger logger = Logger.getLogger(Measure.class);

    //~ Instance variables ---------------------------------------------------

    // Related infos from sheet analysis
    private List<BarInfo> infos = new ArrayList<BarInfo>(2);

    // Attributes
    private Barline linetype;
    private int leftlinex;
    private int rightlinex;
    private boolean lineinvented;
    private int id = 0; // Measure Id
    private int leftX; // X of start of this measure (wrt stave)

    //~ Constructors ---------------------------------------------------------

    //---------//
    // Measure //
    //---------//

    /**
     * Default constructor (needed by Castor)
     */
    public Measure ()
    {
        super(null, null);
    }

    //---------//
    // Measure //
    //---------//

    /**
     * Create a measure with the specified parameters
     *
     * @param info         physical description of the ending bar line
     * @param stave        the containing stave
     * @param linetype     the kind of ending bar line
     * @param leftlinex    abscissa of the left part of the ending bar line
     * @param rightlinex   abscissa of the right part of the ending bar line
     * @param lineinvented flag an artificial ending bar line if none existed
     */
    public Measure (BarInfo info,
                    Stave stave,
                    Barline linetype,
                    int leftlinex,
                    int rightlinex,
                    boolean lineinvented)
    {
        super(stave, stave);

        this.infos.add(info);
        this.linetype = linetype;
        this.leftlinex = leftlinex;
        this.rightlinex = rightlinex;
        this.lineinvented = lineinvented;

        if (logger.isDebugEnabled()) {
            Dumper.dump(this, "Constructed");
        }
    }

    //~ Methods --------------------------------------------------------------

    //----------//
    // getInfos //
    //----------//

    /**
     * Report the BarInfo list related to the ending bar line(s)
     *
     * @return the BarInfo list
     */
    public List<BarInfo> getInfos ()
    {
        return infos;
    }

    //--------------//
    // setLeftlinex //
    //--------------//

    /**
     * Set the abscissa of the left part of the ending bar line (needed for
     * Castor unmarshalling)
     *
     * @param leftlinex the abscissa (in units)
     */
    public void setLeftlinex (int leftlinex)
    {
        this.leftlinex = leftlinex;
    }

    //--------------//
    // getLeftlinex //
    //--------------//

    /**
     * Report the abscissa of the left part of the ending bar line
     *
     * @return the abscissa (in units)
     */
    public int getLeftlinex ()
    {
        return leftlinex;
    }

    //-----------------//
    // setLineinvented //
    //-----------------//

    /**
     * Set the flag on artificial existence of the ending bar line (needed for
     * Castor unmarshalling)
     *
     * @param lineinvented true if artificial
     */
    public void setLineinvented (boolean lineinvented)
    {
        this.lineinvented = lineinvented;
    }

    //-----------------//
    // getLineinvented //
    //-----------------//

    /**
     * Report the flag on artificial existence of the ending bar line
     *
     * @return true if no physical ending bar line
     */
    public boolean getLineinvented ()
    {
        return lineinvented;
    }

    //-------------//
    // setLinetype //
    //-------------//

    /**
     * Set the line type of the ending bar line
     *
     * @param linetype the line type, as the proper enumerated type
     */
    public void setLinetype (Barline linetype)
    {
        this.linetype = linetype;
    }

    //-------------//
    // setLinetype //
    //-------------//

    /**
     * Set the line type of the ending bar line (needed for Castor
     * unmarshalling)
     *
     * @param linetype the line type, as a string
     */
    public void setLinetype (String linetype)
    {
        setLinetype(Barline.valueOf(linetype));
    }

    //-------------//
    // getLinetype //
    //-------------//

    /**
     * Report the type of the ending bar line
     *
     * @return the enumerated line type
     */
    public String getLinetype ()
    {
        return linetype.toString();
    }

    //---------------//
    // setRightlinex //
    //---------------//

    /**
     * Set the abscissa of the right part of the ending bar line (needed for
     * Castor unmarshalling)
     *
     * @param rightlinex the abscissa (in units)
     */
    public void setRightlinex (int rightlinex)
    {
        this.rightlinex = rightlinex;
    }

    //---------------//
    // getRightlinex //
    //---------------//

    /**
     * Report the abscissa of the right part of the ending bar line
     *
     * @return the abscissa (in units)
     */
    public int getRightlinex ()
    {
        return rightlinex;
    }

    //----------//
    // addInfos //
    //----------//

    /**
     * Merge the provided barinfos with existing one
     *
     * @param list list of bar info objects
     */
    public void addInfos (List<BarInfo> list)
    {
        this.infos.addAll(list);
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
    protected boolean colorizeNode (Lag lag,
                                    int viewIndex,
                                    Color color)
    {
        // Set color for the sections of the ending bars
        for (BarInfo bar : infos) {
            bar.colorize(lag, viewIndex, color);
        }

        return true;
    }

    //-------------//
    // computeNode //
    //-------------//

    /**
     * Overriding definition, so that computations specific to a measure are
     * performed
     *
     * @return true, so that processing continues
     */
    protected boolean computeNode ()
    {
        // Fix the stave reference
        setStave((Stave) container.getContainer());

        // First/Last measure ids
        stave.incrementLastMeasureId();
        id = stave.getLastMeasureId();

        // Start of the measure
        Measure prevMeasure = (Measure) getPreviousSibling();

        if (prevMeasure == null) { // Very first measure in the stave
            leftX = 0;
        } else {
            leftX = prevMeasure.rightlinex;
        }

        if (logger.isDebugEnabled()) {
            Dumper.dump(this, "Computed");
        }

        return true;
    }

    //-----------//
    // paintNode //
    //-----------//

    /**
     * Renders the measure in the provided graphic context
     *
     * @param g the graphic context to be used
     *
     * @return true if painting of children must be done also
     */
    protected boolean paintNode (Graphics g)
    {
        Point origin = getOrigin();

//         // Draw the bar line symbol
//         Symbols.drawPos(g, linetype.getImage(), origin, 0,
//                         (leftlinex + rightlinex) / 2, Symbols.CENTER);

        // Draw the measure id, if on the first stave only
        if (stave.getStavelink() == 0) {
            Zoom zoom = ScoreView.getZoom();
            g.setColor(Color.lightGray);
            g.drawString(Integer.toString(id),
                         zoom.scaled(origin.x + leftX) - 5,
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
    protected boolean renderNode (Graphics g,
                                  Zoom z)
    {
        for (BarInfo bar : infos) {
            bar.render(g, z);
        }

        return true;
    }
}
