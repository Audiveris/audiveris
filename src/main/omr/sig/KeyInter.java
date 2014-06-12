//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y I n t e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code KeyInter} represents a key signature on a staff.
 *
 * @author Hervé Bitteur
 */
public class KeyInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(KeyInter.class);

    /** Standard (in G clef) pitch positions for the members of the sharp keys */
    private static final int[] sharpPitches = new int[]{
        -4, // F - Fa
        -1, // C - Do
        -5, // G - Sol
        -2, // D - Ré
        +1, // A - La
        -3, // E - Mi
        0 // B - Si
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyInter object.
     *
     * @param box   the bounding box
     * @param shape SHARP or FLAT shape
     * @param grade the interpretation quality
     */
    public KeyInter (Rectangle box,
                     Shape shape,
                     double grade)
    {
        super(null, box, shape, grade);
    }
}
