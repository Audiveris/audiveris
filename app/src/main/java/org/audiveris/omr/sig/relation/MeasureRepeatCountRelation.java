//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                       M e a s u r e R e p e a t C o u n t R e l a t i o n                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.MeasureCountInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MeasureRepeatCountRelation</code> implements the link between
 * a measure repeat sign and the count of measures.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "measure-repeat-count")
public class MeasureRepeatCountRelation
        extends Support
{
    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // A measure repeat sign does not really require a measure count
        // since the sign itself already has a number of slashes (1, 2 or 4)
        /// final MeasureRepeatInter sign = (MeasureRepeatInter) e.getEdgeSource();
        /// sign.checkAbnormal();

        final MeasureCountInter count = (MeasureCountInter) e.getEdgeTarget();
        count.checkAbnormal();
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return true;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        //        final MeasureRepeatInter sign = (MeasureRepeatInter) e.getEdgeSource();
        //
        //        if (!sign.isRemoved()) {
        //            sign.checkAbnormal();
        //        }

        final MeasureCountInter count = (MeasureCountInter) e.getEdgeTarget();

        if (!count.isRemoved()) {
            count.checkAbnormal();
        }
    }
}
