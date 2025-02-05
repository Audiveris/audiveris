//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N o S t a f f V i e w                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;

import ij.process.ByteProcessor;

import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * Class <code>NoStaffView</code> is a RubberPanel which displays the NO_STAFF buffer.
 *
 * @author Hervé Bitteur
 */
public class NoStaffView
        extends ImageView
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final Sheet sheet;

    private ByteProcessor buffer;

    //~ Constructors -------------------------------------------------------------------------------

    public NoStaffView (Sheet sheet)
    {
        super(null);

        this.sheet = sheet;

        setName("No-Staff-View");

        setModelSize(new Dimension(sheet.getWidth(), sheet.getHeight()));
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void render (Graphics2D g)
    {
        // Buffer updated?
        final ByteProcessor noStaff = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);

        if (buffer != noStaff) {
            buffer = noStaff;
            image = buffer.getBufferedImage();
        }

        if (image != null) {
            g.drawRenderedImage(image, null);
        }
    }
}
