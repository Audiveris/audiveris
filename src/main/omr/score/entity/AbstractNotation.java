//----------------------------------------------------------------------------//
//                                                                            //
//                      A b s t r a c t N o t a t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyph;

import omr.log.Logger;

import omr.score.common.SystemPoint;

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

    /**
     * Creates a new instance of a simple Notation (assumed to be both the
     * start and the stop)
     * @param measure the containing measure
     * @param point
     * @param chord the related chord
     * @param glyph the underlying glyph
     */
    public AbstractNotation (Measure     measure,
                             SystemPoint point,
                             Chord       chord,
                             Glyph       glyph)
    {
        super(measure, true, point, chord, glyph);

        // Register at its related chord
        if (chord != null) {
            chord.addNotation(this);
        } else {
            // We have a notation item without any related chord/note
            addError(glyph, "Notation " + this + " without related note");
        }
    }
}
