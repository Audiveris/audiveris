//----------------------------------------------------------------------------//
//                                                                            //
//                              B a r s V i e w                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;

import omr.lag.ui.SectionView;

import omr.log.Logger;

import java.awt.*;
import java.util.List;

/**
 * Class {@code BarsView} is a {@link GlyphLagView} meant for the display of
 * Bars filaments
 *
 * @author Hervé Bitteur
 */
public class BarsView
    extends GlyphLagView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GridBuilder.class);

    /** Color for barline-shape glyphs */
    public static final Color shapeColor = new Color(150, 150, 255);

    //~ Instance fields --------------------------------------------------------

    // Companion for verticals (barlines)
    private final BarsRetriever barsRetriever;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // BarsView //
    //----------//
    /**
     * Creates a new BarsView object.
     *
     * @param vLag vertical lag
     * @param barsRetriever the related model
     * @param specifics specific sections if any
     * @param controller glyphs controller
     */
    public BarsView (GlyphLag           vLag,
                     BarsRetriever      barsRetriever,
                     List<GlyphSection> specifics,
                     GlyphsController   controller)
    {
        super(vLag, specifics, constants.displaySpecifics, controller, null);

        setName("Bars-View");
        this.barsRetriever = barsRetriever;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // colorizeAllGlyphs //
    //-------------------//
    @Override
    public void colorizeAllGlyphs ()
    {
        int viewIndex = lag.viewIndexOf(this);

        // All bar glyphs candidates
        for (Glyph glyph : lag.getActiveGlyphs()) {
            Shape shape = glyph.getShape();

            Color color = ((shape == Shape.THICK_BARLINE) ||
                          (shape == Shape.THIN_BARLINE)) ? shapeColor
                          : GridView.verticalColor;
            glyph.colorize(viewIndex, color);
        }

        // Glyphs actually parts of true bar lines
        for (Glyph glyph : barsRetriever.getBarlineGlyphs()) {
            glyph.colorize(viewIndex, Color.BLUE);
        }
    }

    //-----------------//
    // colorizeSection //
    //-----------------//
    @Override
    protected void colorizeSection (GlyphSection section,
                                    int          viewIndex)
    {
        Glyph glyph = section.getGlyph();
        Color color = GridView.verticalColor;

        if (glyph != null) {
            Shape shape = glyph.getShape();

            color = ((shape == Shape.THICK_BARLINE) ||
                    (shape == Shape.THIN_BARLINE)) ? shapeColor
                    : GridView.verticalColor;
        }

        // vLag
        SectionView view = (SectionView) section.getView(viewIndex);
        view.setColor(color);
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        barsRetriever.renderItems(g);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displaySpecifics = new Constant.Boolean(
            false,
            "Dummy stuff");
    }
}
