//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  L o g S t e p A p p e n d e r                                 //
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
package org.audiveris.omr.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import org.audiveris.omr.step.ui.StepMonitoring;

/**
 * Class {@code LogStepAppender} uses the flow of logging messages (assumed to be
 * filtered on INFO level at least) to notify a slight progress.
 * <p>
 * Filtering on level is performed in the logging configuration file (if any).
 *
 * @author Hervé Bitteur
 */
public class LogStepAppender
        extends AppenderBase<ILoggingEvent>
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    protected void append (ILoggingEvent event)
    {
        if (event.getLevel().toInt() >= Level.INFO_INT) {
            StepMonitoring.animate();
        }
    }
}
