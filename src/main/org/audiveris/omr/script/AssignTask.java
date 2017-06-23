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
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code AssignTask} assigns (or deassign) an inter to a collection of glyphs.
 *
 * <p>
 * If the compound flag is set, a compound glyph is composed from the provided glyphs and assigned
 * the inter. Otherwise, each provided glyph is assigned an inter.</p>
 *
 * @author Hervé Bitteur
 */
public class AssignTask
        extends GlyphTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Assigned shape (or null for a deassignment) */
    @XmlAttribute
    private final Shape shape;

    private final SystemInfo system;

    private final Staff staff;

    private final int interline;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an assignment task
     *
     * @param sheet  the containing sheet
     * @param system the related system
     * @param staff  the related staff (if any)
     * @param shape  the assigned shape
     * @param glyph  the concerned glyph
     */
    public AssignTask (Sheet sheet,
                       SystemInfo system,
                       Staff staff,
                       Shape shape,
                       Glyph glyph)
    {
        super(sheet, glyph);
        this.staff = staff;
        this.system = system;
        this.shape = shape;

        if (staff != null) {
            interline = staff.getSpecificInterline();
        } else {
            interline = sheet.getScale().getInterline();
        }
    }

    /** No-arg constructor for JAXB only. */
    protected AssignTask ()
    {
        shape = null;
        system = null;
        staff = null;
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
        sheet.getGlyphsController().syncAssign(this);
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
     * Report the assigned shape
     *
     * @return the assignedShape
     */
    public Shape getAssignedShape ()
    {
        return shape;
    }

    public int getInterline ()
    {
        return interline;
    }

    public Staff getStaff ()
    {
        return staff;
    }

    public SystemInfo getSystem ()
    {
        return system;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" assign");

        if (shape != null) {
            sb.append(" ").append(shape);
        } else {
            sb.append(" no-shape");
        }

        return sb.toString();
    }
}
