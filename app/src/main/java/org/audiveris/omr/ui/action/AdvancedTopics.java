//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A d v a n c e d T o p i c s                                  //
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
package org.audiveris.omr.ui.action;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

/**
 * Class <code>AdvancedTopics</code> is kept separate from the Customization class for
 * upward compatibility.
 *
 * @author Hervé Bitteur
 */
public abstract class AdvancedTopics
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final Constants constants = new Constants();

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Report whether we should swap out any processed sheet.
     *
     * @return true if so
     */
    public static boolean swapProcessedSheets ()
    {
        return constants.swapProcessedSheets.isSet();
    }

    /**
     * Report whether we should process systems of a sheet in parallel.
     *
     * @return true if so
     */
    public static boolean processSystemsInParallel ()
    {
        return constants.processSystemsInParallel.isSet();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    public static class Constants
            extends ConstantSet
    {
        final Constant.Boolean swapProcessedSheets = new Constant.Boolean(
                true,
                "Automatic swap out of sheets once they are processed");

        final Constant.Boolean processSystemsInParallel = new Constant.Boolean(
                false,
                "Systems processed in parallel for relevant steps");

        final Constant.Boolean useSamples = new Constant.Boolean(
                false,
                "Handling of samples repositories and classifier");

        final Constant.Boolean useAnnotations = new Constant.Boolean(
                false,
                "Production of image annotation with symbols");

        final Constant.Boolean usePlots = new Constant.Boolean(
                false,
                "Display of scale / stem / staves plots");

        final Constant.Boolean useSpecificViews = new Constant.Boolean(
                false,
                "Display of specific sheet views");

        final Constant.Boolean useSpecificItems = new Constant.Boolean(
                false,
                "Specific items shown in sheet view");

        final Constant.Boolean useDebug = new Constant.Boolean( //
                false,
                "Support for debug features");
    }
}
