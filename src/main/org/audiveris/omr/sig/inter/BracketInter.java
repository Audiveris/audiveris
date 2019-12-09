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
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.BracketSymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.sig.ui.InterEditor;

/**
 * Class {@code BracketInter} represents the portion of a bracket limited to a staff.
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

    private static final Logger logger = LoggerFactory.getLogger(BracketInter.class);

    /** Bracket kind. */
    @XmlAttribute(name = "kind")
    private final BracketKind kind;

    /**
     * Creates a new {@code BracketInter} object.
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

    /**
     * Creates a new {@code BracketInter} object, meant for manual use.
     *
     * @param grade inter quality
     */
    public BracketInter (double grade)
    {
        super(null, Shape.BRACKET, grade, null, null);
        this.kind = BracketKind.BOTH;
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

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public void deriveFrom (ShapeSymbol symbol,
                            MusicFont font,
                            Point dropLocation,
                            Alignment alignment)
    {
        BracketSymbol bracketSymbol = (BracketSymbol) symbol;
        Model model = bracketSymbol.getModel(font, dropLocation, alignment);
        median = new Line2D.Double(model.p1, model.p2);
        width = model.width;
        computeArea(font);
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

    //-------------//
    // computeArea //
    //-------------//
    @Override
    protected void computeArea ()
    {
        if (sig != null) {
            Scale scale = sig.getSystem().getSheet().getScale();
            MusicFont font = MusicFont.getBaseFont(scale.getInterline());
            computeArea(font);
        }
    }

    //-------------//
    // computeArea //
    //-------------//
    protected void computeArea (MusicFont font)
    {
        TextLayout upperLayout = font.layout(Symbols.SYMBOL_BRACKET_UPPER_SERIF.getString());
        Rectangle2D upperRect = upperLayout.getBounds();

        TextLayout lowerLayout = font.layout(Symbols.SYMBOL_BRACKET_LOWER_SERIF.getString());
        Rectangle2D lowerRect = lowerLayout.getBounds();

        area = AreaUtil.verticalParallelogram(median.getP1(), median.getP2(), getWidth());

        // Top serif?
        if (kind == BracketKind.TOP || kind == BracketKind.BOTH) {
            Rectangle2D tr = new Rectangle2D.Double(median.getX1() - width / 2,
                                                    median.getY1() + upperRect.getY(),
                                                    upperRect.getWidth(),
                                                    -upperRect.getY());
            area.add(new Area(tr));
        }

        // Bottom serif?
        if (kind == BracketKind.BOTTOM || kind == BracketKind.BOTH) {
            Rectangle2D br = new Rectangle2D.Double(median.getX1() - width / 2,
                                                    median.getY2(),
                                                    lowerRect.getWidth(),
                                                    lowerRect.getHeight() + lowerRect.getY());
            area.add(new Area(br));
        }

        bounds = area.getBounds();
    }

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
        NONE
    }
}
