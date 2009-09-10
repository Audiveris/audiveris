//----------------------------------------------------------------------------//
//                                                                            //
//                           S e c t i o n S e t s                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.lag.Sections;

import omr.log.Logger;

import omr.sheet.Sheet;

import java.util.*;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.*;

/**
 * Class <code>SectionSets</code> handles a  collection of section sets,
 * with the ability to (un)marshall its content using the sections ids.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SectionSets
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SectionSets.class);

    //~ Instance fields --------------------------------------------------------

    /** The collection of sections sets */
    protected Collection<Collection<GlyphSection>> sets;

    /** The collection of sets of section ids */
    @XmlElement(name = "sections")
    private Collection<SectionIdSet> idSets;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SectionSets //
    //-------------//
    /**
     * Creates a new SectionSets object.
     *
     * @param sets The collection of collections of sections
     */
    public SectionSets (Collection<Collection<GlyphSection>> sets)
    {
        this.sets = sets;
    }

    //-------------//
    // SectionSets // No-arg constructor needed by JAXB
    //-------------//
    private SectionSets ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // createFromGlyphs //
    //------------------//
    /**
     * Convenient method to create the proper SectionSets out of a provided
     * collection of glyphs
     * @param glyphs the provided glyphs
     * @return a newly built SectionSets instance
     */
    public static SectionSets createFromGlyphs (Collection<Glyph> glyphs)
    {
        SectionSets sectionSets = new SectionSets();
        sectionSets.sets = new ArrayList<Collection<GlyphSection>>();

        for (Glyph glyph : glyphs) {
            sectionSets.sets.add(
                new ArrayList<GlyphSection>(glyph.getMembers()));
        }

        return sectionSets;
    }

    //--------------------//
    // createFromSections //
    //--------------------//
    /**
     * Convenient method to create the proper SectionSets out of a provided
     * collection of sections
     * @param sections the provided sections
     * @return a newly built SectionSets instance (a singleton actually)
     */
    public static SectionSets createFromSections (Collection<GlyphSection> sections)
    {
        SectionSets sectionSets = new SectionSets();
        sectionSets.sets = new ArrayList<Collection<GlyphSection>>();
        sectionSets.sets.add(sections);

        return sectionSets;
    }

    //---------//
    // getSets //
    //---------//
    /**
     * Report the collection of section sets
     * @param sheet the containing sheet (needed to get sections from their id)
     * @return the collection of section sets
     */
    public Collection<Collection<GlyphSection>> getSets (Sheet sheet)
    {
        if (sets == null) {
            sets = new ArrayList<Collection<GlyphSection>>();

            for (SectionIdSet idSet : idSets) {
                List<GlyphSection> sectionSet = new ArrayList<GlyphSection>();

                for (int id : idSet.ids) {
                    GlyphSection section = sheet.getVerticalLag()
                                                .getVertexById(id);

                    if (section == null) {
                        logger.warning(
                            "Cannot find section for " + id,
                            new Throwable());
                    } else {
                        sectionSet.add(section);
                    }
                }

                sets.add(sectionSet);
            }
        }

        return sets;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        if (sets != null) {
            StringBuilder sb = new StringBuilder();

            for (Collection<GlyphSection> set : sets) {
                // Separator needed?
                if (sb.length() > 0) {
                    sb.append(" ");
                }

                sb.append(Sections.toString(set));
            }

            return sb.toString();
        }

        return "";
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        // Convert sections -> ids
        if (sets != null) {
            idSets = new ArrayList<SectionIdSet>();

            for (Collection<GlyphSection> set : sets) {
                SectionIdSet idSet = new SectionIdSet();

                for (GlyphSection section : set) {
                    idSet.ids.add(section.getId());
                }

                idSets.add(idSet);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // SectionIdSet //
    //--------------//
    /**
     * Handles one collection of section ids.
     * The only purpose of this class (vs the direct use of List<Integer>) is
     * the ability to add annotations meant for JAXB
     */
    private static class SectionIdSet
    {
        //~ Instance fields ----------------------------------------------------

        // Annotation to get all ids, space-separated, in one single element:
        @XmlList
        // Annotation to avoid any wrapper:
        @XmlValue
        private Collection<Integer> ids = new ArrayList<Integer>();
    }
}
