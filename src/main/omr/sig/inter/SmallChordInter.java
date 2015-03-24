//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l C h o r d I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

/**
 * Class {@code SmallChordInter} is a ChordInter composed of small heads.
 *
 * @author Hervé Bitteur
 */
public class SmallChordInter
        extends ChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SmallChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public SmallChordInter (double grade)
    {
        super(grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "SmallChord";
    }
}
