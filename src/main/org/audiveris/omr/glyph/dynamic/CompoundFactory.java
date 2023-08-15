//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  C o m p o u n d F a c t o r y                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.lag.Section;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class <code>CompoundFactory</code> build compounds out of sections or other compounds.
 *
 * @author Hervé Bitteur
 */
public class CompoundFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CompoundFactory.class);

    //~ Constructors -------------------------------------------------------------------------------

    private CompoundFactory ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------//
    // buildCompound //
    //---------------//
    /**
     * Build a compound from provided sections.
     *
     * @param sections    the provided sections
     * @param constructor specific compound constructor
     * @return the created compound
     */
    public static SectionCompound buildCompound (Collection<Section> sections,
                                                 CompoundConstructor constructor)
    {
        if ((sections == null) || sections.isEmpty()) {
            throw new IllegalArgumentException("Building a SectionCompound out of no sections");
        }

        final SectionCompound compound = constructor.newInstance();

        for (Section section : sections) {
            compound.addSection(section);
        }

        return compound;
    }

    //------------------------//
    // buildCompoundFromParts //
    //------------------------//
    /**
     * Build a compound from other compounds.
     *
     * @param parts       the other compounds
     * @param constructor specific compound constructor
     * @return the created compound
     */
    public static SectionCompound buildCompoundFromParts (
                                                          Collection<? extends SectionCompound> parts,
                                                          CompoundConstructor constructor)
    {
        if ((parts == null) || parts.isEmpty()) {
            throw new IllegalArgumentException("Building a SectionCompound out of no parts");
        }

        List<Section> sections = new ArrayList<>();

        for (SectionCompound part : parts) {
            sections.addAll(part.getMembers());
        }

        return buildCompound(sections, constructor);
    }

    //----------------//
    // buildCompounds //
    //----------------//
    /**
     * Browse through the provided isolated sections and return a list of compound
     * instances, one for each set of connected sections.
     * <p>
     * This method does not use the sections neighboring links, it is thus rather slow but usable
     * when these links are not yet set.
     *
     * @param sections    the sections to browse
     * @param constructor specific compound constructor
     * @return the list of compound instances created
     */
    public static List<SectionCompound> buildCompounds (Collection<Section> sections,
                                                        CompoundConstructor constructor)
    {
        // Build a temporary graph of all sections with "touching" relations
        List<Section> list = new ArrayList<>(sections);

        ///Collections.sort(list, Section.byAbscissa);
        SimpleGraph<Section, Touching> graph = new SimpleGraph<>(Touching.class);

        // Populate graph with all sections as vertices
        for (Section section : list) {
            graph.addVertex(section);
        }

        // Populate graph with relations
        for (int i = 0; i < list.size(); i++) {
            Section one = list.get(i);

            for (Section two : list.subList(i + 1, list.size())) {
                if (one.touches(two)) {
                    graph.addEdge(one, two, new Touching());
                }
            }
        }

        // Retrieve all the clusters of sections (sets of touching sections)
        ConnectivityInspector<Section, Touching> inspector = new ConnectivityInspector<>(graph);
        List<Set<Section>> sets = inspector.connectedSets();
        logger.debug("sets: {}", sets.size());

        List<SectionCompound> compounds = new ArrayList<>();

        for (Set<Section> set : sets) {
            compounds.add(buildCompound(set, constructor));
        }

        return compounds;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------

    //---------------------//
    // CompoundConstructor //
    //---------------------//
    /**
     * Actual constructor for compounds built by this factory.
     */
    public interface CompoundConstructor
    {

        /**
         * Actual creation of compound instance.
         *
         * @return the created compound
         */
        SectionCompound newInstance ();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // Touching //
    //----------//
    /**
     * Represents a "touching" relationship between two sections.
     */
    private static class Touching
    {
    }
}
