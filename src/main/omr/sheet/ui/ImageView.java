//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I m a g e V i e w                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.ui.view.RubberPanel;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.RenderedImage;

/**
 * Class {@code ImageView} displays a view on an image.
 * Typically, subclasses would have to only override the {@link #renderItems} method.
 *
 * @author Hervé Bitteur
 */
public class ImageView
        extends RubberPanel
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final RenderedImage image;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ImageView object.
     *
     * @param image the image to display
     */
    public ImageView (RenderedImage image)
    {
        this.image = image;

        setName("Image-View");

        ////setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        setModelSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void render (Graphics2D g)
    {
        g.drawRenderedImage(image, null);
    }
}
