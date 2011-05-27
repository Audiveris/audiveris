//----------------------------------------------------------------------------//
//                                                                            //
//                         S t a v e s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.step.StepException;

/**
 * Class {@code StavesBuilder} defines the interface for the retrieval of all
 * stave lines.
 *
 * <p>There is a current implementation by LinesBuilder.
 * <p>A new one is being prototyped using both horizontal and vertical long
 * sticks.
 *
 * @author Hervé Bitteur
 */
public interface StavesBuilder
{
    //~ Methods ----------------------------------------------------------------

    //    //-----------//
    //    // getStaves //
    //    //-----------//
    //    /**
    //     * Report the list of staves found in the sheet
    //     *
    //     * @return the collection of staves found
    //     */
    //    List<StaffInfo> getStaves ();

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Perform the retrieval of the various staves
     * @throws StepException is processing must stop
     */
    void buildInfo ()
        throws StepException;

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Display relevant data chart, if any
     */
    void displayChart ();
}
