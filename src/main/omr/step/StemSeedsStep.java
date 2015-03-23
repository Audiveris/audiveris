//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t e m S e e d s S t e p                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.StemScale;
import omr.sheet.StemScaler;
import omr.sheet.SystemInfo;
import omr.sheet.VerticalsBuilder;
import omr.sheet.ui.SheetTab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code StemSeedsStep} implements <b>STEM_SEEDS</b> step, which retrieves all
 * vertical sticks that may constitute <i>seeds</i> of future stems.
 *
 * @author Hervé Bitteur
 */
public class StemSeedsStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StemSeedsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemSeedsStep object.
     */
    public StemSeedsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        // We need a system of this sheet (any one)
        SystemInfo aSystem = sheet.getSystems().get(0);

        // Add stem checkboard
        new VerticalsBuilder(aSystem).addCheckBoard();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        new VerticalsBuilder(system).buildVerticals(); // -> Stem seeds
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        StemScale stemScale = new StemScaler(sheet).retrieveStem();

        logger.info("{}{}", sheet.getLogPrefix(), stemScale);
        sheet.setStemScale(stemScale);
        sheet.getBench().recordStemScale(stemScale);

        return null;
    }
}
