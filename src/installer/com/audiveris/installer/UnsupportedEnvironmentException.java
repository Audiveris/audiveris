//----------------------------------------------------------------------------//
//                                                                            //
//        U n s u p p o r t e d E n v i r o n m e n t E x c e p t i o n       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;


/**
 * Class {@code UnsupportedEnvironmentException} is used to signal
 * that the environment is not supported by the installer.
 *
 * @author Hervé Bitteur
 */
public class UnsupportedEnvironmentException
    extends RuntimeException
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new UnsupportedEnvironmentException object.
     */
    public UnsupportedEnvironmentException ()
    {
    }

    /**
     * Creates a new UnsupportedEnvironmentException object.
     *
     * @param message DOCUMENT ME!
     */
    public UnsupportedEnvironmentException (String message)
    {
        super(message);
    }

    /**
     * Creates a new UnsupportedEnvironmentException object.
     *
     * @param cause DOCUMENT ME!
     */
    public UnsupportedEnvironmentException (Throwable cause)
    {
        super(cause);
    }

    /**
     * Creates a new UnsupportedEnvironmentException object.
     *
     * @param message DOCUMENT ME!
     * @param cause DOCUMENT ME!
     */
    public UnsupportedEnvironmentException (String    message,
                                            Throwable cause)
    {
        super(message, cause);
    }
}
