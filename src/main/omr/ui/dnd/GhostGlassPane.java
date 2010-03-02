//----------------------------------------------------------------------------//
//                                                                            //
//                        G h o s t G l a s s P a n e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Class {@code GhostGlassPane} is a special glasspane, meant for displaying
 * an image being dragged and finally dropped.
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostGlassPane
    extends JPanel
{
    //~ Instance fields --------------------------------------------------------

    /**
     * Determine how the dragged image is combined with the components
     * underneath
     */
    private AlphaComposite composite;

    /** The image to be dragged */
    private BufferedImage draggedImage = null;

    /** The current location within this glasspane */
    private Point location = new Point(0, 0);

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
        composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        setName("GhostGlassPane");
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setImage //
    //----------//
    /**
     * Assign the image to be dragged
     * @param draggedImage the image to drag
     */
    public void setImage (BufferedImage draggedImage)
    {
        this.draggedImage = draggedImage;
    }

    //----------//
    // setPoint //
    //----------//
    /**
     * Assign the current point, where the dragged image is to be displayed
     * @param location the current location (glasspane-based)
     */
    public void setPoint (Point location)
    {
        this.location = location;
    }

    //----------------//
    // paintComponent //
    //----------------//
    @Override
    public void paintComponent (Graphics g)
    {
        if (draggedImage == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(composite);
        g2.drawImage(
            draggedImage,
            location.x - (draggedImage.getWidth(this) / 2),
            location.y - (draggedImage.getHeight(this) / 2),
            null);
    }
}
