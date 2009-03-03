//----------------------------------------------------------------------------//
//                                                                            //
//                  I m a g e F o r m a t E x c e p t i o n                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet.picture;


/**
 * Class <code>ImageFormatException</code> describes an exception raised when a
 * non-handled format is detected in an image file.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ImageFormatException
    extends Exception
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Construct an <code>ImageFormatException</code> with provided detail
     * message.
     */
    public ImageFormatException (String message)
    {
        super(message);
    }
}
