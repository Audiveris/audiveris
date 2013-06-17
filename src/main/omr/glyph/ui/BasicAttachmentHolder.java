//----------------------------------------------------------------------------//
//                                                                            //
//                 B a s i c A t t a c h m e n t H o l d e r                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.ui.Colors;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code BasicAttachmentHolder} is a simple implementation of
 * {@link AttachmentHolder} interface.
 *
 * @author Hervé Bitteur
 */
public class BasicAttachmentHolder
        implements AttachmentHolder
{
    //~ Instance fields --------------------------------------------------------

    /** Map for attachments */
    protected Map<String, java.awt.Shape> attachments = new HashMap<>();

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // addAttachment //
    //---------------//
    @Override
    public void addAttachment (String id,
                               Shape attachment)
    {
        if (attachment != null) {
            attachments.put(id, attachment);
        }
    }

    //----------------//
    // getAttachments //
    //----------------//
    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        return Collections.unmodifiableMap(attachments);
    }

    //-------------------//
    // removeAttachments //
    //-------------------//
    @Override
    public int removeAttachments (String prefix)
    {
        // To avoid concurrent modifications
        List<String> toRemove = new ArrayList<>();

        for (String key : attachments.keySet()) {
            if (key.startsWith(prefix)) {
                toRemove.add(key);
            }
        }

        for (String key : toRemove) {
            attachments.remove(key);
        }

        return toRemove.size();
    }

    //-------------------//
    // renderAttachments //
    //-------------------//
    @Override
    public void renderAttachments (Graphics2D g)
    {
        if (attachments.isEmpty()
            || !ViewParameters.getInstance()
                .isAttachmentPainting()) {
            return;
        }

        Color oldColor = g.getColor();
        g.setColor(Colors.ATTACHMENT);

        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont(4f));

        for (Map.Entry<String, Shape> entry : attachments.entrySet()) {
            Shape shape = entry.getValue();
            g.draw(shape);

            String key = entry.getKey();
            Rectangle rect = shape.getBounds();
            g.drawString(
                    key,
                    rect.x + (rect.width / 2),
                    rect.y + (rect.height / 2));
        }

        g.setFont(oldFont);
        g.setColor(oldColor);
    }
}
