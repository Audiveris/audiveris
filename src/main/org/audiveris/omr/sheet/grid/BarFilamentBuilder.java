//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B a r F i l a m e n t B u i l d e r                              //
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.lag.Section;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.Navigable;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code BarFilamentBuilder}
 *
 * @author Hervé Bitteur
 */
public class BarFilamentBuilder
{

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Specific factory for peak-based filaments. */
    private final BarFilamentFactory factory;

    /**
     * Creates a new {@code BarFilamentBuilder} object.
     *
     * @param sheet the containing sheet
     */
    public BarFilamentBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        factory = new BarFilamentFactory(sheet.getScale());
    }

    //---------------//
    // buildFilament //
    //---------------//
    /**
     * Build a bar/bracket/brace filament out of provided sections.
     *
     * @param peak              the peak to process
     * @param verticalExtension additional margin beyond staff to detect bracket/brace ends
     * @param allSections       large pre-filtered collection of candidates (ordered by abscissa)
     * @return the filament built, or null
     */
    public Filament buildFilament (StaffPeak peak,
                                   int verticalExtension,
                                   List<Section> allSections)
    {
        final Rectangle peakBox = peak.getBounds();

        // Increase height slightly beyond staff
        peakBox.grow(0, verticalExtension);

        final int xBreak = peakBox.x + peakBox.width;
        final List<Section> sections = new ArrayList<Section>();
        final int maxSectionWidth = peak.getWidth(); // Width of this particular peak

        for (Section section : allSections) {
            final Rectangle sectionBox = section.getBounds();

            if (sectionBox.intersects(peakBox)) {
                if (section.getLength(HORIZONTAL) <= maxSectionWidth) {
                    sections.add(section);
                }
            } else if (sectionBox.x >= xBreak) {
                break; // Since allSections are sorted by abscissa
            }
        }

        Filament filament = factory.buildBarFilament(sections, peak.getBounds());

        if (filament != null) {
            sheet.getFilamentIndex().register(filament);
        }

        return filament;
    }
}
