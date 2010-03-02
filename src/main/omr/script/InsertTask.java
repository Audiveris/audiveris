//----------------------------------------------------------------------------//
//                                                                            //
//                            I n s e r t T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.GlyphLag;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.VirtualGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.LagOrientation;

import omr.score.common.PixelPoint;

import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.PointFacade;

import java.util.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class {@code InsertTask} inserts a set of (virtual) glyphs into the sheet
 * environment
 *
 * @author Hervé Bitteur
 */
public class InsertTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Shape of the inserted glyphs */
    @XmlAttribute
    private final Shape shape;

    /** Locations */
    private List<PixelPoint> locations;

    /** Wrapping of the collections of points */
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "point")
    private PointFacade[] points;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // InsertTask //
    //------------//
    /**
     * Create an glyph insertion task
     *
     * @param shape the inserted shape
     * @param locations the locations for insertion
     * @param orientation the orientation of the containing lag
     * @throws IllegalArgumentException if any of the arguments is not valid
     */
    public InsertTask (Shape                  shape,
                       Collection<PixelPoint> locations,
                       LagOrientation         orientation)
    {
        super(orientation);

        // Check parameters
        if (shape == null) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " needs a non-null shape");
        }

        if ((locations == null) || locations.isEmpty()) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " needs at least one location");
        }

        this.shape = shape;
        this.locations = new ArrayList<PixelPoint>(locations);
    }

    //------------//
    // InsertTask //
    //------------//
    /** No-arg constructor needed for JAXB */
    private InsertTask ()
    {
        shape = null; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getInsertedShape //
    //------------------//
    /**
     * Report the inserted shape
     * @return the insertedShape
     */
    public Shape getInsertedShape ()
    {
        return shape;
    }

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        // Nothing to do
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        super.epilog(sheet);

        if (logger.isFineEnabled()) {
            logger.fine(toString());
        }

        logger.info(
            "Insertion of virtual " + shape + " " + Glyphs.toString(glyphs));

        // Take inserted glyph(s) as selected glyph(s)
        GlyphLag lag = (orientation == LagOrientation.VERTICAL)
                       ? sheet.getVerticalLag() : sheet.getHorizontalLag();
        lag.getSelectionService()
           .publish(
            new GlyphSetEvent(
                this,
                SelectionHint.GLYPH_INIT,
                MouseMovement.PRESSING,
                glyphs));
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" insert");

        sb.append(" ")
          .append(orientation);

        sb.append(" ")
          .append(shape);

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

        return sb + super.internalsString();
    }

    //-----------------------//
    // retrieveCurrentImpact //
    //-----------------------//
    @Override
    protected SortedSet<SystemInfo> retrieveCurrentImpact (Sheet sheet)
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();

        for (PixelPoint location : locations) {
            SystemInfo system = sheet.getSystemOf(location);

            if (system != null) {
                // Include this system
                impactedSystems.add(system);
            }

            if (shape.isPersistent()) {
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
     * Here, we have to build virtual glyphs, based on the desired shape and the
     * locations.
     */
    @Override
    protected void retrieveGlyphs ()
    {
        glyphs = new LinkedHashSet<Glyph>();

        for (PixelPoint location : locations) {
            Glyph      glyph = new VirtualGlyph(shape);
            PixelPoint center = glyph.getAreaCenter();
            glyph.translate(new PixelPoint(center.to(location)));

            if (orientation == LagOrientation.VERTICAL) {
                SystemInfo system = sheet.getSystemOf(center);
                glyph = system.addGlyph(glyph);
                system.computeGlyphFeatures(glyph);
            } else {
                sheet.getHorizontalLag()
                     .addGlyph(glyph);
            }

            glyphs.add(glyph);
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
