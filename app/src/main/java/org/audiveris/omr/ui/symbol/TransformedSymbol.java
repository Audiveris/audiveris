//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               T r a n s f o r m e d S y m b o l                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.glyph.Shape;

import java.awt.geom.AffineTransform;

/**
 * Class <code>TransformedSymbol</code> displays a baseShape symbol with AffineTransform.
 *
 * @author Hervé Bitteur
 */
public class TransformedSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The root shape, before transformation. */
    private final Shape root;

    /** Proper transformation. */
    private final AffineTransform at;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new TransformedSymbol object.
     *
     * @param shape  the related shape (after transformation)
     * @param root   the shape used as root (before transformation)
     * @param at     the AffineTransform to apply
     * @param family the musicFont family
     */
    public TransformedSymbol (Shape shape,
                              Shape root,
                              AffineTransform at,
                              MusicFamily family)
    {
        super(shape, family);
        this.root = root;
        this.at = at;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        final FontSymbol fs = root.getFontSymbol(font);
        final Params p = new Params();

        final MusicFont atFont = (at == null) ? fs.font
                : fs.font.deriveFont((float) at.getScaleX() * fs.font.getSize2D());
        p.layout = fs.symbol.getLayout(atFont);
        p.rect = p.layout.getBounds();

        return p;
    }
}
