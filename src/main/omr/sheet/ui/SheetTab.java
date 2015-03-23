//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S h e e t T a b                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

/**
 * Class {@code SheetTab} gathers all available tab names in sheet assemblies.
 *
 * @author Hervé Bitteur
 */
public enum SheetTab
{

    PICTURE_TAB("Picture"),
    BINARY_TAB("Binary"),
    DELTA_TAB("Delta"),
    DIFF_TAB("Diff"),
    DATA_TAB("Data"),
    BEAM_SPOT_TAB("BeamSpots"),
    GRAY_SPOT_TAB("GraySpots"),
    LEDGER_TAB("Ledgers"),
    SKELETON_TAB("Skeleton"),
    TEMPLATE_TAB("Templates"),
    NO_STAFF_TAB("NoStaff");

    public final String label;

    SheetTab (String label)
    {
        this.label = label;
    }
}
