//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t e m S e e d s S t e p                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.stem;

import omr.sheet.Scale.StemScale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.Step;
import omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected Void doProlog (Sheet sheet)
            throws StepException
    {
        // Retrieve typical stem width on global sheet
        StemScale stemScale = new StemScaler(sheet).retrieveStemWidth();

        logger.info("{}", stemScale);
        sheet.getScale().setStemScale(stemScale);

        return null;
    }
}
