//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           B a s i c A t t a c h m e n t H o l d e r                            //
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
package org.audiveris.omr.ui.util;

import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.ViewParameters;

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
    //~ Instance fields ----------------------------------------------------------------------------

    /** Map for attachments */
    protected Map<String, java.awt.Shape> attachments = new HashMap<String, java.awt.Shape>();

    //~ Methods ------------------------------------------------------------------------------------
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
        List<String> toRemove = new ArrayList<String>();

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
        if (attachments.isEmpty() || !ViewParameters.getInstance().isAttachmentPainting()) {
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
            g.drawString(key, rect.x + (rect.width / 2), rect.y + (rect.height / 2));
        }

        g.setFont(oldFont);
        g.setColor(oldColor);
    }
}
