//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B r a c k e t I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.GradeImpacts;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BracketInter}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bracket")
public class BracketInter
        extends AbstractVerticalInter
{

    /** Bracket kind. */
    @XmlAttribute(name = "kind")
    private final BracketKind kind;

    /**
     * Creates a new {@code BracketInter} object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the bar line width
     * @param kind    precise kind of bracket item
     */
    public BracketInter (Glyph glyph,
                         GradeImpacts impacts,
                         Line2D median,
                         double width,
                         BracketKind kind)
    {
        super(glyph, Shape.BRACKET, impacts, median, width);
        this.kind = kind;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BracketInter ()
    {
        super(null, null, null, null, null);
        this.kind = null;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());
        sb.append(" ").append(kind);

        return sb.toString();
    }

    //---------//
    // getKind //
    //---------//
    /**
     * Report the bracket kind.
     *
     * @return the kind
     */
    public BracketKind getKind ()
    {
        return kind;
    }

    /**
     * Kind of bracket.
     */
    public static enum BracketKind
    {
        TOP,
        BOTH,
        BOTTOM,
        NONE
    }
}
