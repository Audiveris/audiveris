//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P r o f i l e s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

/**
 * Class <code>Profiles</code> gathers all defined profile levels.
 *
 * @author Hervé Bitteur
 */
public abstract class Profiles
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** Strict case: no gap allowed. */
    public static final int STRICT = 0;

    /** Standard case: no particular profile. */
    public static final int STANDARD = 1;

    /** Case of an entity manually defined by the user, we can be more relax. */
    public static final int MANUAL = 2;

    /** Sheet of rather poor quality. */
    public static final int POOR = 2;

    /** Linking a rather good head. */
    public static final int RATHER_GOOD_HEAD = 3;

    /** Linking a beam center, from a connected stem seed. */
    public static final int BEAM_SEED = 3;

    /** Linking a beam side, for a good beam, should not fail. */
    public static final int BEAM_SIDE = 4;

    /** Maximum defined profile value. */
    public static final int MAX_VALUE = 4;

    /** Default input quality. */
    public static final Param<InputQuality> defaultQualityParam = new ConstantBasedParam<>(
            constants.defaultQuality,
            Param.GLOBAL_SCOPE);

    //~ Constructors -------------------------------------------------------------------------------

    private Profiles ()
    {
        // Not meant to be instantiated
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Enum<InputQuality> defaultQuality = new Constant.Enum<>(
                InputQuality.class,
                InputQuality.Standard,
                "Default quality for input image");
    }

    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * Enum <code>InputQuality</code> describes input quality.
     */
    public static enum InputQuality
    {
        /** The highest quality, no gaps allowed. */
        Synthetic,
        /** The standard quality, small gaps allowed. */
        Standard,
        /** The lowest quality, use a hierarchy of gap profiles. */
        Poor;
    }
}
