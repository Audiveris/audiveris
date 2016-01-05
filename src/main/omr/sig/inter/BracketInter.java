//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B r a c k e t I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sig.GradeImpacts;

import java.awt.geom.Line2D;

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
    //~ Enumerations -------------------------------------------------------------------------------

    public static enum BracketKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        TOP,
        BOTH,
        BOTTOM,
        NONE;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Bracket kind. */
    private final BracketKind kind;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
     * @return the kind
     */
    public BracketKind getKind ()
    {
        return kind;
    }
}
