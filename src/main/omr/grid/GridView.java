//----------------------------------------------------------------------------//
//                                                                            //
//                              G r i d V i e w                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Scene;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.SceneView;

import omr.lag.Lag;

import omr.log.Logger;

import java.awt.Graphics2D;
import java.util.Arrays;

/**
 * Class {@code GridView} is a special {@link SceneView}, meant as a
 * companion of {@link GridBuilder} with its 2 lags (horizontal & vertical).
 * <p>We paint on the same display the vertical and horizontal sections.
 *
 * @author Hervé Bitteur
 */
public class GridView
    extends SceneView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GridBuilder.class);

    //~ Instance fields --------------------------------------------------------

    // Companion for horizontals (staff lines)
    private final LinesRetriever linesRetriever;

    // Companion for verticals (barlines)
    private final BarsRetriever barsRetriever;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GridView //
    //----------//
    /**
     * Creates a new GridView object.
     *
     * @param linesRetriever the related lines retriever
     * @param hLag horizontal lag
     * @param barsRetriever the related bars retriever
     * @param vLag vertical lag
     * @param controller glyphs controller
     */
    public GridView (Scene            scene,
                     LinesRetriever   linesRetriever,
                     Lag              hLag,
                     BarsRetriever    barsRetriever,
                     Lag              vLag,
                     GlyphsController controller)
    {
        super(scene, controller, Arrays.asList(hLag, vLag));

        setName("Grid-View");
        this.linesRetriever = linesRetriever;
        this.barsRetriever = barsRetriever;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        boolean showTangents = constants.showTangents.getValue();
        boolean showCombs = constants.showCombs.getValue();

        // Horizontal items
        linesRetriever.renderItems(g, showTangents, showCombs);

        // Vertical items
        barsRetriever.renderItems(g, showTangents);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean showTangents = new Constant.Boolean(
            true,
            "Should we show filament ending tangents?");
        Constant.Boolean showCombs = new Constant.Boolean(
            true,
            "Should we show staff lines combs?");
    }
}
