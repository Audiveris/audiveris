//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T e x t T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;

/**
 * Class {@code TextTask} acts on textual items (words and sentences).
 *
 * @author Hervé Bitteur
 */
public abstract class TextTask
        extends UITask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code TextTask} object.
     *
     * @param sig the underlying sig
     */
    public TextTask (SIGraph sig)
    {
        super(sig);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the underlying inter (word or sentence)
     *
     * @return the underlying inter
     */
    public abstract Inter getInter ();
}
