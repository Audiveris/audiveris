//----------------------------------------------------------------------------//
//                                                                            //
//                          M e a s u r e s S t e p                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.log.Logger;

import omr.score.MeasureBasicNumberer;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code MeasuresStep} retrieves the measures from the barline glyphs
 *
 * @author Hervé Bitteur
 */
public class MeasuresStep
    extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MeasuresStep.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // MeasuresStep //
    //--------------//
    /**
     * Creates a new MeasuresStep object.
     */
    public MeasuresStep ()
    {
        super(
            Steps.MEASURES,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.REDOABLE,
            SYSTEMS_TAB,
            "Retrieve Measures from Bar sticks");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        //        Main.getGui().scoreController.setScoreEditor(sheet.getScore());
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
        throws StepException
    {
        if (Main.getGui() != null) {
            system.getSheet()
                  .getErrorsEditor()
                  .clearSystem(this, system.getId());
        }

        system.buildMeasures(); // For Measures
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet                  sheet)
        throws StepException
    {
        // Assign basic measure ids
        sheet.getPage()
             .accept(new MeasureBasicNumberer());

        // Log the number of measures per system
        sheet.getPage()
             .dumpMeasureCounts();
    }
}
