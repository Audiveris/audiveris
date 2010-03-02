//----------------------------------------------------------------------------//
//                                                                            //
//                               M y C l a s s                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>

import omr.glyph.Shape;

import omr.lag.LagOrientation;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.ui.ScoreDependent;

import omr.script.InsertTask;
import omr.script.Script;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.ui.symbol.SymbolRipper;

import org.jdesktop.application.Action;

import java.util.Arrays;
import java.util.Collection;

/**
 * Class <code>MyClass</code> is meant as just an example of user plugin
 *
 * @author Hervé Bitteur
 * @version $Id$
 */
public class MyClass
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MyClass.class);

    //~ Methods ----------------------------------------------------------------

    /**
     * Dump the script of the sheet currently selected
     */
    @Action(enabledProperty = "sheetAvailable")
    public void dumpCurrentScript ()
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            Script script = sheet.getScript();

            if (script != null) {
                script.dump();
            }
        }
    }

    /**
     * Test glyph injection
     */
    @Action(enabledProperty = "sheetAvailable")
    public void insertGlyph ()
    {
        Sheet sheet = SheetsController.selectedSheet();

        Collection<PixelPoint> points = Arrays.asList(
            new PixelPoint(1505, 725),
            new PixelPoint(1755, 725),
            new PixelPoint(1981, 735),
            new PixelPoint(2200, 533));

        new InsertTask(Shape.TUPLET_SIX, points, LagOrientation.VERTICAL).launch(
            sheet);
    }

    /**
     * Launch the computation of score dimensions
     */
    @Action(enabledProperty = "sheetAvailable")
    public void launchScoreComputations ()
    {
        Sheet sheet = SheetsController.selectedSheet();
        sheet.getScore()
             .accept(new ScoreDimensions());
    }

    /**
     * Launch the utility to rip a symbol
     */
    @Action
    public void launchSymbolRipper ()
    {
        SymbolRipper.main();
    }
}
