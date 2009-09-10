//----------------------------------------------------------------------------//
//                                                                            //
//                            A s s i g n T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.Sheet;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>AssignTask</code> is a script task which assigns (or deassign)
 * a shape to a collection of glyphs.
 *
 * <p>Il the compound flag is set, a compound glyph may is composed from the
 * provided glyphs and assigned the shape. Otherwise, each provided glyph is
 * assigned the shape.</p>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class AssignTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Assigned shape (or null for a deassignment) */
    @XmlAttribute
    private final Shape shape;

    /** True for a compound building */
    @XmlAttribute
    private final boolean compound;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // AssignTask //
    //------------//
    /**
     * Create an assignment task
     *
     * @param shape the assigned shape (or null for a de-assignment)
     * @param compound true if all glyphs are to be merged into one compound
     * which is assigned to the given shape, false if each and every glyph is to
     * be assigned to the given shape
     * @param glyphs the collection of concerned glyphs
     */
    public AssignTask (Shape             shape,
                       boolean           compound,
                       Collection<Glyph> glyphs)
    {
        super(glyphs);
        this.shape = shape;
        this.compound = compound;
    }

    //------------//
    // AssignTask //
    //------------//
    /**
     * Convenient way to create an deassignment task
     *
     * @param glyphs the collection of glyphs to deassign
     */
    public AssignTask (Collection<Glyph> glyphs)
    {
        this(null, false, glyphs);
    }

    //------------//
    // AssignTask //
    //------------//
    /** No-arg constructor needed for JAXB */
    protected AssignTask ()
    {
        shape = null;
        compound = false;
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getAssignedShape //
    //------------------//
    /**
     * Report the assigned shape (for an assignment impact)
     * @return the assignedShape (null for a deasssignment)
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
     * @return true for a compound assignment, false otherwise
     */
    public boolean isCompound ()
    {
        return compound;
    }

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        sheet.getSymbolsController()
             .syncAssign(this);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" assign");

        if (compound) {
            sb.append(" compound");
        }

        if (shape != null) {
            sb.append(" ")
              .append(shape);
        }

        sb.append(" ")
          .append(shape);

        return sb + super.internalsString();
    }
}
