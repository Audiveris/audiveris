//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       M i x P a i n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.ViewParameters;

/**
 * Class {@code MixPainter} prints the binary image in gray then the SIG contents,
 * with or without voices, according to user-selected parameters.
 *
 * @author Hervé Bitteur
 */
public abstract class MixPainter
{

    private MixPainter ()
    {
    }

    //-----------------//
    // SheetMixPainter //
    //-----------------//
    public static class SheetMixPainter
            implements SimpleSheetPainter
    {

        @Override
        public void paint (Sheet sheet,
                           Graphics2D g)
        {
            // White background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());

            // Binary
            Picture picture = sheet.getPicture();
            RunTable table = picture.getTable(Picture.TableKey.BINARY);
            g.setColor(Color.LIGHT_GRAY);
            table.render(g, new Point(0, 0));

            // Inters with their color (or voice color)
            final boolean withVoices = ViewParameters.getInstance().isVoicePainting();
            new SheetGradedPainter(sheet, g, withVoices, false).process();
        }
    }
}
