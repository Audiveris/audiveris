//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C l a s s i f i e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.classifier;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeChecker;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Class {@code AbstractClassifier} is an abstract classifier implementation that
 * can handle additional checks.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractClassifier
        implements Classifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractClassifier.class);

    /** A special evaluation array, used to report NOISE. */
    protected static final Evaluation[] noiseEvaluations = {
        new Evaluation(
        Shape.NOISE,
        Evaluation.ALGORITHM)
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  SystemInfo system,
                                  int count,
                                  double minGrade,
                                  EnumSet<Classifier.Condition> conditions)
    {
        final int interline = system.getSheet().getInterline();

        return evaluate(glyph, system, count, minGrade, conditions, interline);
    }

    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  int interline,
                                  int count,
                                  double minGrade,
                                  EnumSet<Condition> conditions)
    {
        return evaluate(glyph, null, count, minGrade, conditions, interline);
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (Glyph glyph,
                                int interline)
    {
        return isBigEnough(glyph.getNormalizedWeight(interline));
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (double weight)
    {
        return weight >= constants.minWeight.getValue();
    }

    //----------------------//
    // getSortedEvaluations //
    //----------------------//
    /**
     * Run the classifier with the specified glyph, and return a sequence of all
     * interpretations (ordered from best to worst) with no additional check.
     *
     * @param glyph     the glyph to be examined
     * @param interline the global sheet interline
     * @return the ordered best evaluations
     */
    protected Evaluation[] getSortedEvaluations (Glyph glyph,
                                                 int interline)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph, interline)) {
            return noiseEvaluations;
        } else {
            Evaluation[] evals = getNaturalEvaluations(glyph, interline);
            // Order the evals from best to worst
            Arrays.sort(evals);

            return evals;
        }
    }

    //----------//
    // evaluate //
    //----------//
    private Evaluation[] evaluate (Glyph glyph,
                                   SystemInfo system,
                                   int count,
                                   double minGrade,
                                   EnumSet<Classifier.Condition> conditions,
                                   int interline)
    {
        List<Evaluation> bests = new ArrayList<Evaluation>();
        Evaluation[] evals = getSortedEvaluations(glyph, interline);

        EvalsLoop:
        for (Evaluation eval : evals) {
            // Bounding test?
            if ((bests.size() >= count) || (eval.grade < minGrade)) {
                break;
            }

            // Successful checks?
            if (conditions.contains(Condition.CHECKED)) {
                double[] ins = ShapeDescription.features(glyph, interline);
                // This may change the eval shape in only one case:
                // HW_REST_set may be changed for HALF_REST or WHOLE_REST based on pitch
                glyphChecker.annotate(system, eval, glyph, ins);

                if (eval.failure != null) {
                    continue;
                }
            }

            // Everything is OK, add the shape if not already in the list
            // (this can happen when checks have modified the eval original shape)
            for (Evaluation e : bests) {
                if (e.shape == eval.shape) {
                    continue EvalsLoop;
                }
            }

            bests.add(eval);
        }

        return bests.toArray(new Evaluation[bests.size()]);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.08,
                "Minimum normalized weight to be considered not a noise");
    }
}
