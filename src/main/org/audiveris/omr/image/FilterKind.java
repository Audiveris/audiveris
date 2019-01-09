//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       F i l t e r K i n d                                      //
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
package org.audiveris.omr.image;

/**
 * Class {@code FilterKind} handles the various kinds of {@link PixelFilter}
 * implementations.
 *
 * @author Hervé Bitteur
 */
public enum FilterKind
{
    GLOBAL("Basic filter using a global threshold", GlobalFilter.class),
    ADAPTIVE("Adaptive filter using a local threshold", AdaptiveFilter.class);

    /** Description. */
    public final String description;

    /** Implementing class. */
    public final Class<?> classe;

    FilterKind (String description,
                Class<?> classe)
    {
        this.description = description;
        this.classe = classe;
    }
}
