//----------------------------------------------------------------------------//
//                                                                            //
//                                I m p a c t                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.*;

/**
 * Class <code>Impact</code> allows to handle the impact of a action on glyphs
 * (or sections) and to determine which systems will need to be rebuilt.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Impact
{
    //~ Instance fields --------------------------------------------------------

    /** The related sheet */
    private final Sheet sheet;

    /** The assigned shape (or null for a deassignment) */
    private final Shape assignedShape;

    /** The impacted glyphs */
    private Set<Glyph> glyphs;

    /** The impacted vertical section sets */
    private final SectionSets sectionSets;

    /** The set of (pre) impacted systems, using status before action */
    private final SortedSet<SystemInfo> initialSystems;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Impact //
    //--------//
    /**
     * Creates a new Impact object from a collection of section sets
     *
     * @param shape the assigned shape (or null for a de-assignment)
     * @param sectionSets the collection of impacted section sets
     */
    private Impact (Sheet       sheet,
                    Shape       shape,
                    SectionSets sectionSets)
    {
        this.sheet = sheet;
        this.assignedShape = shape;
        this.sectionSets = sectionSets;

        // Remember the systems initially impacted
        initialSystems = retrieveCurrentImpact();
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getAssignedShape //
    //------------------//
    /**
     * Report the assigned shape (for an assignment impact)
     * @return the assignedShape (null for a deasssignment)
     */
    public Shape getAssignedShape ()
    {
        return assignedShape;
    }

    //--------------------//
    // getImpactedSystems //
    //--------------------//
    /**
     * Report the set of systems that are impacted by the action
     * @return the ordered set of impacted systems
     */
    public SortedSet<SystemInfo> getImpactedSystems ()
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();
        impactedSystems.addAll(initialSystems);
        impactedSystems.addAll(retrieveCurrentImpact());

        return impactedSystems;
    }

    //-------------------//
    // createDummyImpact // for Boundary
    //-------------------//
    /**
     * Creates a dummy Impact
     * @param sheet the related sheet
     * @return the created Impact instance
     */
    public static Impact createDummyImpact (Sheet sheet)
    {
        return new Impact(sheet, null, null);
    }

    //--------------------//
    // createGlyphsImpact // for assign, deassign, text, slur, segment
    //--------------------//
    /**
     * Creates an Impact from a  collection of glyphs
     * @param sheet the related sheet
     * @param shape the assigned shape (or null for a de-assignment)
     * @param glyphs the collection of impacted glyphs
     * @return the created Impact instance
     */
    public static Impact createGlyphsImpact (Sheet             sheet,
                                             Shape             shape,
                                             Collection<Glyph> glyphs)
    {
        Impact impact = new Impact(
            sheet,
            shape,
            SectionSets.createFromGlyphs(glyphs));

        impact.glyphs = Glyphs.sortedSet(glyphs);

        return impact;
    }

    //------------------//
    // getInitialGlyphs //
    //------------------//
    /**
     * Report the collection of initial glyphs
     * @return the impactedGlyphs
     */
    public Set<Glyph> getInitialGlyphs ()
    {
        return glyphs;
    }

    //----------------//
    // getSectionSets //
    //----------------//
    public Collection<Collection<GlyphSection>> getSectionSets ()
    {
        return sectionSets.getSets(sheet);
    }

    //------------------//
    // impactAllSystems //
    //------------------//
    /**
     * Extends the range of the impacted systems from the first known impacted
     * until the end of the systems
     */
    public void impactAllSystems ()
    {
        initialSystems.addAll(remaining(initialSystems.first()));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Impact");

        if (assignedShape != null) {
            sb.append(" ")
              .append(assignedShape);
        }

        if (glyphs != null) {
            sb.append(" ")
              .append(Glyphs.toString(glyphs));
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------------------//
    // retrieveCurrentImpact //
    //-----------------------//
    /**
     * Report the set of systems that are impacted by the action, as determined
     * by the *current status* of the glyphs *currently* pointed by the sections
     * @return the ordered set of impacted systems
     */
    protected SortedSet<SystemInfo> retrieveCurrentImpact ()
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();
        List<SystemInfo>      all = sheet.getSystems();

        for (Collection<GlyphSection> set : sectionSets.getSets(sheet)) {
            for (GlyphSection section : set) {
                SystemInfo system = section.getSystem();
                Glyph      glyph = section.getGlyph();

                if (system != null) {
                    impactedSystems.add(system);
                }

                if (glyph != null) {
                    Shape shape = glyph.getShape();

                    if ((shape != null) && shape.isPersistent()) {
                        // Include all following systems
                        impactedSystems.addAll(remaining(system));
                    }
                }
            }
        }

        return impactedSystems;
    }

    //-----------//
    // remaining //
    //-----------//
    /**
     * Report the collection of systems that follow the provided one
     * @param system the one after which we pick any system
     * @return the remaining portion of the sequence of systems in the sheet
     */
    private Collection<SystemInfo> remaining (SystemInfo system)
    {
        List<SystemInfo> all = sheet.getSystems();

        return all.subList(all.indexOf(system) + 1, all.size());
    }
}
