//----------------------------------------------------------------------------//
//                                                                            //
//                        G h o s t G l a s s P a n e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import omr.ui.symbol.SymbolImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Class {@code GhostGlassPane} is a special glasspane, meant for
 * displaying a shape being dragged and finally dropped.
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostGlassPane
        extends JPanel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GhostGlassPane.class);

    /** Composite to be used over a droppable target */
    private static AlphaComposite targetComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.5f);

    /** Composite to be used over a non-droppable target */
    private static AlphaComposite nonTargetComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.2f);

    //~ Instance fields --------------------------------------------------------
    /** The image to be dragged */
    private BufferedImage draggedImage = null;

    /** The previous display bounds */
    private Rectangle prevRectangle = null;

    /** The current location within this glasspane */
    private Point localPoint = null;

    /** Are we over a droppable target? */
    private boolean overTarget = false;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // GhostGlassPane //
    //----------------//
    /**
     * Create a new GhostGlassPane object
     */
    public GhostGlassPane ()
    {
        setOpaque(false);
        setName("GhostGlassPane");
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // paintComponent //
    //----------------//
    @Override
    public void paintComponent (Graphics g)
    {
        if ((draggedImage == null) || (localPoint == null)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        // Use composition with display underneath
        if (!overTarget) {
            g2.setComposite(nonTargetComposite);
        } else {
            g2.setComposite(targetComposite);
        }

        Rectangle rect = getImageBounds(localPoint);
        g2.drawImage(draggedImage, null, rect.x, rect.y);
    }

    //---------//
    // setImage //
    //---------//
    /**
     * Assign the image to be dragged
     *
     * @param draggedImage the image to drag
     */
    public void setImage (BufferedImage draggedImage)
    {
        this.draggedImage = draggedImage;
    }

    //---------------//
    // setOverTarget //
    //---------------//
    /**
     * Tell the glasspane whether we are currently over a droppable target
     *
     * @param overTarget true if over a target
     */
    public void setOverTarget (boolean overTarget)
    {
        this.overTarget = overTarget;
    }

    //----------//
    // setPoint //
    //----------//
    /**
     * Assign the current point, where the dragged image is to be displayed,
     * and repaint as few as possible of the glass pane.
     *
     * @param localPoint the current location (glasspane-based)
     */
    public void setPoint (Point localPoint)
    {
        // Anything to repaint since last time the point was set?
        if (draggedImage != null) {
            Rectangle rect = getImageBounds(localPoint);
            Rectangle dirty = new Rectangle(rect);

            if (prevRectangle != null) {
                dirty.add(prevRectangle);
            }

            dirty.grow(1, 1); // To cope with rounding errors

            // Set new values now, to avoid race condition with repaint
            this.localPoint = localPoint;
            prevRectangle = rect;

            repaint(dirty.x, dirty.y, dirty.width, dirty.height);
        } else {
            this.localPoint = localPoint;
            prevRectangle = null;
        }
    }

    //----------//
    // setPoint //
    //----------//
    /**
     * Assign the current point, where the dragged image is to be displayed
     *
     * @param screenPoint the current location (screen-based)
     */
    public void setPoint (ScreenPoint screenPoint)
    {
        setPoint(screenPoint.getLocalPoint(this));
    }

    //----------------//
    // getImageBounds //
    //----------------//
    private Rectangle getImageBounds (Point center)
    {
        Rectangle rect = new Rectangle(center);
        rect.grow(draggedImage.getWidth() / 2, draggedImage.getHeight() / 2);

        if (draggedImage instanceof SymbolImage) {
            SymbolImage symbolImage = (SymbolImage) draggedImage;
            Point refPoint = symbolImage.getRefPoint();

            if (refPoint != null) {
                rect.translate(-refPoint.x, -refPoint.y);
            }
        }

        return rect;
    }
}
