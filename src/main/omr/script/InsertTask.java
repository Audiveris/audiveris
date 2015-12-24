//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n s e r t T a s k                                       //
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
import omr.glyph.Shape;

import omr.selection.EntityListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Class {@code InsertTask} inserts a set of (virtual) glyphs into the sheet environment.
 *
 * @author Hervé Bitteur
 */
public class InsertTask
        extends GlyphTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Shape of the inserted glyphs */
    @XmlAttribute
    private final Shape shape;

    /** Locations */
    @XmlElementWrapper(name = "locations")
    @XmlElement(name = "point")
    private List<Point> locations;

    //~ Constructors -------------------------------------------------------------------------------
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
        this.locations = new ArrayList<Point>(locations);
    }

    /** No-arg constructor for JAXB only. */
    private InsertTask ()
    {
        shape = null; // Dummy value
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        sheet.getGlyphIndex().getEntityService().publish(
                new EntityListEvent<Glyph>(
                        this,
                        SelectionHint.ENTITY_INIT,
                        MouseMovement.PRESSING,
                        new ArrayList<Glyph>(glyphs)));
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
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
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();

        logger.error("Not yet implemented");

        //        for (Point location : locations) {
        //            SystemInfo system = sheet.getSystemOf(location);
        //
        //            if (system != null) {
        //                // Include this system
        //                impactedSystems.add(system);
        //            }
        //
        //            if (shape.isPersistent()) {
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
     * Here, we have to build virtual glyphs, based on the desired
     * shape and the locations.
     */
    @Override
    protected void retrieveGlyphs ()
    {
        logger.error("Not yet implemented");

        //        glyphs = new LinkedHashSet<>();
        //        SystemManager systemManager = sheet.getSystemManager();
        //
        //        for (Point location : locations) {
        //            Glyph glyph = new VirtualGlyph(
        //                    shape,
        //                    sheet.getScale().getInterline(),
        //                    location);
        //
        //            // TODO: Some location other than the areacenter may be desired
        //            // (depending on the shape)?
        //            for (SystemInfo system : systemManager.getSystemsOf(location)) {
        //                glyph = system.register(glyph);
        //
        //                // Specific for LEDGERs: add them to related staff
        //                if (shape == Shape.LEDGER) {
        //                    StaffInfo staff = system.getStaffAt(glyph.getAreaCenter());
        //                //TODO: insert a LedgerInter!
        //                    ///staff.addLedger(glyph);
        //                }
        //            }
        //
        //            glyphs.add(glyph);
        //        }
    }
}
