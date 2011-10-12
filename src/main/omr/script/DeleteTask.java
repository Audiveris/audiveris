//----------------------------------------------------------------------------//
//                                                                            //
//                            D e l e t e T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.run.Orientation;

import omr.score.common.PixelPoint;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.PointFacade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Class {@code DeleteTask} deletes a set of (virtual) glyphs from the sheet
 * environment
 *
 * @author Hervé Bitteur
 */
public class DeleteTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Locations */
    private List<PixelPoint> locations;

    /** Wrapping of the collections of points */
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "point")
    private PointFacade[] points;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // DeleteTask //
    //------------//
    /**
     * Create an glyph deletion task
     *
     * @param sheet the sheet impacted
     * @param glyphs the (virtual) glyphs to delete
     * @throws IllegalArgumentException if any of the arguments is not valid
     */
    public DeleteTask (Sheet             sheet,
                       Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);

        locations = new ArrayList<PixelPoint>();

        for (Glyph glyph : glyphs) {
            locations.add(glyph.getAreaCenter());
        }
    }

    //------------//
    // DeleteTask //
    //------------//
    /** No-arg constructor needed for JAXB */
    private DeleteTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        //        switch (orientation) {
        //        case HORIZONTAL :
        //            sheet.getHorizontalsBuilder()
        //                 .getController()
        //                 .syncDelete(this);
        //
        //            break;
        //
        //        case VERTICAL :
        sheet.getSymbolsController()
             .syncDelete(this);

        //        }
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        super.epilog(sheet);
        logger.info("Deletion of virtual " + Glyphs.toString(glyphs));
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" delete");

        if (!locations.isEmpty()) {
            sb.append(" locations[");

            for (PixelPoint point : locations) {
                sb.append(" ")
                  .append(point.toString());
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

        for (Glyph glyph : glyphs) {
            PixelPoint location = glyph.getAreaCenter();
            SystemInfo system = sheet.getSystemOf(location);

            if (system != null) {
                // Include this system
                impactedSystems.add(system);
            }

            if (glyph.getShape()
                     .isPersistent()) {
                // Include all following systems as well
                impactedSystems.addAll(remaining(system));
            }
        }

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

        for (PixelPoint location : locations) {
            Glyph glyph = null;
            glyph = sheet.getNest()
                         .lookupVirtualGlyph(location);
            glyphs.add(glyph);

            if (logger.isFineEnabled()) {
                logger.fine("To be deleted: " + glyph);
            }
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this
     * object, but before this object is set to the parent object.
     */
    private void afterUnmarshal (Unmarshaller um,
                                 Object       parent)
    {
        // Convert array of point facades -> locations
        if (locations == null) {
            locations = new ArrayList<PixelPoint>();

            for (PointFacade facade : points) {
                locations.add(new PixelPoint(facade.getX(), facade.getY()));
            }
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    private void beforeMarshal (Marshaller m)
    {
        // Convert locations -> array of point facades
        if (points == null) {
            List<PointFacade> facades = new ArrayList<PointFacade>();

            for (PixelPoint point : locations) {
                facades.add(new PointFacade(point));
            }

            points = facades.toArray(new PointFacade[0]);
        }
    }
}
