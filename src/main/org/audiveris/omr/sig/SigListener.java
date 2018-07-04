//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S i g L i s t e n e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig;

import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;

import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code SigListener} listens for SIG modifications.
 *
 * @author Hervé Bitteur
 */
public class SigListener
        implements GraphListener<Inter, Relation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SigListener.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SigListener} object.
     */
    public SigListener ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void edgeAdded (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // logger.info(
        //         "GRAPH edgeAdded {} src:{} tgt:{}",
        //         e.getEdge(),
        //         e.getEdgeSource(),
        //         e.getEdgeTarget());
        //
        e.getEdge().added(e);
    }

    @Override
    public void edgeRemoved (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // logger.info(
        //         "GRAPH edgeRemoved {} src:{} tgt:{}",
        //         e.getEdge(),
        //         e.getEdgeSource(),
        //         e.getEdgeTarget());
        //
        e.getEdge().removed(e);
    }

    @Override
    public void vertexAdded (GraphVertexChangeEvent<Inter> e)
    {
        //        logger.info(
        //                "GRAPH vertexAdded {} source:{} type:{}",
        //                e.getVertex(),
        //                e.getSource(),
        //                e.getType());
        //
        //        if (e.getVertex().isVip()) {
        //            logger.info(
        //                    "VIP GRAPH vertexAdded {} source:{} type:{}",
        //                    e.getVertex(),
        //                    e.getSource(),
        //                    e.getType());
        //        }
    }

    @Override
    public void vertexRemoved (GraphVertexChangeEvent<Inter> e)
    {
        //        logger.info(
        //                "GRAPH vertexRemoved {} source:{} type:{}",
        //                e.getVertex(),
        //                e.getSource(),
        //                e.getType());
        //
        //        if (e.getVertex().isVip()) {
        //            logger.info(
        //                    "VIP GRAPH vertexRemoved {} source:{} type:{}",
        //                    e.getVertex(),
        //                    e.getSource(),
        //                    e.getType());
        //        }
    }
}
