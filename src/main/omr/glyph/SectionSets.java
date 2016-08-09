//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e c t i o n S e t s                                      //
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
package omr.glyph;

import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;

import omr.run.Orientation;

import omr.sheet.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import omr.util.Entities;

/**
 * Class {@code SectionSets} handles a collection of section sets,
 * with the ability to (un)marshal its content using the sections ids.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SectionSets
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SectionSets.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The collection of sections sets. */
    protected Collection<Collection<Section>> sets;

    /** The collection of sets (=glyphs) of section descriptors. */
    @XmlElement(name = "sections")
    private Collection<SectionDescSet> descSets;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SectionSets object.
     *
     * @param sets The collection of collections of sections
     */
    public SectionSets (Collection<Collection<Section>> sets)
    {
        this.sets = sets;
    }

    //-------------//
    // SectionSets // No-arg constructor needed by JAXB
    //-------------//
    private SectionSets ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // createFromGlyphs //
    //------------------//
    /**
     * Convenient method to create the proper SectionSets out of a provided
     * collection of glyphs
     *
     * @param glyphs the provided glyphs
     * @return a newly built SectionSets instance
     */
    public static SectionSets createFromGlyphs (Collection<Glyph> glyphs)
    {
        throw new RuntimeException("HB. Not implemented yet");
//        SectionSets sectionSets = new SectionSets();
//        sectionSets.sets = new ArrayList<Collection<Section>>();
//
//        for (Glyph glyph : glyphs) {
//            sectionSets.sets.add(new ArrayList<Section>(glyph.getMembers()));
//        }
//
//        return sectionSets;
    }

    //--------------------//
    // createFromSections //
    //--------------------//
    /**
     * Convenient method to create the proper SectionSets out of a provided
     * collection of sections
     *
     * @param sections the provided sections
     * @return a newly built SectionSets instance (a singleton actually)
     */
    public static SectionSets createFromSections (Collection<Section> sections)
    {
        SectionSets sectionSets = new SectionSets();
        sectionSets.sets = new ArrayList<Collection<Section>>();
        sectionSets.sets.add(sections);

        return sectionSets;
    }

    //---------//
    // getSets //
    //---------//
    /**
     * Report the collection of section sets
     *
     * @param sheet the containing sheet (needed to get sections from their id)
     * @return the collection of section sets
     */
    public Collection<Collection<Section>> getSets (Sheet sheet)
    {
        if (sets == null) {
            sets = new ArrayList<Collection<Section>>();

            for (SectionDescSet idSet : descSets) {
                List<Section> sectionSet = new ArrayList<Section>();

                for (SectionDesc sectionId : idSet.sections) {
                    Lag lag = sheet.getLagManager().getLag(
                            (sectionId.orientation == Orientation.VERTICAL) ? Lags.VLAG : Lags.HLAG);
                    Section section = lag.getEntity(sectionId.id);

                    if (section == null) {
                        logger.warn("Cannot find section for " + sectionId);
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

            for (Collection<Section> set : sets) {
                // Separator needed?
                if (sb.length() > 0) {
                    sb.append(" ");
                }

                sb.append(Entities.ids(set));
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
            descSets = new ArrayList<SectionDescSet>();

            for (Collection<Section> set : sets) {
                SectionDescSet descSet = new SectionDescSet();

                for (Section section : set) {
                    descSet.sections.add(
                            new SectionDesc(section.getId(), section.getOrientation()));
                }

                descSets.add(descSet);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // SectionDesc //
    //-------------//
    /**
     * Descriptor for one section
     */
    private static class SectionDesc
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Annotation to get all ids, space-separated, in one single element:
        //@XmlList
        // Annotation to avoid any wrapper:
        //@XmlValue
        //private Collection<Integer> ids = new ArrayList<Integer>();
        /** Section id */
        @XmlAttribute(name = "id")
        int id;

        /** Section orientation */
        @XmlAttribute(name = "orientation")
        Orientation orientation;

        //~ Constructors ---------------------------------------------------------------------------
        // For JAXB
        public SectionDesc ()
        {
        }

        public SectionDesc (int id,
                            Orientation orientation)
        {
            this.id = id;
            this.orientation = orientation;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{SectionDesc");
            sb.append(" ").append(orientation);
            sb.append(" ").append(id);
            sb.append("}");

            return super.toString();
        }
    }

    //----------------//
    // SectionDescSet //
    //----------------//
    /**
     * Handles one collection of section ids.
     * The only purpose of this class (vs the direct use of List<Integer>) is
     * the ability to add annotations meant for JAXB
     */
    private static class SectionDescSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        //        // Annotation to get all ids, space-separated, in one single element:
        //        @XmlList
        //        // Annotation to avoid any wrapper:
        //        @XmlValue
        @XmlElement(name = "section")
        private Collection<SectionDesc> sections = new ArrayList<SectionDesc>();
    }
}
