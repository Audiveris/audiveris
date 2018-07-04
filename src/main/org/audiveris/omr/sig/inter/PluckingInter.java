//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P l u c k i n g I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omrdataset.api.OmrShape;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code PluckingInter} represents the fingering for guitar right-hand.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "plucking")
public class PluckingInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Letter for the finger. (p, i, m, a) */
    @XmlAttribute
    private final char letter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PluckingInter} object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public PluckingInter (Glyph glyph,
                          Shape shape,
                          double grade)
    {
        super(glyph, null, shape, grade);
        this.letter = valueOf(shape);
    }

    /**
     * Creates a new {@code PluckingInter} object.
     *
     * @param annotationId ID of the original annotation if any
     * @param bounds       bounding box
     * @param omrShape     precise shape
     * @param grade        evaluation value
     */
    public PluckingInter (int annotationId,
                          Rectangle bounds,
                          OmrShape omrShape,
                          double grade)
    {
        super(annotationId, bounds, omrShape, grade);
        this.letter = valueOf(omrShape);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private PluckingInter ()
    {
        this.letter = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        return String.valueOf(letter);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + letter;
    }

    //---------//
    // valueOf //
    //---------//
    private static char valueOf (Shape shape)
    {
        switch (shape) {
        case PLUCK_P:
            return 'p';

        case PLUCK_I:
            return 'i';

        case PLUCK_M:
            return 'm';

        case PLUCK_A:
            return 'a';
        }

        throw new IllegalArgumentException("Invalid plucking shape " + shape);
    }

    //---------//
    // valueOf //
    //---------//
    private static char valueOf (OmrShape omrShape)
    {
        switch (omrShape) {
        case fingeringPLower:
            return 'p';

        case fingeringILower:
            return 'i';

        case fingeringMLower:
            return 'm';

        case fingeringALower:
            return 'a';
        }

        throw new IllegalArgumentException("Invalid plucking shape " + omrShape);
    }
}
