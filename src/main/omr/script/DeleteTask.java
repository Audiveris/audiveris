//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      D e l e t e T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.script;

import omr.glyph.Glyph;
import omr.glyph.Glyphs;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Class {@code DeleteTask} deletes a set of (virtual) glyphs from the sheet environment
 *
 * @author Hervé Bitteur
 */
public class DeleteTask
        extends GlyphTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Locations */
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "point")
    private List<Point> locations;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an glyph deletion task
     *
     * @param sheet  the sheet impacted
     * @param glyphs the (virtual) glyphs to delete
     * @throws IllegalArgumentException if any of the arguments is not valid
     */
    public DeleteTask (Sheet sheet,
                       Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);

        locations = new ArrayList<Point>();

        for (Glyph glyph : glyphs) {
            locations.add(glyph.getCenter());
        }
    }

    /** No-arg constructor needed for JAXB. */
    private DeleteTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController().syncDelete(this);
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        super.epilog(sheet);
        logger.info("Deletion of virtual {}", Glyphs.ids(glyphs));
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" delete");

        if (!locations.isEmpty()) {
            sb.append(" locations[");

            for (Point point : locations) {
                sb.append(" ").append(point.toString());
            }

            sb.append("]");
        } else {
            sb.append(" no-locations");
        }

        return sb.toString();
    }

    //-----------------------//
    // retrieveCurrentImpact //
    //-----------------------//
    @Override
    protected SortedSet<SystemInfo> retrieveCurrentImpact (Sheet sheet)
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();
        logger.error("Not yet implemented");

        //        for (Glyph glyph : glyphs) {
        //            Point location = glyph.getCenter();
        //            SystemInfo system = sheet.getSystemOf(location);
        //
        //            if (system != null) {
        //                // Include this system
        //                impactedSystems.add(system);
        //            }
        //
        //            if (glyph.getShape().isPersistent()) {
        //                // Include all following systems as well
        //                impactedSystems.addAll(remaining(system));
        //            }
        //        }
        return impactedSystems;
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * Here, we have to retrieve virtual glyphs, based on their locations.
     */
    @Override
    protected void retrieveGlyphs ()
    {
        glyphs = new LinkedHashSet<Glyph>();

        for (Point location : locations) {
            Glyph glyph = sheet.getGlyphIndex().lookupVirtualGlyph(location);
            glyphs.add(glyph);
            logger.debug("To be deleted: {}", glyph);
        }
    }
}
