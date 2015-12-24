//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      D e l e t e T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
