//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e m s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code StemsStep} implements <b>STEMS</b> step, which establishes all
 * possible relations between stems and note heads or beams.
 *
 * @author Hervé Bitteur
 */
public class StemsStep
        extends AbstractSystemStep<Void>
{

    private static final Logger logger = LoggerFactory.getLogger(StemsStep.class);

    /**
     * Creates a new StemsStep object.
     */
    public StemsStep ()
    {
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        // Inspect stems possibilities between heads & beams
        // Then link
        new StemsRetriever(system).inspectThenLink();
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Void context)
            throws StepException
    {
        // Further beams processing
        for (SystemInfo system : sheet.getSystems()) {
            new StemsRetriever(system).finalizeStems();

            // Compute all contextual grades (for better visual check)
            system.getSig().contextualize();
        }
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * Make sure that beams are gathered into {@link BeamGroupInter} instances.
     * <p>
     * This is needed to stay compatible with .omr files built before 5.2 release
     *
     * @param sheet the sheet to check
     * @return null
     * @throws StepException
     */
    @Override
    protected Void doProlog (Sheet sheet)
            throws StepException
    {
        for (SystemInfo system : sheet.getSystems()) {
            BeamGroupInter.checkSystemForOldBeamGroup(system);
        }

        return null;
    }
}
