//----------------------------------------------------------------------------//
//                                                                            //
//                                  N o t e                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.visitor.Visitor;

/**
 * Class <code>Note</code>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Note
    extends MeasureNode
{
    //~ Instance fields --------------------------------------------------------

    /** The note shape */
    private Shape shape;

    /** Accidental is any */
    private Shape accidental;

    /** Pitch position */
    private double pitchPosition;

    /** The underlying glyph */
    private Glyph glyph;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Note //
    //------//
    /** Creates a new instance of Note */
    public Note (Measure measure)
    {
        super(measure);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }
}
