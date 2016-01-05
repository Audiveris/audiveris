//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  C o m p o u n d F a c t o r y                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.lag.Section;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class {@code CompoundFactory} build compounds out of sections or other compounds.
 *
 * @author Hervé Bitteur
 */
public class CompoundFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CompoundFactory.class);

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // buildCompound //
    //---------------//
    public static SectionCompound buildCompound (Collection<Section> sections,
                                                 Class<? extends SectionCompound> compoundClass,
                                                 int interline)
    {
        if ((sections == null) || sections.isEmpty()) {
            throw new IllegalArgumentException("Building a SectionCompound out of no sections");
        }

        final SectionCompound compound = createInstance(compoundClass, interline);

        for (Section section : sections) {
            compound.addSection(section, false);
        }

        return compound;
    }

    //------------------------//
    // buildCompoundFromParts //
    //------------------------//
    public static SectionCompound buildCompoundFromParts (
            Collection<? extends SectionCompound> parts,
            final Class<? extends SectionCompound> compoundClass,
            int interline)
    {
        if ((parts == null) || parts.isEmpty()) {
            throw new IllegalArgumentException("Building a SectionCompound out of no parts");
        }

        List<Section> sections = new ArrayList<Section>();

        for (SectionCompound part : parts) {
            sections.addAll(part.getMembers());
        }

        return buildCompound(sections, compoundClass, interline);
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
     * @param sections      the sections to browse
     * @param interline     scaling info for created compounds
     * @param compoundClass specific class for created compounds
     * @return the list of compound instances created
     */
    public static List<SectionCompound> buildCompounds (Collection<Section> sections,
                                                        int interline,
                                                        Class<? extends SectionCompound> compoundClass)
    {
        // Build a temporary graph of all sections with "touching" relations
        List<Section> list = new ArrayList<Section>(sections);

        ///Collections.sort(list, Section.byAbscissa);
        SimpleGraph<Section, Touching> graph = new SimpleGraph<Section, Touching>(Touching.class);

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
        ConnectivityInspector inspector = new ConnectivityInspector(graph);
        List<Set<Section>> sets = inspector.connectedSets();
        logger.debug("sets: {}", sets.size());

        List<SectionCompound> compounds = new ArrayList<SectionCompound>();

        for (Set<Section> set : sets) {
            compounds.add(buildCompound(set, compoundClass, interline));
        }

        return compounds;
    }

    //----------------//
    // createInstance //
    //----------------//
    private static SectionCompound createInstance (Class<? extends SectionCompound> compoundClass,
                                                   int interline)
    {
        try {
            final Constructor<?> constructor = compoundClass.getConstructor(int.class);

            return (SectionCompound) constructor.newInstance(new Object[]{interline});
        } catch (Exception ex) {
            logger.error(null, ex);

            throw new RuntimeException(ex);
        }
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
