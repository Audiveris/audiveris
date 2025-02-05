//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                        M u l t i p l e R e s t C o u n t R e l a t i o n                       //
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
import org.audiveris.omr.sig.inter.MultipleRestInter;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MultipleRestCountRelation</code> implements the link between
 * a multiple rest and the count of measures.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "multiple-rest-count")
public class MultipleRestCountRelation
        extends Support
{
    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final MultipleRestInter rest = (MultipleRestInter) e.getEdgeSource();
        rest.checkAbnormal();

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
        final MultipleRestInter rest = (MultipleRestInter) e.getEdgeSource();

        if (!rest.isRemoved()) {
            rest.checkAbnormal();
        }

        final MeasureCountInter count = (MeasureCountInter) e.getEdgeTarget();

        if (!count.isRemoved()) {
            count.checkAbnormal();
        }
    }
}
