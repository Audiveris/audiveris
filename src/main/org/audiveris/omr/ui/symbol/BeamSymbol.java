//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B e a m S y m b o l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.BeamScale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BeamSymbol} implements beam symbols.
 * <p>
 * Note: on score, beams are painted using areas rather than symbols.
 * However, beam symbols (decorated and non-decorated) are still needed for user drag and drop.
 *
 * @author Hervé Bitteur
 */
public class BeamSymbol
        extends ShapeSymbol
{

    // The decorating quarter (head + stem) part
    private static final BasicSymbol quarter = Symbols.SYMBOL_QUARTER;

    /** Specified beam thickness, if any, as a ratio of interline. */
    protected Double thicknessFraction;

    /**
     * Create a BeamSymbol.
     *
     * @param decorated true for a decorated image
     */
    public BeamSymbol (boolean decorated)
    {
        this(false, Shape.BEAM, null, decorated);
    }

    /**
     * Create a BeamSymbol.
     *
     * @param thicknessFraction specified thickness, if any
     * @param decorated         true for a decorated image
     */
    public BeamSymbol (Double thicknessFraction,
                       boolean decorated)
    {
        this(false, Shape.BEAM, thicknessFraction, decorated);
    }

    //------------//
    // BeamSymbol //
    //------------//
    /**
     * Create a BeamSymbol.
     *
     * @param isIcon            true for an icon
     * @param shape             the precise shape
     * @param thicknessFraction specified thickness, if any
     * @param decorated         true for a decorated image
     */
    protected BeamSymbol (boolean isIcon,
                          Shape shape,
                          Double thicknessFraction,
                          boolean decorated)
    {
        super(isIcon, shape, decorated);
        this.thicknessFraction = thicknessFraction;
    }

    //----------//
    // getModel //
    //----------//
    @Override
    public AbstractBeamInter.Model getModel (MusicFont font,
                                             Point location,
                                             Alignment alignment)
    {
        MyParams p = getParams(font);
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(loc.getX(), loc.getY());

        return p.model;
    }

    //-------------//
    // updateModel //
    //-------------//
    @Override
    public void updateModel (Sheet sheet)
    {
        // We use this call to precisely adapt beam thickness using sheet scale info on beams
        final Scale scale = sheet.getScale();
        final BeamScale beamScale = scale.getBeamScale();

        if (!beamScale.isExtrapolated()) {
            thicknessFraction = beamScale.getDistanceMean() / scale.getInterline();
        }
    }

    //-----------------------//
    // createDecoratedSymbol //
    //-----------------------//
    @Override
    protected ShapeSymbol createDecoratedSymbol ()
    {
        return new BeamSymbol(isIcon, shape, thicknessFraction, true);
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BeamSymbol(true, shape, thicknessFraction, decorated);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();
        p.model = new AbstractBeamInter.Model();

        final int il = font.getStaffInterline();
        final double fraction = (thicknessFraction != null) ? thicknessFraction : 0.5;
        p.model.thickness = Math.rint(il * fraction);
        double width = il * 2.0; // Beam width
        double yShift = 0; ///-il * 1.0; // Non zero for a slanted beam (p2.y - p1.y)
        double absShift = Math.abs(yShift);

        p.layout = font.layout(quarter.getString()); // Quarter layout

        if (decorated) {
            p.quarterCount = 2;
            Rectangle2D qRect = p.layout.getBounds();
            p.rect = new Rectangle2D.Double(0,
                                            0,
                                            qRect.getWidth() + width,
                                            qRect.getHeight() + absShift);

            if (yShift >= 0) {
                p.model.p1 = new Point2D.Double(qRect.getWidth(), p.model.thickness / 2.0);
                p.model.p2 = new Point2D.Double(qRect.getWidth() + width,
                                                p.model.thickness / 2.0 + absShift);
            } else {
                p.model.p1 = new Point2D.Double(qRect.getWidth(),
                                                p.model.thickness / 2.0 + absShift);
                p.model.p2 = new Point2D.Double(qRect.getWidth() + width, p.model.thickness / 2.0);
            }

            // Define specific offset to point at center of beam
            p.offset = new Point(
                    (int) Math.rint((p.rect.getWidth() - width) / 2.0),
                    (int) Math.rint((absShift + p.model.thickness - p.rect.getHeight()) / 2.0));
        } else {
            if (yShift >= 0) {
                p.model.p1 = new Point2D.Double(0, p.model.thickness / 2.0);
                p.model.p2 = new Point2D.Double(width, p.model.thickness / 2.0 + absShift);
            } else {
                p.model.p1 = new Point2D.Double(0, p.model.thickness / 2.0 + absShift);
                p.model.p2 = new Point2D.Double(width, p.model.thickness / 2.0);
            }

            p.rect = new Rectangle((int) Math.ceil(width),
                                   (int) Math.ceil(p.model.thickness + absShift));
        }

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
        MyParams p = (MyParams) params;
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(loc.getX(), loc.getY());

        // Beam
        Area area = AreaUtil.horizontalParallelogram(p.model.p1, p.model.p2, p.model.thickness);
        g.fill(area);

        if (decorated) {
            // Draw the two quarters
            Composite oldComposite = g.getComposite();
            g.setComposite(decoComposite);
            final int yShift = (int) Math.rint(p.model.p2.getY() - p.model.p1.getY());
            final int absShift = Math.abs(yShift);

            if (yShift < 0) {
                PointUtil.add(loc, 0, absShift);
            }

            for (int iq = 0; iq < p.quarterCount; iq++) {
                // begin by left side (p1)
                MusicFont.paint(g, p.layout, loc, TOP_LEFT);
                PointUtil.add(loc, p.model.p2.getX() - p.model.p1.getX(), yShift);
            }

            g.setComposite(oldComposite);
        }
    }

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {

        // layout for just quarter layout
        // rect for global image
        //
        // model
        AbstractBeamInter.Model model;

        // number of decorating quarters
        int quarterCount;
    }
}
