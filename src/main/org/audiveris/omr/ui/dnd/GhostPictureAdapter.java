//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             G h o s t P i c t u r e A d a p t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.ui.dnd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

/**
 * Class {@code GhostPictureAdapter} is a {@link GhostDropAdapter} whose image is
 * retrieved from the class resource path.
 *
 * @param <A> The precise type of action carried by the drop
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostPictureAdapter<A>
        extends GhostDropAdapter<A>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new GhostPictureAdapter object
     *
     * @param glassPane The related glasspane
     * @param action    the carried action
     * @param picture   the path to the image resource
     */
    public GhostPictureAdapter (GhostGlassPane glassPane,
                                A action,
                                String picture)
    {
        super(glassPane, action);

        try {
            image = ImageIO.read(
                    new BufferedInputStream(GhostPictureAdapter.class.getResourceAsStream(picture)));
        } catch (MalformedURLException mue) {
            throw new IllegalStateException("Invalid picture URL.");
        } catch (IOException ioe) {
            throw new IllegalStateException("Invalid picture or picture URL.");
        }
    }
}
