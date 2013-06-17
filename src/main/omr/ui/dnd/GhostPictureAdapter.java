//----------------------------------------------------------------------------//
//                                                                            //
//                   G h o s t P i c t u r e A d a p t e r                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

/**
 * Class {@code GhostPictureAdapter}is a {@link GhostDropAdapter} whose
 * image is retrieved from the class resource path.
 *
 * @param <A> The precise type of action carried by the drop
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostPictureAdapter<A>
        extends GhostDropAdapter<A>
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Create a new GhostPictureAdapter object
     *
     * @param glassPane The related glasspane
     * @param action    the carried action
     * @param picture   the path to the image resource
     */
    //---------------------//
    // GhostPictureAdapter //
    //---------------------//
    public GhostPictureAdapter (GhostGlassPane glassPane,
                                A action,
                                String picture)
    {
        super(glassPane, action);

        try {
            image = ImageIO.read(
                    new BufferedInputStream(
                    GhostPictureAdapter.class.getResourceAsStream(picture)));
        } catch (MalformedURLException mue) {
            throw new IllegalStateException("Invalid picture URL.");
        } catch (IOException ioe) {
            throw new IllegalStateException("Invalid picture or picture URL.");
        }
    }
}
