//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I m a g e V i e w                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.ui.view.RubberPanel;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Class {@code ImageView}
 *
 * @author Hervé Bitteur
 */
public class ImageView
        extends RubberPanel
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final BufferedImage image;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ImageView object.
     *
     * @param image the image to display
     */
    public ImageView (BufferedImage image)
    {
        this.image = image;

        setName("Image-View");
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void render (Graphics2D g)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.drawRenderedImage(image, null);

        renderItems(g);
    }

    protected void renderItems (Graphics2D g)
    {
        // Void by default
    }
}
