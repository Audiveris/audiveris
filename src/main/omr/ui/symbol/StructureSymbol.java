//----------------------------------------------------------------------------//
//                                                                            //
//                       S t r u c t u r e S y m b o l                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;

import omr.score.common.PixelPoint;
import static omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code StructureSymbol} implements a decorated structure symbol
 *
 * @author Hervé Bitteur
 */
public class StructureSymbol
    extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The head+stem part
    // We draw 3 quarters tied by beams
    private static final BasicSymbol quarter = Symbols.SYMBOL_QUARTER;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // StructureSymbol //
    //-----------------//
    /**
     * Create a StructureSymbol
     */
    public StructureSymbol ()
    {
        this(false);
    }

    //-----------------//
    // StructureSymbol //
    //-----------------//
    /**
     * Create a StructureSymbol
     *
     * @param isIcon true for an icon
     */
    protected StructureSymbol (boolean isIcon)
    {
        super(isIcon, Shape.STRUCTURE, true); // Decorated
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        // Quarter layout
        p.layout = font.layout(quarter.getString());

        Rectangle2D qRect = p.layout.getBounds();
        p.quarterDx = (int) Math.rint(qRect.getWidth() * 2);
        p.quarterDy = (int) Math.rint(qRect.getWidth() * 0.8);

        p.rect = new Rectangle(
            (int) Math.ceil(qRect.getWidth() + (2 * p.quarterDx)),
            (int) Math.ceil(qRect.getHeight() + (2 * p.quarterDy)));

        return p;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new StructureSymbol(true);
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params     params,
                          PixelPoint location,
                          Alignment  alignment)
    {
        MyParams    p = (MyParams) params;
        PixelPoint  loc = alignment.translatedPoint(
            TOP_RIGHT,
            p.rect,
            location);

        // Beams
        Rectangle2D quarterRect = p.layout.getBounds();
        int         beamHeight = (int) Math.rint(
            quarterRect.getHeight() * 0.12);
        int         beamDelta = (int) Math.rint(quarterRect.getHeight() * 0.18);

        for (int col = 0; col < 2; col++) {
            for (int i = 0; i < (col + 1); i++) {
                Point   left = new Point(
                    loc.x - ((col + 1) * p.quarterDx),
                    loc.y + (i * beamDelta) + ((col + 1) * p.quarterDy));
                Point   right = new Point(
                    loc.x - (col * p.quarterDx),
                    loc.y + (i * beamDelta) + (col * p.quarterDy));
                Polygon polygon = new Polygon();
                polygon.addPoint(left.x, left.y);
                polygon.addPoint(left.x, left.y + beamHeight);
                polygon.addPoint(right.x, right.y + beamHeight);
                polygon.addPoint(right.x, right.y);
                g.fill(polygon);
            }
        }

        // Head + stem
        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
        loc.translate(-p.quarterDx, p.quarterDy);
        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
        loc.translate(-p.quarterDx, p.quarterDy);
        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected class MyParams
        extends Params
    {
        //~ Instance fields ----------------------------------------------------

        // layout for just quarter layout
        // rect for global image 

        // Dx between 2 quarters
        int quarterDx;

        // Dy between 2 quarters
        int quarterDy;
    }
}
