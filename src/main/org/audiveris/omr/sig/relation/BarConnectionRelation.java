//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            B a r C o n n e c t i o n R e l a t i o n                           //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sig.GradeImpacts;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarConnectionRelation} records the relation between two bar lines
 * connected across staves, so that they can support each other.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bar-connection")
public class BarConnectionRelation
        extends Support
{

    private static final Constants constants = new Constants();

    /**
     * Creates a new BarConnectionRelation object.
     *
     * @param impacts quality of relation
     */
    public BarConnectionRelation (GradeImpacts impacts)
    {
        super(impacts.getGrade());
        setImpacts(impacts);
    }

    // For JAXB
    private BarConnectionRelation ()
    {
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

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.barSupportCoeff.getValue();
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.barSupportCoeff.getValue();
    }

    @Override
    public Object clone ()
            throws CloneNotSupportedException
    {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio barSupportCoeff = new Constant.Ratio(
                5,
                "Value for source/target (bar) coeff in support formula");
    }
}
