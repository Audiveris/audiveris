//-----------------------------------------------------------------------//
//                                                                       //
//                                M a r k                                //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.util.Logger;
import omr.ui.Zoom;

import java.awt.*;

/**
 * Class <code>Mark</code> handles a visible mark, related to a location.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Mark
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Mark.class);

    //~ Instance variables ------------------------------------------------

    private final Color color;
    private final Stroke stroke;
    private final Rectangle rect;
    private final Point center = new Point();
    private boolean active = false;

    //~ Constructors ------------------------------------------------------

    //------//
    // Mark //
    //------//

    /**
     * Create a Mark, displayed in the provided color, and with default
     * appearance parameters.
     *
     * @param color the color to be used for this mark
     */
    public Mark (Color color)
    {
        this(color, 1.5f, // thickness
             20, // dx
             20); // dy
    }

    //------//
    // Mark //
    //------//

    /**
     * Create a Mark, with all parameters provided.
     *
     * @param color     the color to be used when the mark is rendered
     * @param thickness thickness of the mark stroke
     * @param dx        half width of the mark
     * @param dy        half height of the mark
     */
    public Mark (Color color,
                 float thickness,
                 int dx,
                 int dy)
    {
        this.color = color;
        this.stroke = new BasicStroke(thickness);
        this.rect = new Rectangle(0, 0, 2 * dx, 2 * dy);

        if (logger.isDebugEnabled()) {
            logger.debug("new Mark");
        }
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // isActive //
    //----------//

    /**
     * Predicate to report the current active status of the mark.
     *
     * @return true if active, false otherwise.
     */
    public boolean isActive ()
    {
        return active;
    }

    //-------------//
    // setLocation //
    //-------------//

    /**
     * Update the location of the Mark, forcing it to the active status.
     *
     * @param pt the new location
     */
    public void setLocation (Point pt)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("setLocation " + pt);
        }

        center.x = pt.x;
        center.y = pt.y;
        activate(true);
    }

    //-------------//
    // getLocation //
    //-------------//

    /**
     * Report the current (unzoomed) location of the mark
     *
     * @return the current location, or null if mark is inactive
     */
    public Point getLocation ()
    {
        if (active) {
            if (logger.isDebugEnabled()) {
                logger.debug("getLocation " + center);
            }

            return center;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("getLocation on inactive mark");
            }

            return null;
        }
    }

    //--------------//
    // getRectangle //
    //--------------//

    /**
     * Report the current (zoomed) rectangle of the mark, where it would
     * appear on the display panel.
     *
     * @param zoom the zooming factor
     *
     * @return the corresponding rectangle, or null if mark is inactive
     */
    public Rectangle getRectangle (Zoom zoom)
    {
        if (active) {
            rect.x = zoom.scaled(center.x) - (rect.width / 2);
            rect.y = zoom.scaled(center.y) - (rect.height / 2);

            if (logger.isDebugEnabled()) {
                logger.debug("getRectangle " + rect);
            }

            return rect;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("getRectangle on inactive mark");
            }

            return null;
        }
    }

    //----------//
    // activate //
    //----------//

    /**
     * Make an existing mark active or not (and so rendered), using its
     * current location parameter.
     *
     * @param bool true for active, false for inactive
     */
    public void activate (boolean bool)
    {
        active = bool;
    }

    //--------//
    // render //
    //--------//

    /**
     * Render the (properly zoomed) Mark in the provided graphics
     *
     * @param g    the graphics context
     * @param zoom the zoom factor
     */
    public void render (Graphics g,
                        Zoom zoom)
    {
        // Prepare context
        g.setColor(color);

        // Stroke thickness
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(stroke);

        // Compute display value for rectangle, into 'rect' variable
        getRectangle(zoom);

        // Horizontal stick
        g.drawLine(rect.x, rect.y + (rect.height / 2), rect.x + rect.width,
                   rect.y + (rect.height / 2));

        // Vertical stick
        g.drawLine(rect.x + (rect.width / 2), rect.y,
                   rect.x + (rect.width / 2), rect.y + rect.height);
    }

    //----------//
    // toString //
    //----------//

    /**
     * Return a readable string about this mark
     *
     * @return a string based on the location pointed by the mark, if any.
     */
    public String toString ()
    {
        return "{Mark " + getLocation() + "}";
    }
}
