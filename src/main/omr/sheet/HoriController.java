//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H o r i C o n t r o l l e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.GlyphNest;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.NestView;
import omr.glyph.ui.SymbolGlyphBoard;

import omr.lag.Lag;
import omr.lag.ui.SectionBoard;

import omr.sheet.ui.PixelBoard;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.view.ScrollView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Class {@code HoriController} display horizontal glyphs for ledgers etc.
 *
 * @author Hervé Bitteur
 */
public class HoriController
        extends GlyphsController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HoriController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final Lag lag;

    /** Related user display if any */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HoriController} object.
     *
     * @param sheet related sheet
     * @param lag   the full horizontal lag
     */
    public HoriController (Sheet sheet,
                           Lag lag)
    {
        super(new GlyphsModel(sheet, sheet.getNest(), null));
        this.lag = lag;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the display if any, with proper colors for sections
     */
    public void refresh ()
    {
        if (view == null) {
            displayFrame();
        } else if (view != null) {
            view.refresh();
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Specific rubber display
        view = new MyView(getNest());

        sheet.getAssembly().addViewTab(
                Step.LEDGER_TAB,
                new ScrollView(view),
                new BoardsPane(
                        new PixelBoard(sheet),
                        new SectionBoard(lag, false),
                        new SymbolGlyphBoard(this, true, true)));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (GlyphNest nest)
        {
            super(nest, Arrays.asList(lag), sheet);

            setLocationService(sheet.getLocationService());

            setName("HoriController-MyView");
        }
    }
}
