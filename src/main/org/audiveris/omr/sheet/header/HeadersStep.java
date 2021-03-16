//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H e a d e r s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code HeadersStep} implements <b>HEADERS</b> step, which handles the beginning
 * of every staff in a system.
 *
 * @author Hervé Bitteur
 */
public class HeadersStep
        extends AbstractSystemStep<Void>
{

    private static final Logger logger = LoggerFactory.getLogger(HeadersStep.class);

    /**
     * Creates a new {@code HeadersStep} object.
     */
    public HeadersStep ()
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
        new HeaderBuilder(system).processHeader(); // -> Staff clef + key + time
    }
}
