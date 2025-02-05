//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C o m p o u n d N o t e S y m b o l                              //
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

import static org.audiveris.omr.glyph.Shape.HALF_NOTE_UP;
import static org.audiveris.omr.glyph.Shape.QUARTER_NOTE_UP;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.CompoundNoteInter;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>CompoundNoteSymbol</code> implements a head + stem symbol.
 *
 * @author Hervé Bitteur
 */
public class CompoundNoteSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a standard size CompoundNoteSymbol.
     *
     * @param shape  one of {@link ShapeSet#CompoundNotes}
     * @param family the musicFont family
     */
    public CompoundNoteSymbol (Shape shape,
                               MusicFamily family)
    {
        super(shape, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getModel //
    //----------//
    @Override
    public CompoundNoteInter.Model getModel (MusicFont font,
                                             Point location)
    {
        final MyParams p = getParams(font);
        p.model.translate(p.vectorTo(location));

        return p.model;
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();
        p.model = new CompoundNoteInter.Model();
        p.layout = font.layoutShapeByCode(shape);

        final Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle2D.Double(0, 0, r.getWidth(), r.getHeight());
        p.model.box = p.rect.getBounds2D();

        // Specific offset of head center WRT area center
        p.model.headCenter = new Point2D.Double(r.getWidth() / 2, -r.getY());
        final Point2D areaCenter = GeoUtil.center2D(p.rect);
        p.offset = PointUtil.subtraction(p.model.headCenter, areaCenter);
        p.model.headBox = new Rectangle2D.Double(
                0,
                isUp() ? -2 * r.getY() - r.getHeight() : 0,
                r.getWidth(),
                isUp() ? 2 * (r.getHeight() + r.getY()) : -2 * r.getY());

        // Stem
        final Rectangle2D stem = font.layoutShapeByCode(Shape.STEM).getBounds();
        final Point2D stemTop = isUp() ? new Point2D.Double(r.getWidth() - stem.getWidth() / 2, 0)
                : new Point2D.Double(stem.getWidth() / 2, -r.getY());
        p.model.stemBox = new Rectangle2D.Double(
                stemTop.getX() - stem.getWidth() / 2,
                stemTop.getY(),
                stem.getWidth(),
                stem.getHeight());

        return p;
    }

    //------//
    // isUp //
    //------//
    private boolean isUp ()
    {
        return (shape == QUARTER_NOTE_UP) || (shape == HALF_NOTE_UP);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends Params
    {
        // offset used (area center to head center)
        // layout used (compound layout)
        // rect used (compound bounds)

        // model
        CompoundNoteInter.Model model;

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder("noteParams{");
            sb.append(model);
            sb.append(" offset:").append(offset);
            return sb.append('}').toString();
        }
    }
}
