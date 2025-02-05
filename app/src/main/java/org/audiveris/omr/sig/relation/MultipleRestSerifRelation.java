//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                        M u l t i p l e R e s t S e r i f R e l a t i o n                       //
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

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.MultipleRestInter;
import org.audiveris.omr.sig.inter.VerticalSerifInter;
import org.audiveris.omr.util.HorizontalSide;

import org.jgrapht.event.GraphEdgeChangeEvent;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MultipleRestSerifRelation</code> implements the geometrical link between
 * a multiple rest and one of its side serifs.
 *
 * @see MultipleRestInter
 * @see VerticalSerifInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "multiple-rest-serif")
public class MultipleRestSerifRelation
        extends AbstractStemConnection
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The rest-side attribute indicates on which side of the multiple rest
     * the serif is connected.
     */
    @XmlAttribute(name = "rest-side")
    private HorizontalSide restSide;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>MultipleRestSerifRelation</code> object.
     */
    public MultipleRestSerifRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final MultipleRestInter multipleRest = (MultipleRestInter) e.getEdgeSource();
        multipleRest.checkAbnormal();

        final VerticalSerifInter serif = (VerticalSerifInter) e.getEdgeTarget();
        serif.checkAbnormal();
    }

    //-------------//
    // getRestSide //
    //-------------//
    /**
     * @return the rest side
     */
    public HorizontalSide getRestSide ()
    {
        return restSide;
    }

    //----------------//
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        return StemPortion.STEM_MIDDLE;
    }

    @Override
    protected Scale.Fraction getXOutGapMax (int profile)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Scale.Fraction getYGapMax (int profile)
    {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return false;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final MultipleRestInter multipleRest = (MultipleRestInter) e.getEdgeSource();
        if (!multipleRest.isRemoved()) {
            multipleRest.checkAbnormal();
        }

        final VerticalSerifInter serif = (VerticalSerifInter) e.getEdgeTarget();
        if (!serif.isRemoved()) {
            serif.checkAbnormal();
        }
    }

    //-------------//
    // setRestSide //
    //-------------//
    /**
     * Set the rest side where serif is connected.
     *
     * @param restSide the rest side to set
     */
    public void setRestSide (HorizontalSide restSide)
    {
        this.restSide = restSide;
    }
}
