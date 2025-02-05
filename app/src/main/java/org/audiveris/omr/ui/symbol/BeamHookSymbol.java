//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B e a m H o o k S y m b o l                                   //
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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>BeamHookSymbol</code> implements a decorated beam hook symbol
 *
 * @author Hervé Bitteur
 */
public class BeamHookSymbol
        extends BeamSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a BeamHookSymbol.
     *
     * @param family the musicFont family
     */
    public BeamHookSymbol (MusicFamily family)
    {
        super(Shape.BEAM_HOOK, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = super.getParams(font);

        // Cut standard beam length by 2
        final double width = (p.model.p2.getX() - p.model.p1.getX()) / 2;
        final double yShift = (p.model.p2.getY() - p.model.p1.getY()) / 2;
        final double absShift = Math.abs(yShift);

        if (isDecorated) {
            p.quarterCount = 1;

            Rectangle2D qRect = p.layout.getBounds();
            Rectangle2D r = p.rect;
            r.setRect(r.getX(), r.getY(), qRect.getWidth() + width, r.getHeight());

            if (yShift >= 0) {
                p.model.p1 = new Point2D.Double(r.getWidth() - width, p.model.thickness / 2.0);
                p.model.p2 = new Point2D.Double(r.getWidth(), (p.model.thickness / 2.0) + absShift);
            } else {
                p.model.p1 = new Point2D.Double(
                        r.getWidth() - width,
                        (p.model.thickness / 2.0) + absShift);
                p.model.p2 = new Point2D.Double(r.getWidth(), p.model.thickness / 2.0);
            }

            // Modify offset to point at center of beam hook
            p.offset = new Point2D.Double(
                    (r.getWidth() - width) / 2,
                    ((absShift + p.model.thickness) - r.getHeight()) / 2.0);
        } else {
            if (yShift >= 0) {
                p.model.p1 = new Point2D.Double(0, p.model.thickness / 2.0);
                p.model.p2 = new Point2D.Double(width, (p.model.thickness / 2.0) + absShift);
            } else {
                p.model.p1 = new Point2D.Double(0, (p.model.thickness / 2.0) + absShift);
                p.model.p2 = new Point2D.Double(width, p.model.thickness / 2.0);
            }

            p.rect = new Rectangle(
                    (int) Math.ceil(width),
                    (int) Math.ceil(p.model.thickness + absShift));
        }

        return p;
    }
}
