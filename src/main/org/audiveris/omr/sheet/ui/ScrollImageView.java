//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c r o l l I m a g e V i e w                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.view.ScrollView;

/**
 * Class {@code ScrollImageView}
 *
 * @author Hervé Bitteur
 */
public class ScrollImageView
        extends ScrollView
{
    //~ Constructors -------------------------------------------------------------------------------

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
