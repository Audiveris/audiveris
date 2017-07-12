//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e c t i o n G r a p h                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.glyph.dynamic.SectionGraph.Link;
import org.audiveris.omr.lag.Section;

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
