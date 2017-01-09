//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S u p p o r t                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.util.Jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractSupport} is the base implementation of {@link Support} interface.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "support")
public class AbstractSupport
        extends AbstractRelation
        implements Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Quality of the geometric junction. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double3Adapter.class)
    protected double grade;

    /** Details about grade (for debugging). */
    protected GradeImpacts impacts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicSupport object, with a grade set to 1.
     */
    public AbstractSupport ()
    {
        this(1.0);
    }

    /**
     * Creates a new BasicSupport object.
     *
     * @param grade the grade evaluated for this relation
     */
    public AbstractSupport (double grade)
    {
        this.grade = grade;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getSourceRatio //
    //----------------//
    @Override
    public final double getSourceRatio ()
    {
        return 1.0 + (getSourceCoeff() * grade);
    }

    //----------------//
    // getTargetRatio //
    //----------------//
    @Override
    public final double getTargetRatio ()
    {
        return 1.0 + (getTargetCoeff() * grade);
    }

    //----------//
    // getGrade //
    //----------//
    /**
     * @return the grade
     */
    public double getGrade ()
    {
        return grade;
    }

    //------------//
    // getImpacts //
    //------------//
    @Override
    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public double getMinGrade ()
    {
        return constants.minGrade.getValue();
    }

    //----------//
    // setGrade //
    //----------//
    /**
     * @param grade the grade to set
     */
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //------------//
    // setImpacts //
    //------------//
    @Override
    public void setImpacts (GradeImpacts impacts)
    {
        this.impacts = impacts;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return String.format("%.2f~%s", grade, super.toString());
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    /**
     * @return the coefficient used to compute source support ratio (default: 0)
     */
    protected double getSourceCoeff ()
    {
        return 0;
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    /**
     * @return the coefficient used to compute target support ratio (default: 0)
     */
    protected double getTargetCoeff ()
    {
        return 0;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio minGrade = new Constant.Ratio(
                0.1,
                "Minimum support relation grade");
    }
}
