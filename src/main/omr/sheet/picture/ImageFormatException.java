//----------------------------------------------------------------------------//
//                                                                            //
//                  I m a g e F o r m a t E x c e p t i o n                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.picture;

/**
 * Class {@code ImageFormatException} describes an exception raised
 * when a non-handled format is detected in an image file.
 *
 * @author Hervé Bitteur
 */
public class ImageFormatException
        extends Exception
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Construct an {@code ImageFormatException} with provided detail
     * message.
     */
    public ImageFormatException (String message)
    {
        super(message);
    }
}
