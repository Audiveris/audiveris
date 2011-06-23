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

    //~ Instance fields --------------------------------------------------------

    // Companion for verticals (barlines)
    private final BarsRetriever barsRetriever;

    // Max section length 
    private int maxLength;

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

        maxLength = maxLengthOf(vLag);

        setName("Bars-View");
        this.barsRetriever = barsRetriever;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // colorizeSection //
    //-----------------//
    @Override
    protected void colorizeSection (GlyphSection section,
                                    int          viewIndex)
    {
        int   length = section.getLength();
        Color color;

        // vLag
        int level = (int) Math.rint(240 * (1 - (length / (double) maxLength)));
        color = new Color(level, level, 255); // Blue gradient

        //        } else {
        //            // hLag
        //            // Flag too thick sections
        //            if (linesRetriever.isSectionFat(section)) {
        //                color = Color.GRAY;
        //            } else {
        //                int level = (int) Math.rint(
        //                    200 * (1 - (length / (double) maxLength)));
        //                color = new Color(255, level, level); // Red Gradient
        //            }
        //        }
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

    //-------------//
    // maxLengthOf //
    //-------------//
    private int maxLengthOf (GlyphLag lag)
    {
        // Retrieve max section length in the lag
        int maxLength = 0;

        for (GlyphSection section : lag.getVertices()) {
            maxLength = Math.max(maxLength, section.getLength());
        }

        return maxLength;
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
