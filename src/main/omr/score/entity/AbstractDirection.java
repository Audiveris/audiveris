//----------------------------------------------------------------------------//
//                                                                            //
//                     A b s t r a c t D i r e c t i o n                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;

import omr.score.common.SystemPoint;

import omr.util.Logger;

/**
 * Class <code>Direction</code> is the basis for all variants of direction
 * indications: pedal, words, dynamics, wedge, dashes, etc...
 *
 * <p>For some directions (such as wedge, dashes, pedal), we may have two
 * "events": the starting event and the stopping event. Both will trigger the
 * creation of a Direction instance, the difference being made by the "start"
 * boolean.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class AbstractDirection
    extends MeasureElement
    implements Direction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        AbstractDirection.class);

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of Direction */
    public AbstractDirection (Measure     measure,
                              SystemPoint point,
                              Chord       chord,
                              Glyph       glyph)
    {
        this(measure, true, point, chord, glyph);
    }

    /** Creates a new instance of Direction */
    public AbstractDirection (Measure     measure,
                              boolean     start,
                              SystemPoint point,
                              Chord       chord,
                              Glyph       glyph)
    {
        super(measure, start, point, chord, glyph);

        // Register at its related chord
        if (chord != null) {
            chord.addDirection(this);
        } else {
            // We have a direction item without any related chord/note
            // This is legal, however where do we store this item?
            addError(glyph, "Direction " + this + " without related note");
        }
    }
}
