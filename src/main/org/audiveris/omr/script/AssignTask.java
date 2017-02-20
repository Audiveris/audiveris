//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      A s s i g n T a s k                                       //
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
package org.audiveris.omr.script;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code AssignTask} assigns (or deassign) a shape to a collection of glyphs.
 *
 * <p>
 * Il the compound flag is set, a compound glyph is composed from the provided glyphs and assigned
 * the shape. Otherwise, each provided glyph is assigned the shape.</p>
 *
 * @author Hervé Bitteur
 */
public class AssignTask
        extends GlyphUpdateTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Assigned shape (or null for a deassignment) */
    @XmlAttribute
    private final Shape shape;

    /** True for a compound building */
    @XmlAttribute
    private final boolean compound;

    private final int interline;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an assignment task
     *
     * @param sheet    the containing sheet
     * @param shape    the assigned shape (or null for a de-assignment)
     * @param compound true if all glyphs are to be merged into one compound
     *                 which is assigned to the given shape, false if each and
     *                 every glyph is to be assigned to the given shape
     * @param glyphs   the collection of concerned glyphs
     */
    public AssignTask (Sheet sheet,
                       Shape shape,
                       boolean compound,
                       Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
        this.shape = shape;
        this.compound = compound;
        interline = sheet.getScale().getInterline();
    }

    /**
     * Convenient way to create an deassignment task
     *
     * @param sheet  the containing sheet
     * @param glyphs the collection of glyphs to deassign
     */
    public AssignTask (Sheet sheet,
                       Collection<Glyph> glyphs)
    {
        this(sheet, null, false, glyphs);
    }

    /** No-arg constructor for JAXB only. */
    protected AssignTask ()
    {
        shape = null;
        compound = false;
        interline = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //------//
    // core //
    //------//
    /**
     * {@inheritDoc}
     */
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController().syncAssign(this);
    }

    //--------//
    // epilog //
    //--------//
    /**
     * {@inheritDoc}
     */
    @Override
    public void epilog (Sheet sheet)
    {
        // We rebuild from SYMBOLS
        //        Stepping.reprocessSheet(
        //                Steps.valueOf(Steps.SYMBOLS),
        //                sheet,
        //                getImpactedSystems(sheet),
        //                false);
    }

    //------------------//
    // getAssignedShape //
    //------------------//
    /**
     * Report the assigned shape (for an assignment impact)
     *
     * @return the assignedShape (null for a deassignment)
     */
    public Shape getAssignedShape ()
    {
        return shape;
    }

    //------------//
    // isCompound //
    //------------//
    /**
     * Report whether the assignment is a compound
     *
     * @return true for a compound assignment, false otherwise
     */
    public boolean isCompound ()
    {
        return compound;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" assign");

        if (compound) {
            sb.append(" compound");
        }

        if (shape != null) {
            sb.append(" ").append(shape);
        } else {
            sb.append(" no-shape");
        }

        return sb.toString();
    }
}
