//----------------------------------------------------------------------------//
//                                                                            //
//                      A t t a c h m e n t H o l d e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Map;

/**
 * Interface {@code AttachmentHolder} defines the handling of visual
 * attachments than can be displayed on user views.
 *
 * @author Hervé Bitteur
 */
public interface AttachmentHolder
{
    //~ Methods ----------------------------------------------------------------

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
