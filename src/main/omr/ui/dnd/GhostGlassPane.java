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
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

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
    //~ Static fields/initializers ---------------------------------------------

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

    /** The current location within this glasspane */
    private Point location = new Point(0, 0);

    /** Display ratio */
    private double ratio = 1f;

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

    //---------------//
    // setOverTarget //
    //---------------//
    /**
     * Tell the glasspane whether we are currently over a droppable target
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
     * Assign the current point, where the dragged image is to be displayed
     * @param location the current location (glasspane-based)
     */
    public void setPoint (Point location)
    {
        this.location = location;
    }

    //----------//
    // setRatio //
    //----------//
    public void setRatio (double ratio)
    {
        this.ratio = ratio;
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
        g2.setComposite(overTarget ? targetComposite : nonTargetComposite);

        BufferedImageOp op = new AffineTransformOp(
            AffineTransform.getScaleInstance(ratio, ratio),
            AffineTransformOp.TYPE_BILINEAR);

        g2.drawImage(
            draggedImage,
            op,
            (int) Math.rint(
                location.x - ((draggedImage.getWidth(this) * ratio) / 2)),
            (int) Math.rint(
                location.y - ((draggedImage.getHeight(this) * ratio) / 2)));
    }
}
