//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G l y p h C l a s s i f i e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

/**
 * Class {@code GlyphClassifier} hides the actual classifier in use.
 *
 * @author Hervé Bitteur
 */
public class GlyphClassifier
{
    //~ Methods ------------------------------------------------------------------------------------

    public static ShapeEvaluator getInstance ()
    {
        return GlyphNetwork.getInstance();
        ///return WekaClassifier.getInstance();
    }
}
