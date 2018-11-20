//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n d i n g I n t e r                                     //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.EndingSentenceRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.util.Jaxb;

import java.awt.Rectangle;
import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code EndingInter} represents an ending.
 * <p>
 * MusicXML spec:
 * The number attribute reflects the numeric values of what is under the ending line.
 * Single endings such as "1" or comma-separated multiple endings such as "1,2" may be used.
 * The ending element text is used when the text displayed in the ending is different than what
 * appears in the number attribute.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ending")
public class EndingInter
        extends AbstractInter
{

    // Persistent Data
    //----------------
    //
    /** Mandatory left leg. */
    @XmlElement(name = "left-leg")
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private final Line2D leftLeg;

    /** Horizontal line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private final Line2D line;

    /** Optional right leg, if any. */
    @XmlElement(name = "right-leg")
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private final Line2D rightLeg;

    // Transient Data
    //---------------
    //
    private final SegmentInter segment;

    /**
     * Creates a new EndingInter object.
     *
     * @param segment  horizontal segment
     * @param line     precise line
     * @param leftLeg  mandatory left leg
     * @param rightLeg optional right leg
     * @param bounds   bounding box
     * @param impacts  assignments details
     */
    public EndingInter (SegmentInter segment,
                        Line2D line,
                        Line2D leftLeg,
                        Line2D rightLeg,
                        Rectangle bounds,
                        GradeImpacts impacts)
    {
        super(null, bounds, Shape.ENDING, impacts);
        this.segment = segment;
        this.line = line;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private EndingInter ()
    {
        this.segment = null;
        this.line = null;
        this.leftLeg = null;
        this.rightLeg = null;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        Rectangle box = super.getBounds();

        if (box != null) {
            return box;
        }

        box = line.getBounds().union(leftLeg.getBounds());

        if (rightLeg != null) {
            box = box.union(rightLeg.getBounds());
        }

        return new Rectangle(bounds = box);
    }

    //-------------------//
    // getExportedNumber //
    //-------------------//
    /**
     * Filter the ending number string to comply with MusicXML constraint that it must
     * be formatted as "1" or "1,2".
     *
     * @return the formatted number string, if any
     */
    public String getExportedNumber ()
    {
        String raw = getNumber();

        if (raw == null) {
            return null;
        }

        String[] nums = raw.split("[^0-9]"); // Any non-digit character is a separator
        StringBuilder sb = new StringBuilder();

        for (String num : nums) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(num);
        }

        return sb.toString();
    }

    //------------//
    // getLeftLeg //
    //------------//
    /**
     * @return the leftLeg
     */
    public Line2D getLeftLeg ()
    {
        return leftLeg;
    }

    //---------//
    // getLine //
    //---------//
    /**
     * @return the line
     */
    public Line2D getLine ()
    {
        return line;
    }

    //-----------//
    // getNumber //
    //-----------//
    /**
     * Report the ending number clause, if any.
     *
     * @return ending number clause or null
     */
    public String getNumber ()
    {
        for (Relation r : sig.getRelations(this, EndingSentenceRelation.class)) {
            SentenceInter sentence = (SentenceInter) sig.getOppositeInter(this, r);
            TextRole role = sentence.getRole();
            String value = sentence.getValue().trim();

            if ((role == TextRole.EndingNumber) || value.matches("[1-9].*")) {
                return value;
            }
        }

        return null;
    }

    //-------------//
    // getRightLeg //
    //-------------//
    /**
     * @return the rightLeg
     */
    public Line2D getRightLeg ()
    {
        return rightLeg;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * The raw ending text, only if different from normalized number.
     * <p>
     * For instance, the actual text could be: "1., 2." and the normalized number: "1, 2"
     *
     * @return the raw ending text or null
     */
    public String getValue ()
    {
        final String number = getNumber();

        for (Relation r : sig.getRelations(this, EndingSentenceRelation.class)) {
            SentenceInter sentence = (SentenceInter) sig.getOppositeInter(this, r);
            String value = sentence.getValue().trim();

            if (!value.equals(number)) {
                return value;
            }
        }

        return null;
    }

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{
            "straight",
            "slope",
            "length",
            "leftBar",
            "rightBar"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1, 1};

        public Impacts (double straight,
                        double slope,
                        double length,
                        double leftBar,
                        double rightBar)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, straight);
            setImpact(1, slope);
            setImpact(2, length);
            setImpact(3, leftBar);
            setImpact(4, rightBar);
        }
    }
}
