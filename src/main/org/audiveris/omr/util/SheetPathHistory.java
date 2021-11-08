//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S h e e t P a t h H i s t o r y                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.util;

import org.audiveris.omr.constant.Constant;

import java.nio.file.Path;

/**
 * Class <code>SheetPathHistory</code> is a history of SheetPath entities.
 *
 * @author Hervé Bitteur
 */
public class SheetPathHistory
        extends AbstractHistory<SheetPath>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SheetPathHistory</code> object.
     *
     * @param name           a name for this history instance
     * @param constant       backing constant on disk
     * @param folderConstant backing constant for last folder, or null
     * @param maxSize        maximum items in history
     */
    public SheetPathHistory (String name,
                             Constant.String constant,
                             Constant.String folderConstant,
                             int maxSize)
    {
        super(name, constant, folderConstant, maxSize, (s1, s2) -> areEquivalent(s1, s2));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected Path getParent (SheetPath sheetPath)
    {
        return sheetPath.getBookPath().toAbsolutePath().getParent();
    }

    @Override
    protected String encode (SheetPath sheetPath)
    {
        return sheetPath.toString();
    }

    @Override
    protected SheetPath decode (String string)
    {
        return SheetPath.decode(string);
    }

    //---------------//
    // areEquivalent //
    //---------------//
    private static boolean areEquivalent (String s1,
                                          String s2)
    {
        final SheetPath sp1 = SheetPath.decode(s1);
        final SheetPath sp2 = SheetPath.decode(s2);

        return sp1.getBookPath().compareTo(sp2.getBookPath()) == 0;
    }
}
