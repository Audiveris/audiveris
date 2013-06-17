//----------------------------------------------------------------------------//
//                                                                            //
//                                E n d i n g                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

/**
 * Class {@code Ending} is a physical {@link Dash} that is the
 * horizontal part of an alternate ending.
 *
 * @author Hervé Bitteur
 */
public class Ending
        extends Dash
{
    //~ Constructors -----------------------------------------------------------

    //--------//
    // Ending //
    //--------//
    /**
     * Create an Ending entity, with its underlying horizontal stick.
     *
     * @param stick the underlying stick
     * @param staff the related staff
     */
    public Ending (Glyph stick,
                   StaffInfo staff)
    {
        super(stick, staff);
    }
}
