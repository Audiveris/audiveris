//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A t t a c h m e n t H o l d e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.ui.util;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Map;

/**
 * Interface {@code AttachmentHolder} defines the handling of visual attachments than
 * can be displayed on user views.
 *
 * @author Hervé Bitteur
 */
public interface AttachmentHolder
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Register an attachment with a key and a shape.
     * This is meant to add arbitrary awt shapes to an entity, mainly for
     * display and analysis purposes.
     *
     * @param id         the attachment ID
     * @param attachment Shape to attach. If null, attachment is ignored.
     */
    void addAttachment (String id,
                        Shape attachment);

    /**
     * Report a view on the map of attachments.
     *
     * @return a (perhaps empty) map of attachments
     */
    Map<String, Shape> getAttachments ();

    /**
     * Remove all the attachments whose id begins with the provided
     * prefix.
     *
     * @param prefix the beginning of ids
     * @return the number of attachments removed
     */
    int removeAttachments (String prefix);

    /**
     * Render the attachments on the provided graphics context.
     *
     * @param g the graphics context
     */
    void renderAttachments (Graphics2D g);
}
