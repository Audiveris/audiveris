//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h R e c o g n i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Evaluation;
import omr.glyph.Shape;

import omr.score.entity.TimeRational;

/**
 * Interface {@code GlyphRecognition} defines a facet that deals with
 * the shape recognition of a glyph.
 *
 * @author Hervé Bitteur
 */
interface GlyphRecognition
        extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Remove the provided shape from the collection of forbidden
     * shaped, if any.
     *
     * @param shape the shape to allow
     */
    void allowShape (Shape shape);

    /**
     * Forbid a specific shape.
     *
     * @param shape the shape to forbid
     */
    void forbidShape (Shape shape);

    /**
     * Report the evaluation, if any.
     *
     * @return the evaluation structure (shape + grade + failure if any)
     */
    Evaluation getEvaluation ();

    /**
     * Report the grade of the glyph shape.
     *
     * @return the grade related to glyph shape
     */
    double getGrade ();

    /**
     * Report the registered glyph shape.
     *
     * @return the glyph shape, which may be null
     */
    Shape getShape ();

    /**
     * Report the related timesig rational if any.
     *
     * @return the time rational
     */
    TimeRational getTimeRational ();

    /**
     * Convenient method which tests if the glyph is a Bar line.
     *
     * @return true if glyph shape is a bar
     */
    boolean isBar ();

    /**
     * Convenient method which tests if the glyph is a Clef.
     *
     * @return true if glyph shape is a Clef
     */
    boolean isClef ();

    /**
     * A glyph is considered as known if it has a registered shape other
     * than NOISE.
     * (Notice that CLUTTER as well as NO_LEGAL_TIME and GLYPH_PART are
     * considered as being known).
     *
     * @return true if known
     */
    boolean isKnown ();

    /**
     * Report whether the shape of this glyph has been manually assigned.
     * (and thus can only be modified by explicit user action).
     *
     * @return true if shape manually assigned
     */
    boolean isManualShape ();

    /**
     * Check whether a shape is forbidden for this glyph.
     *
     * @param shape the shape to check
     * @return true if the provided shape is one of the forbidden shapes for
     *         this glyph
     */
    boolean isShapeForbidden (Shape shape);

    /**
     * Check whether the glyph shape is a Stem.
     *
     * @return true if glyph shape is a Stem
     */
    boolean isStem ();

    /**
     * Check whether the glyph shape is a text.
     *
     * @return true if text or character
     */
    boolean isText ();

    /**
     * A glyph is considered as well known if it has a registered well
     * known shape.
     *
     * @return true if so
     */
    boolean isWellKnown ();

    /**
     * Nullify the current evaluation, without impact on forbidden
     * shapes, to allow a new evaluation computation.
     */
    void resetEvaluation ();

    /**
     * Assign an evaluation.
     *
     * @param evaluation the evaluation structure, perhaps null
     */
    void setEvaluation (Evaluation evaluation);

    /**
     * Setter for the glyph shape (Algorithm assumed).
     *
     * @param shape the assigned shape, which may be null
     */
    void setShape (Shape shape);

    /**
     * Setter for the glyph shape, with related grade.
     *
     * @param shape the assigned shape
     * @param grade the related grade
     */
    void setShape (Shape shape,
                   double grade);

    /**
     * Set the glyph timesig rational value.
     *
     * @param timeRational the time rational to set
     */
    void setTimeRational (TimeRational timeRational);
}
