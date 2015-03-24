//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e s t C h o r d I n t e r                                  //
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
 * Class {@code RestChordInter} is a ChordInter composed of (one) rest.
 *
 * @author Hervé Bitteur
 */
public class RestChordInter
        extends ChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code RestChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public RestChordInter (double grade)
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
        return "RestChord";
    }
}
