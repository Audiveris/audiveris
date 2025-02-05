//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 H e a d S p o t s B u i l d e r                                //
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class <code>HeadSpotsBuilder</code> builds spot glyphs meant to guide head retrieval.
 *
 * @author Hervé Bitteur
 */
public class HeadSpotsBuilder
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    private final Sheet sheet;

    /** Spot glyphs, per system. */
    Map<SystemInfo, List<Glyph>> glyphMap = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>HeadSpotsBuilder</code> object.
     *
     * @param sheet the related sheet
     */
    public HeadSpotsBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------------//
    // dispatchSheetSpots //
    //--------------------//
    /**
     * Dispatch sheet spots according to their containing system(s),
     * and keeping only those within system width.
     *
     * @param spots the spots to dispatch
     */
    private Map<SystemInfo, List<Glyph>> dispatchSheetSpots (List<Glyph> spots)
    {
        final SystemManager systemManager = sheet.getSystemManager();
        final Map<SystemInfo, List<Glyph>> spotMap = new TreeMap<>();
        for (SystemInfo system : systemManager.getSystems()) {
            spotMap.put(system, new ArrayList<>());
        }

        for (Glyph spot : spots) {
            final Point center = spot.getCentroid();
            final List<SystemInfo> relevants = systemManager.getSystemsOf(center);

            for (SystemInfo system : relevants) {
                // Check glyph is within system abscissa boundaries
                if ((center.x >= system.getLeft()) && (center.x <= system.getRight())) {
                    spot.addGroup(GlyphGroup.HEAD_SPOT); // Needed
                    spotMap.get(system).add(spot);
                }
            }
        }

        return spotMap;
    }

    //----------//
    // getSpots //
    //----------//
    /**
     * Retrieve the glyphs out of buffer runs.
     *
     * @return the map of spot glyphs per system
     */
    public Map<SystemInfo, List<Glyph>> getSpots ()
    {
        RunTable headRuns = sheet.getPicture().getVerticalTable(Picture.TableKey.HEAD_SPOTS);
        List<Glyph> spots = GlyphFactory.buildGlyphs(
                headRuns,
                new Point(0, 0),
                GlyphGroup.HEAD_SPOT);

        // Dispatch spots per system(s)
        return dispatchSheetSpots(spots);
    }
}
