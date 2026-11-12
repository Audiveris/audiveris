//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           O m r G u i                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.ui;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * Class <code>OmrGui</code> is the basis for any {@link SingleFrameApplication} defined in
 * Audiveris.
 * <p>
 * Its related .properties file defines the needed Application resources.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrGui
        extends SingleFrameApplication
{
    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // getApplication //
    //----------------//
    /**
     * Report the single instance of this application.
     *
     * @return the SingleFrameApplication instance
     */
    public static SingleFrameApplication getApplication ()
    {
        return (SingleFrameApplication) Application.getInstance();
    }
}
