//----------------------------------------------------------------------------//
//                                                                            //
//                     A b s t r a c t D i r e c t i o n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code Direction} is the basis for all variants of direction
 * indications: pedal, words, dynamics, wedge, dashes, etc...
 *
 * <p>For some directions (such as wedge, dashes, pedal), we may have two
 * "events": the starting event and the stopping event. Both will trigger the
 * creation of a Direction instance, the difference being made by the "start"
 * boolean.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractDirection
        extends MeasureElement
        implements Direction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AbstractDirection.class);

    //~ Constructors -----------------------------------------------------------
    /** Creates a new instance of Direction
     *
     * @param measure        the containing measure
     * @param referencePoint the reference point for this direction
     * @param chord          the related chord if any
     * @param glyph          the underlying glyph
     */
    public AbstractDirection (Measure measure,
                              Point referencePoint,
                              Chord chord,
                              Glyph glyph)
    {
        this(measure, true, referencePoint, chord, glyph);
    }

    /** Creates a new instance of Direction
     *
     * @param measure        the containing measure
     * @param isStart        true or false, to flag a start or a stop
     * @param referencePoint the reference point for this direction
     * @param chord          the related chord if any
     * @param glyph          the underlying glyph
     */
    public AbstractDirection (Measure measure,
                              boolean isStart,
                              Point referencePoint,
                              Chord chord,
                              Glyph glyph)
    {
        super(measure, isStart, referencePoint, chord, glyph);

        // Register at its related chord
        if (chord != null) {
            chord.addDirection(this);
        } else {
            // We have a direction item without any related chord/note
            // This is legal, however where do we store this item? TODO
            addError(glyph, "Direction with no related note");
        }
    }
}
