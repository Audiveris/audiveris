//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                 P r o c e s s i n g C a n c e l l a t i o n E x c e p t i o n                  //
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
package org.audiveris.omr.step;

/**
 * Class {@code ProcessingCancellationException} describes the exception raised then the
 * processing of a sheet has been canceled (typically because of time out).
 *
 * @author Hervé Bitteur
 */
public class ProcessingCancellationException
        extends RuntimeException
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Construct an {@code ProcessingCancellationException} with no detail
     * message.
     */
    public ProcessingCancellationException ()
    {
        super();
    }

    /**
     * Construct an {@code ProcessingCancellationException} with detail
     * message.
     *
     * @param message the related message
     */
    public ProcessingCancellationException (String message)
    {
        super(message);
    }

    /**
     * Construct an {@code ProcessingCancellationException} from an
     * existing exception.
     *
     * @param ex the related exception
     */
    public ProcessingCancellationException (Throwable ex)
    {
        super(ex);
    }
}
