//----------------------------------------------------------------------------//
//                                                                            //
//                            I n s e r t T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Shape;
import omr.glyph.VirtualGlyph;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.PointFacade;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Class {@code InsertTask} inserts a set of (virtual) glyphs into the
 * sheet environment.
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
    private List<Point> locations;

    /** Wrapping of the collections of points */
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "point")
    private PointFacade[] points;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // InsertTask //
    //------------//
    /**
     * Create an glyph insertion task.
     *
     * @param sheet     the sheet impacted
     * @param shape     the inserted shape
     * @param locations the locations for insertion
     * @throws IllegalArgumentException if any of the arguments is not valid
     */
    public InsertTask (Sheet sheet,
                       Shape shape,
                       Collection<Point> locations)
    {
        super(sheet);

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
        this.locations = new ArrayList<>(locations);
    }

    //------------//
    // InsertTask //
    //------------//
    /** No-arg constructor for JAXB only */
    private InsertTask ()
    {
        shape = null; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------
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

        logger.debug("{}", this);

        // Take inserted glyph(s) as selected glyph(s)
        sheet.getNest().getGlyphService().publish(
                new GlyphSetEvent(
                this,
                SelectionHint.GLYPH_INIT,
                MouseMovement.PRESSING,
                glyphs));
    }

    //------------------//
    // getInsertedShape //
    //------------------//
    /**
     * Report the inserted shape.
     *
     * @return the insertedShape
     */
    public Shape getInsertedShape ()
    {
        return shape;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" insert");

        sb.append(" ").append(shape);

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
        SortedSet<SystemInfo> impactedSystems = new TreeSet<>();

        for (Point location : locations) {
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
     * Here, we have to build virtual glyphs, based on the desired
     * shape and the locations.
     */
    @Override
    protected void retrieveGlyphs ()
    {
        glyphs = new LinkedHashSet<>();

        for (Point location : locations) {
            Glyph glyph = new VirtualGlyph(
                    shape,
                    sheet.getScale().getInterline(),
                    location);

            // TODO: Some location other than the areacenter may be desired
            // (depending on the shape)?
            SystemInfo system = sheet.getSystemOf(glyph.getAreaCenter());
            glyph = system.addGlyph(glyph);
            system.computeGlyphFeatures(glyph);

            // Specific for LEDGERs: add them to related staff
            if (shape == Shape.LEDGER) {
                StaffInfo staff = system.getStaffAt(glyph.getAreaCenter());
                staff.addLedger(glyph);
            }

            glyphs.add(glyph);
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     */
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        // Convert array of point facades -> locations
        if (locations == null) {
            locations = new ArrayList<>();

            for (PointFacade facade : points) {
                locations.add(new Point(facade.getX(), facade.getY()));
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
            List<PointFacade> facades = new ArrayList<>();

            for (Point point : locations) {
                facades.add(new PointFacade(point));
            }

            points = facades.toArray(new PointFacade[0]);
        }
    }
}
