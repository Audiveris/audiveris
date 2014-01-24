//----------------------------------------------------------------------------//
//                                                                            //
//                        S c r o l l I m a g e V i e w                       //
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

import omr.ui.view.ScrollView;

/**
 * Class {@code ScrollImageView}
 *
 * @author Hervé Bitteur
 */
public class ScrollImageView
        extends ScrollView
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScrollImageView object.
     *
     * @param sheet related sheet
     * @param view  the image view
     */
    public ScrollImageView (Sheet sheet,
                            ImageView view)
    {
        // Inject dependency of pixel location
        view.setLocationService(sheet.getLocationService());

        // Insert view
        setView(view);
    }
}
