//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S h e e t T a b                                        //
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
package org.audiveris.omr.sheet.ui;

/**
 * Class {@code SheetTab} gathers all available tab names in sheet assemblies.
 *
 * @author Hervé Bitteur
 */
public enum SheetTab
{
    INITIAL_TAB("Initial"),
    BINARY_TAB("Binary"),
    DELTA_TAB("Delta"),
    DIFF_TAB("Diff"),
    DATA_TAB("Data"),
    FILAMENT_TAB("Filaments"),
    BEAM_SPOT_TAB("BeamSpots"),
    GRAY_SPOT_TAB("GraySpots"),
    ANNOTATION_TAB("Annotations"),
    LEDGER_TAB("Ledgers"),
    SKELETON_TAB("Skeleton"),
    TEMPLATE_TAB("Templates"),
    NO_STAFF_TAB("NoStaff"),
    STAFF_LINE_TAB("StaffLineGlyphs");

    public final String label;

    SheetTab (String label)
    {
        this.label = label;
    }
}
