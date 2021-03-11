//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P r o f i l e s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

/**
 * Class {@code Profiles} gathers all defined profile levels.
 *
 * @author Hervé Bitteur
 */
public abstract class Profiles
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Standard case: no particular profile. */
    public static final int STANDARD = 0;

    /** Case of an entity manually defined by the user, we can be more relax. */
    public static final int MANUAL = 1;

    /** Sheet of rather poor quality. */
    public static final int POOR = 1;

    /** Linking a rather good head. */
    public static final int RATHER_GOOD_HEAD = 2;

    /** Linking a beam center, from a connected stem seed. */
    public static final int BEAM_SEED = 2;

    /** Linking a beam side, for a good beam, should not fail. */
    public static final int BEAM_SIDE = 3;

    /** Maximum defined profile value. */
    public static final int MAX_VALUE = 3;

    //~ Constructors -------------------------------------------------------------------------------
    private Profiles ()
    {
        // Not meant to be instantiated
    }
}
