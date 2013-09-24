//----------------------------------------------------------------------------//
//                                                                            //
//                            A n c h o r s S t e p                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.HorizontalsFilter;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code AnchorsStep} retrieves prominent sheet items (anchors)
 * that are used for composites made of beams, stems, ledgers, note
 * heads, etc.
 * It processes the sheet binary image (with its staff lines already recognized
 * but not removed), using mathematical morphology techniques to ease retrieval
 * of these anchors.
 *
 * @author Hervé Bitteur
 */
public class AnchorsStep
        extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AnchorsStep.class);

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // AnchorsStep //
    //-------------//
    /**
     * Creates a new AnchorsStep object.
     */
    public AnchorsStep ()
    {
        super(
                Steps.ANCHORS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve composite anchors");
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        // We need a system of this sheet (any one)
        SystemInfo aSystem = sheet.getSystems()
                .get(0);
        
        // Add stem checkboard
        aSystem.verticalsBuilder.addCheckBoard();

        // Add ledger checkboard
        aSystem.horizontalsBuilder.addCheckBoard();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        clearSystemErrors(system);
        system.beamsBuilder.buildBeams();
        system.horizontalsBuilder.buildLedgers();
        system.verticalsBuilder.buildVerticals();
        system.notesBuilder.buildBlackHeads();
        system.voidNotesBuilder.buildVoidHeads();
        system.stemsBuilder.linkHeads();
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * Perform a closing operation on the whole image with a disk
     * shape as the structure element.
     */
    @Override
    protected void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        // Retrieve significant spots for the whole sheet
        sheet.getSpotsBuilder()
                .buildSpots();

        // Retrieve long horizontal runs outside staves for ledgers/endings
        new HorizontalsFilter(sheet).process();
    }
}
