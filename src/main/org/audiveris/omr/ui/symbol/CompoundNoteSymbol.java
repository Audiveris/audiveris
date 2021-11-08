//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C o m p o u n d N o t e S y m b o l                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import static org.audiveris.omr.glyph.Shape.HALF_NOTE_UP;
import static org.audiveris.omr.glyph.Shape.QUARTER_NOTE_UP;
import org.audiveris.omr.glyph.ShapeSet;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.CompoundNoteInter;

/**
 * Class <code>CompoundNoteSymbol</code> implements a head + stem symbol.
 *
 * @author Hervé Bitteur
 */
public class CompoundNoteSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final BasicSymbol stemSymbol = Symbols.SYMBOL_STEM;

    private static final BasicSymbol blackSymbol = new BasicSymbol(207);

    private static final BasicSymbol voidSymbol = new BasicSymbol(250);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a non-icon CompoundNoteSymbol.
     *
     * @param shape one of {@link ShapeSet#CompoundNotes}
     * @param codes the codes for MusicFont characters
     */
    public CompoundNoteSymbol (Shape shape,
                               int... codes)
    {
        this(false, shape, codes);
    }

    /**
     * Create a CompoundNoteSymbol.
     *
     * @param isIcon true for an icon
     * @param shape  one of {@link ShapeSet#CompoundNotes}
     * @param codes  the codes for MusicFont characters
     */
    public CompoundNoteSymbol (boolean isIcon,
                               Shape shape,
                               int... codes)
    {
        super(isIcon, shape, false, codes);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getModel //
    //----------//
    @Override
    public CompoundNoteInter.Model getModel (MusicFont font,
                                             Point location,
                                             Alignment alignment)
    {
        final MyParams p = getParams(font);

        // Location is assumed to be the head center
        final Point2D topLeft = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(topLeft.getX() - p.offset.getX(), topLeft.getY() - p.offset.getY());

        return p.model;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new CompoundNoteSymbol(true, shape, codes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();
        p.model = new CompoundNoteInter.Model();
        p.layout = font.layout(getString());

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
        final Rectangle2D stem = font.layout(stemSymbol.getString()).getBounds();
        final Point2D stemTop = isUp()
                ? new Point2D.Double(r.getWidth() - stem.getWidth() / 2, 0)
                : new Point2D.Double(stem.getWidth() / 2, -r.getY());
        p.model.stemBox = new Rectangle2D.Double(
                stemTop.getX() - stem.getWidth() / 2,
                stemTop.getY(),
                stem.getWidth(),
                stem.getHeight());

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point2D location,
                          Alignment alignment)
    {
        final MyParams p = (MyParams) params;
        OmrFont.paint(g, p.layout, location, alignment);
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
