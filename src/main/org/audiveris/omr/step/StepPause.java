//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e p P a u s e                                       //
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
package org.audiveris.omr.step;

/**
 * Class {@code StepPause} is a soft version of {@link StepException} so that processing
 * can gracefully stop at end of current step.
 * <p>
 * Current step is considered as successfully done, but processing stops at step end.
 * The user would have to resume processing for subsequent steps if desired.
 *
 * @author Hervé Bitteur
 */
public class StepPause
        extends StepException
{

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Construct an {@code StepPause} with detail message.
     *
     * @param message the related message
     */
    public StepPause (String message)
    {
        super(message);
    }
}
