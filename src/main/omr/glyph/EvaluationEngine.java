//----------------------------------------------------------------------------//
//                                                                            //
//                      E v a l u a t i o n E n g i n e                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.math.NeuralNetwork;

import java.util.Collection;

/**
 * Interface {@code EvaluationEngine} describes the life-cycle of an
 * evaluation engine.
 *
 * @author Hervé Bitteur
 */
public interface EvaluationEngine
        extends ShapeEvaluator
{
    //~ Enumerations -----------------------------------------------------------

    /** The various modes for starting the training of an evaluator. */
    public static enum StartingMode
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Start with the current values. */
        INCREMENTAL,
        /** Start from
         * scratch, with new initial values. */
        SCRATCH;

    }

    //~ Methods ----------------------------------------------------------------
    /**
     * Dump the internals of the engine.
     */
    void dump ();

    /**
     * Store the engine in XML format.
     */
    void marshal ();

    /**
     * Stop the on-going training.
     */
    void stop ();

    /**
     * Train the evaluator on the provided base of sample glyphs.
     *
     * @param base    the collection of glyphs to train the evaluator
     * @param monitor a monitoring interface
     * @param mode    specify the starting mode of the training session
     */
    void train (Collection<Glyph> base,
                Monitor monitor,
                StartingMode mode);

    //~ Inner Interfaces -------------------------------------------------------
    /**
     * General monitoring interface to pass information about the
     * training of an evaluator when processing a sample glyph.
     */
    public static interface Monitor
            extends NeuralNetwork.Monitor
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Entry called when a glyph is being processed.
         *
         * @param glyph the sample glyph being processed
         */
        void glyphProcessed (Glyph glyph);
    }
}
