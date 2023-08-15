//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S u p p o r t                                       //
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
 * Abstract class <code>Support</code> is a relation between two interpretation instances,
 * designated as the source and the target, that support one another.
 * <p>
 * Typical examples are mutual support:
 * <ul>
 * <li>between a stem and a note head
 * <li>between a stem and a beam
 * </ul>
 * <p>
 * Depending on the precise support relation class and on the quality of the geometric junction,
 * the source and the target instances will have their contextual grades benefit from this relation
 * instance.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "support")
public abstract class Support
        extends Relation
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The [0..1] quality value of the geometric junction.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double3Adapter.class)
    protected double grade;

    /** Details about grade (mainly for debugging). */
    protected GradeImpacts impacts;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BasicSupport object, with a grade set to 1.
     */
    public Support ()
    {
        this(1.0);
    }

    /**
     * Creates a new BasicSupport object.
     *
     * @param grade the grade evaluated for this relation
     */
    public Support (double grade)
    {
        this.grade = grade;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getGrade //
    //----------//
    /**
     * Report the support grade.
     *
     * @return the grade
     */
    public double getGrade ()
    {
        return grade;
    }

    //------------//
    // getImpacts //
    //------------//
    /**
     * Report details about the final relation grade
     *
     * @return the grade details
     */
    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    //-------------//
    // getMinGrade //
    //-------------//
    /**
     * Report the minimum grade for this relation.
     *
     * @return minimum grade
     */
    public double getMinGrade ()
    {
        return constants.minGrade.getValue();
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
    // getSourceRatio //
    //----------------//
    /**
     * Report the support ratio for source inter
     *
     * @return support ratio for source (value is always &ge; 1)
     */
    public final double getSourceRatio ()
    {
        return 1.0 + (getSourceCoeff() * grade);
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

    //----------------//
    // getTargetRatio //
    //----------------//
    /**
     * Report the support ratio for target inter
     *
     * @return support ratio for target (value is always &ge; 1)
     */
    public final double getTargetRatio ()
    {
        return 1.0 + (getTargetCoeff() * grade);
    }

    //----------//
    // setGrade //
    //----------//
    /**
     * Set the support grade value.
     *
     * @param grade the grade to set
     */
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //------------//
    // setImpacts //
    //------------//
    /**
     * Assign details about the relation grade
     *
     * @param impacts the grade impacts
     */
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio minGrade = new Constant.Ratio(
                0.1,
                "Minimum support relation grade");
    }
}
