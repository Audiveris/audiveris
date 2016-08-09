//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e m s S t e p                                       //
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

import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StemsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemsStep object.
     */
    public StemsStep ()
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
        // -> Stems links to heads & beams
        new StemsBuilder(system).linkStems();

        // Compute all contextual grades (for better UI)
        system.getSig().contextualize();
    }
}
