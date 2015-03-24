//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C h o r d s S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.note;

import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ChordsStep} gathers notes into chords and handle their relationships.
 *
 * @author Hervé Bitteur
 */
public class ChordsStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ChordsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ChordsStep object.
     */
    public ChordsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        // First, gather all system notes (heads & rests) into chords
        new ChordsBuilder(system).buildHeadChords();

        // Second, handle chord relationships with other symbols within the same system
        new ChordsLinker(system).linkChords();
    }
}
