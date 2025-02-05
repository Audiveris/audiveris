//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B r a c k e t I n t e r                                    //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>BracketInter</code> represents the portion of a bracket limited to a staff.
 * <p>
 * A bracket that spans several staves is modeled as one BracketInter per staff, interleaved by
 * {@link BracketConnectorInter} instances.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bracket")
public class BracketInter
        extends AbstractStaffVerticalInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BracketInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Bracket kind. */
    @XmlAttribute(name = "kind")
    private final BracketKind kind;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private BracketInter ()
    {
        super(null, null, (Double) null, null, null);
        this.kind = null;
    }

    /**
     * Creates a new <code>BracketInter</code> object, meant for manual use.
     *
     * @param grade inter quality
     */
    public BracketInter (Double grade)
    {
        super(null, Shape.BRACKET, grade, null, null);
        this.kind = BracketKind.BOTH;
    }

    /**
     * Creates a new <code>BracketInter</code> object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the assignment details
     * @param median  the median line (without the serifs, from upper to lower staff lines)
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

    //-------------//
    // computeArea //
    //-------------//
    @Override
    protected void computeArea ()
    {
        if (sig != null) {
            final Sheet sheet = sig.getSystem().getSheet();
            final MusicFamily family = sheet.getStub().getMusicFamily();
            final MusicFont font = MusicFont.getBaseFont(family, sheet.getScale().getInterline());
            computeArea(font);
        }
    }

    //-------------//
    // computeArea //
    //-------------//
    protected void computeArea (MusicFont font)
    {
        final Rectangle2D upperRect = font.layoutShapeByCode(Shape.BRACKET_UPPER_SERIF).getBounds();
        final Rectangle2D lowerRect = font.layoutShapeByCode(Shape.BRACKET_LOWER_SERIF).getBounds();

        area = AreaUtil.verticalParallelogram(median.getP1(), median.getP2(), getWidth());

        // Top serif?
        if (kind == BracketKind.TOP || kind == BracketKind.BOTH) {
            Rectangle2D tr = new Rectangle2D.Double(
                    median.getX1() - width / 2,
                    median.getY1() - upperRect.getHeight(),
                    upperRect.getWidth(),
                    upperRect.getHeight());
            area.add(new Area(tr));
        }

        // Bottom serif?
        if (kind == BracketKind.BOTTOM || kind == BracketKind.BOTH) {
            Rectangle2D br = new Rectangle2D.Double(
                    median.getX1() - width / 2,
                    median.getY2(),
                    lowerRect.getWidth(),
                    lowerRect.getHeight());
            area.add(new Area(br));
        }

        bounds = area.getBounds();
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        final Rectangle2D wholeRect = font.layoutShapeByCode(symbol.getShape()).getBounds();
        final Rectangle2D upperRect = font.layoutShapeByCode(Shape.BRACKET_UPPER_SERIF).getBounds();
        final double wholeWidth = wholeRect.getWidth();
        final double trunkHeight = wholeRect.getHeight() - 2 * upperRect.getHeight();
        width = font.layoutShapeByCode(Shape.THICK_BARLINE).getBounds().getWidth();
        median = new Line2D.Double(
                dropLocation.x - wholeWidth / 2 + width / 2,
                dropLocation.y - trunkHeight / 2,
                dropLocation.x - wholeWidth / 2 + width / 2,
                dropLocation.y + trunkHeight / 2);
        computeArea(font);

        return true;
    }

    //---------//
    // getArea //
    //---------//
    @Override
    public Area getArea ()
    {
        // For brackets, due to serifs, the computation of area needs SIG information.
        // Therefore, area computation must be done in a lazy manner.
        if (area == null) {
            computeArea();
        }

        return area;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        final StringBuilder sb = new StringBuilder(super.getDetails());
        sb.append((sb.length() != 0) ? " " : "");
        sb.append(kind);

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new AbstractVerticalInter.Editor(this, isManual());
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

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle bounds)
    {
        // We cannot use super.setBounds()
        // because AbstractVerticalInter.setBounds() assigns bounds width to item width variable.
        // And bracket width variable is only the trunk width, much less than the serifs width.
        this.bounds = (bounds != null) ? new Rectangle(bounds) : null;
    }

    //~ Enumerations -------------------------------------------------------------------------------

    //-------------//
    // BracketKind //
    //-------------//
    /**
     * Kind of bracket.
     */
    public static enum BracketKind
    {
        /** With upper serif. */
        TOP,
        /** With upper and lower serifs. */
        BOTH,
        /** With lower serif. */
        BOTTOM,
        /** With no serif. */
        NONE;
    }
}
