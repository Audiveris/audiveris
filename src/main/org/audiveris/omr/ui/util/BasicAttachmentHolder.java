//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           B a s i c A t t a c h m e n t H o l d e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.symbol.OmrFont;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map for attachments. */
    protected Map<String, java.awt.Shape> attachments = new HashMap<>();

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
        if (attachments.isEmpty() || !ViewParameters.getInstance().isAttachmentPainting()) {
            return;
        }

        final Color oldColor = g.getColor();
        g.setColor(Colors.ATTACHMENT);

        final double zoom = g.getTransform().getScaleX();
        Font oldFont = null;
        Font font = null;

        if (constants.keyPainting.isSet() && (zoom >= constants.minZoomForKey.getValue())) {
            oldFont = g.getFont();

            final double std = constants.keyFontRatio.getValue() * UIUtil.GLOBAL_FONT_RATIO;
            final double z = Math.max(std, zoom);
            final AffineTransform at = AffineTransform.getScaleInstance(std / z, std / z);
            font = oldFont.deriveFont(at);
            g.setFont(font);
        }

        for (Map.Entry<String, Shape> entry : attachments.entrySet()) {
            // Draw shape
            final Shape shape = entry.getValue();
            g.draw(shape);

            if (font != null) {
                // Draw key
                final String key = entry.getKey();
                final Rectangle2D k = new TextLayout(key, font, OmrFont.frc).getBounds();
                final Rectangle2D s = shape.getBounds2D();
                g.drawString(key,
                             (int) Math.rint(s.getX() + (s.getWidth() / 2) - k.getWidth() / 2),
                             (int) Math.rint(s.getY() + (s.getHeight() / 2) + k.getHeight() / 2));
            }
        }

        if (oldFont != null) {
            g.setFont(oldFont);
        }

        g.setColor(oldColor);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean keyPainting = new Constant.Boolean(
                true,
                "Should the attachment key be painted?");

        private final Constant.Ratio minZoomForKey = new Constant.Ratio(
                2.0,
                "Minimum zoom value to display the attachment key");

        private final Constant.Ratio keyFontRatio = new Constant.Ratio(
                1.0,
                "Ratio of standard font size for the attachment key");
    }
}
