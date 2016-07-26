//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G l y p h C l a s s i f i e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

/**
 * Class {@code GlyphClassifier} hides the actual classifier in use.
 *
 * @author Hervé Bitteur
 */
public class GlyphClassifier
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the classifier instance in use.
     *
     * @return the current classifier
     */
    public static Classifier getInstance ()
    {
        return NeuralClassifier.getInstance();
    }
}
