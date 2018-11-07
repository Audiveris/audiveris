//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   F i x e d W i d t h I c o n                                  //
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
package org.audiveris.omr.ui.util;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;

import javax.swing.Icon;

/**
 * Force an icon to be painted within a given width.
 */
public class FixedWidthIcon
        implements Icon
{

    private static final int width = 35; // TODO: use an application constant

    private final Icon icon;

    /**
     * Creates a new {@code FixedIcon} object.
     *
     * @param icon the underlying icon
     */
    public FixedWidthIcon (Icon icon)
    {
        Objects.requireNonNull(icon, "FixedWidthIcon needs a non-null icon");
        this.icon = icon;
    }

    @Override
    public int getIconHeight ()
    {
        return icon.getIconHeight();
    }

    @Override
    public int getIconWidth ()
    {
        return width;
    }

    @Override
    public void paintIcon (Component c,
                           Graphics g,
                           int x,
                           int y)
    {
        int w = icon.getIconWidth();
        icon.paintIcon(c, g, x + ((width - w) / 2), y);
    }
}
