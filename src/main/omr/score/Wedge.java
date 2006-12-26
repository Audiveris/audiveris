//----------------------------------------------------------------------------//
//                                                                            //
//                                 W e d g e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;

/**
 * Class <code>Wedge</code> represents a crescendo or decrescendo (diminuendo)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Wedge
    extends PartNode
{
    //~ Instance fields --------------------------------------------------------

    /** Underlying glyph */
    private final Glyph glyph;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Wedge //
    //-------//
    /**
     * Creates a new instance of Wedge
     * @param part the containing system part
     * @param glyph the underlying glyph
     */
    public Wedge (SystemPart part,
                  Glyph      glyph)
    {
        super(part);

        this.glyph = glyph;
    }
}
