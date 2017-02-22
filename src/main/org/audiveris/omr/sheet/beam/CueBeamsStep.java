//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C u e B e a m s S t e p                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

/**
 * Class {@code CueBeamsStep} implements <b>CUE_BEAMS</b> step, which attempts to
 * retrieve beams for cue notes.
 *
 * @author Hervé Bitteur
 */
public class CueBeamsStep
        extends AbstractSystemStep<Void>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new CueBeamsStep object.
     */
    public CueBeamsStep ()
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
        new BeamsBuilder(system, null, null).buildCueBeams(); // -> Cue beams
    }
}
