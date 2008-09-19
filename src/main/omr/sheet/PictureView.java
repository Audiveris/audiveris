//----------------------------------------------------------------------------//
//                                                                            //
//                           P i c t u r e V i e w                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;

import java.awt.*;

/**
 * Class <code>PictureView</code> defines the view dedicated to the display of
 * the picture bitmap of a music sheet.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PictureView
    extends ScrollView
{
    //~ Instance fields --------------------------------------------------------

    /** Link with sheet */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // PictureView //
    //-------------//
    /**
     * Create a new <code>PictureView</code> instance, dedicated to a sheet.
     *
     * @param sheet the related sheet
     */
    public PictureView (Sheet sheet)
    {
        this.sheet = sheet;

        view = new MyView();
        view.setName("Picture-View");

        // Inject dependency of pixel location
        view.setLocationService(sheet.getEventService());

        // Insert view
        setView(view);
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends RubberZoomedPanel
    {
        //~ Methods ------------------------------------------------------------

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics g)
        {
            // Render the picture image
            sheet.getPicture()
                 .render(g, getZoom().getRatio());
        }
    }
}
