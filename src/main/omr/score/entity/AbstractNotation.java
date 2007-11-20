//----------------------------------------------------------------------------//
//                                                                            //
//                      A b s t r a c t N o t a t i o n                       //
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
 * Class <code>Notation</code> is the basis for all variants of notations:
 * tied, slur, ...
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class AbstractNotation
    extends MeasureElement
    implements Notation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        AbstractNotation.class);

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of Notation */
    public AbstractNotation (Measure     measure,
                             SystemPoint point,
                             Chord       chord,
                             Glyph       glyph)
    {
        this(measure, true, point, chord, glyph);
    }

    /** Creates a new instance of Notation */
    public AbstractNotation (Measure     measure,
                             boolean     start,
                             SystemPoint point,
                             Chord       chord,
                             Glyph       glyph)
    {
        super(measure, start, point, chord, glyph);

        // Register at its related chord
        if (chord != null) {
            chord.addNotation(this);
        } else {
            // We have a notation item without any related chord/note
            addError(glyph, "Notation " + this + " without related note");
        }
    }
}
