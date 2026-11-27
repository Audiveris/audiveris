//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         C l e f K i n d                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.util.Tipped;
import org.audiveris.omr.util.Translator;

/**
 * Class <code>ClefKind</code>, based on shape and pitch.
 *
 * @author Hervé Bitteur
 */
public enum ClefKind implements Tipped
{
    TREBLE(Shape.G_CLEF, 2),
    BASS(Shape.F_CLEF, -2),
    BARITONE(Shape.F_CLEF, 0),
    TENOR(Shape.C_CLEF, -2),
    ALTO(Shape.C_CLEF, 0),
    MEZZO_SOPRANO(Shape.C_CLEF, 2),
    SOPRANO(Shape.C_CLEF, 4),
    PERCUSSION(Shape.PERCUSSION_CLEF, 0);

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Translator ct = new Translator(ClefKind.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Symbol shape class. (regardless of ottava mark if any) */
    public final Shape shape;

    /** Pitch of reference line. */
    public final int pitch;

    //~ Constructors -------------------------------------------------------------------------------

    ClefKind (Shape shape,
              int pitch)
    {
        this.shape = shape;
        this.pitch = pitch;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // getTip //
    //--------//
    @Override
    public String getTip ()
    {
        return ct.trans(name() + ".toolTipText");
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return ct.trans(name() + ".text");
    }

}
