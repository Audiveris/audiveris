//----------------------------------------------------------------------------//
//                                                                            //
//                              I ma g e V i e w                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.sheet.Sheet;

import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Class {@code ImageView}
 *
 * @author Hervé Bitteur
 */
public class ImageView
    extends ScrollView
{
    //~ Instance fields --------------------------------------------------------

    private final BufferedImage image;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ImageView object.
     *
     * @param image the underlying image
     */
    public ImageView (Sheet         sheet,
                      BufferedImage image)
    {
        this.image = image;

        view = new MyView();
        view.setName("Image-View");
        view.setPreferredSize(
            new Dimension(image.getWidth(), image.getHeight()));

        // Inject dependency of pixel location
        view.setLocationService(sheet.getLocationService());

        // Insert view
        setView(view);
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends RubberPanel
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void render (Graphics2D g)
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawRenderedImage(image, null);
        }
    }
}
