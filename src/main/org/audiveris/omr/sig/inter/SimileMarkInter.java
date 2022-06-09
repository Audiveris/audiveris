//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S i m i l e M a r k I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.ui.HorizontalEditor;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SimileMarkInter</code> represents a simile mark to indicate that preceding
 * measure(s) are to be repeated.
 * <p>
 * The precise shape indicates simile marks for one, two or four measures.
 * <p>
 * <img alt="Music Simile" href="https://en.wikipedia.org/wiki/File:Music-simile.svg">
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "simile")
public class SimileMarkInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SimileMarkInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>SimileMarkInter</code> object.
     *
     * @param glyph the mark glyph
     * @param shape one of {@link ShapeSet#RepeatBars} shapes
     * @param grade the interpretation quality
     */
    public SimileMarkInter (Glyph glyph,
                            Shape shape,
                            Double grade)
    {
        super(glyph, glyph.getBounds(), shape, grade);
    }

    /**
     * Creates manually a new SimileMarkInter ghost object.
     *
     * @param shape precise mark shape
     * @param grade quality grade
     */
    public SimileMarkInter (Shape shape,
                            Double grade)
    {
        super(null, null, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SimileMarkInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }
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
        return deriveOnStaffMiddleLine(this, staff, symbol, sheet, font, dropLocation);
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new HorizontalEditor(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a SimileMarkInter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static SimileMarkInter create (Glyph glyph,
                                          Shape shape,
                                          double grade,
                                          Staff staff)
    {
        SimileMarkInter simile = new SimileMarkInter(glyph, shape, grade);
        simile.setStaff(staff);

        return simile;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove it from containing measure.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.removeInter(this);
        }

        super.remove(extensive);
    }
}
