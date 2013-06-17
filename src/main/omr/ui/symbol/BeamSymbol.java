//----------------------------------------------------------------------------//
//                                                                            //
//                            B e a m S y m b o l                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;
import static omr.ui.symbol.Alignment.*;

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BeamSymbol} implements decorated beam symbols.
 * (non-decorated beams are painted using polygons rather than symbols)
 *
 * @author Hervé Bitteur
 */
public class BeamSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers ---------------------------------------------

    // The head+stem part
    private static final BasicSymbol quarter = Symbols.SYMBOL_QUARTER;

    //~ Instance fields --------------------------------------------------------
    // Number of beams
    protected final int beamCount;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // BeamSymbol //
    //------------//
    /**
     * Create a BeamSymbol.
     *
     * @param beamCount the number of beams
     * @param shape     the precise shape
     */
    public BeamSymbol (int beamCount,
                       Shape shape)
    {
        this(beamCount, false, shape);
    }

    //------------//
    // BeamSymbol //
    //------------//
    /**
     * Create a BeamSymbol.
     *
     * @param beamCount the number of beams
     * @param isIcon    true for an icon
     * @param shape     the precise shape
     */
    protected BeamSymbol (int beamCount,
                          boolean isIcon,
                          Shape shape)
    {
        super(isIcon, shape, true); // Decorated
        this.beamCount = beamCount;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BeamSymbol(beamCount, true, shape);
    }

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
                (int) Math.ceil(qRect.getWidth() + p.quarterDx),
                (int) Math.ceil(qRect.getHeight() + p.quarterDy));

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;
        Point loc = alignment.translatedPoint(
                TOP_RIGHT,
                p.rect,
                location);

        // Beams
        Rectangle2D quarterRect = p.layout.getBounds();
        int beamHeight = (int) Math.rint(
                quarterRect.getHeight() * 0.12);
        int beamDelta = (int) Math.rint(quarterRect.getHeight() * 0.18);

        for (int i = 0; i < beamCount; i++) {
            Point left = new Point(
                    loc.x - p.quarterDx,
                    loc.y + (i * beamDelta) + p.quarterDy);
            Point right = new Point(loc.x, loc.y + (i * beamDelta));
            Polygon polygon = new Polygon();
            polygon.addPoint(left.x, left.y);
            polygon.addPoint(left.x, left.y + beamHeight);
            polygon.addPoint(right.x, right.y + beamHeight);
            polygon.addPoint(right.x, right.y);
            g.fill(polygon);
        }

        // Decorations (using composite)
        Composite oldComposite = g.getComposite();
        g.setComposite(decoComposite);

        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);
        loc.translate(-p.quarterDx, p.quarterDy);
        MusicFont.paint(g, p.layout, loc, TOP_RIGHT);

        g.setComposite(oldComposite);
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
        // Between the 2 quarters
        int quarterDx;

        // Between the 2 quarters
        int quarterDy;

    }
}
