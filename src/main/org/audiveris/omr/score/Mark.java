//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            M a r k                                             //
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
package org.audiveris.omr.score;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.ui.symbol.SymbolIcon;
import org.audiveris.omr.util.Navigable;

import java.awt.Point;

/**
 * Class {@code Mark} encapsulates information to be made visible to the
 * end user on the score display in a very general way.
 *
 * @author Hervé Bitteur
 */
public class Mark
{
    //~ Enumerations -------------------------------------------------------------------------------

    /** Position relative to an entity */
    public static enum Position
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Mark should be horizontally located <b>before</b> the entity */
        BEFORE,
        /** Mark
         * should be horizontally located <b>after</b> the entity */
        AFTER;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing system */
    @Navigable(false)
    private final SystemInfo system;

    /** Precise location within system */
    private final Point location;

    /** Position of the mark symbol with respect to the mark location */
    private final Position position;

    /** The symbol of the mark in the MusicFont */
    private final SymbolIcon symbol;

    /** Additional data, perhaps depending on shape for example */
    private final Object data;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of Mark
     *
     * @param system   containing system
     * @param location precise locatrion wrt the containing system
     * @param position relative symbol position wrt location
     * @param symbol   MusicFont descriptor to be used
     * @param data     related data or null
     */
    public Mark (SystemInfo system,
                 Point location,
                 Position position,
                 SymbolIcon symbol,
                 Object data)
    {
        this.system = system;
        this.location = location;
        this.position = position;
        this.symbol = symbol;
        this.data = data;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    /**
     * Report the related data information, or null
     *
     * @return the data information
     */
    public Object getData ()
    {
        return data;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the location (wrt containing system) of this mark
     *
     * @return the mark location
     */
    public Point getLocation ()
    {
        return location;
    }

    //-------------//
    // getPosition //
    //-------------//
    /**
     * Report relative position of symbol wrt mark location
     *
     * @return the relative position of symbol
     */
    public Position getPosition ()
    {
        return position;
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Report the descriptor of the symbol to be displayed
     *
     * @return the MusicFont symbol descriptor
     */
    public SymbolIcon getSymbol ()
    {
        return symbol;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system of this mark (TODO: Useful?)
     *
     * @return the containing system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }
}
