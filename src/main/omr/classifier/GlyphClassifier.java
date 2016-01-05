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

import omr.constant.ConstantSet;

/**
 * Class {@code GlyphClassifier} hides the actual classifier in use.
 *
 * @author Hervé Bitteur
 */
public class GlyphClassifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the classifier instance in use.
     *
     * @return the current classifier
     */
    public static Classifier getInstance ()
    {
        switch (constants.defaultKind.getValue()) {
        case NEURAL:
            return NeuralClassifier.getInstance();

        case BAYESIAN:
            return WekaClassifier.getInstance();
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final ClassifierKind.Constant defaultKind = new ClassifierKind.Constant(
                ClassifierKind.NEURAL,
                "Default kind of glyph classifier (NEURAL or BAYESIAN)");
    }
}
