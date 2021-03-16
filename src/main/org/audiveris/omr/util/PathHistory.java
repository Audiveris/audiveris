//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a t h H i s t o r y                                     //
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
import java.nio.file.Paths;

/**
 * Class {@code PathHistory} handles a history of paths, as used for latest input or
 * book files.
 *
 * @author Hervé Bitteur
 */
public class PathHistory
        extends AbstractHistory<Path>
{

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PathHistory} object.
     *
     * @param name           a name for this history instance
     * @param constant       backing constant on disk
     * @param folderConstant backing constant for last folder, or null
     * @param maxSize        maximum items in history
     */
    public PathHistory (String name,
                        Constant.String constant,
                        Constant.String folderConstant,
                        int maxSize)
    {
        super(name, constant, folderConstant, maxSize, (s1, s2) -> areEquivalent(s1, s2));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected Path getParent (Path path)
    {
        return path.toAbsolutePath().getParent();
    }

    @Override
    protected String encode (Path path)
    {
        return path.toAbsolutePath().toString();
    }

    @Override
    protected Path decode (String str)
    {
        return Paths.get(str);
    }

    //---------------//
    // areEquivalent //
    //---------------//
    private static boolean areEquivalent (String s1,
                                          String s2)
    {
        final Path p1 = Paths.get(s1);
        final Path p2 = Paths.get(s2);

        return p1.compareTo(p2) == 0;
    }
}
