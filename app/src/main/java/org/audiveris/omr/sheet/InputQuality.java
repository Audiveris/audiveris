//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n p u t Q u a l i t y                                    //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.util.Tipped;
import org.audiveris.omr.util.Translator;

/**
 * Enum <code>InputQuality</code> describes input quality.
 *
 * @author Hervé Bitteur
 */
public enum InputQuality implements Tipped
{
    Synthetic,
    Standard,
    Poor;

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Translator ct = new Translator(InputQuality.class);

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // getTip //
    //--------//
    @Override
    public String getTip ()
    {
        return ct.trans(name() + ".toolTipText");
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return ct.trans(name() + ".text");
    }
}
