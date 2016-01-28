//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e c t i o n G r a p h                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.dynamic;

import omr.glyph.dynamic.SectionGraph.Link;

import omr.lag.Section;

import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Class {@code SectionGraph} implements adjacency between sections.
 *
 * @author Hervé Bitteur
 */
public class SectionGraph
        extends SimpleDirectedGraph<Section, Link>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SectionGraph} object.
     */
    public SectionGraph ()
    {
        super(Link.class);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public static class Link
    {
    }
}
