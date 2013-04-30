//----------------------------------------------------------------------------//
//                                                                            //
//                            A s s i g n T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Sheet;

import omr.step.Stepping;
import omr.step.Steps;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code AssignTask} assigns (or deassign) a shape to a
 * collection of glyphs.
 *
 * <p>Il the compound flag is set, a compound glyph is composed from the
 * provided glyphs and assigned the shape. Otherwise, each provided glyph is
 * assigned the shape.</p>
 *
 * @author Hervé Bitteur
 */
public class AssignTask
        extends GlyphUpdateTask
{
    //~ Instance fields --------------------------------------------------------

    /** Assigned shape (or null for a deassignment) */
    @XmlAttribute
    private final Shape shape;

    /** True for a compound building */
    @XmlAttribute
    private final boolean compound;

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // AssignTask //
    //------------//
    /**
     * Create an assignment task
     *
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
    }

    //------------//
    // AssignTask //
    //------------//
    /**
     * Convenient way to create an deassignment task
     *
     * @param glyphs the collection of glyphs to deassign
     */
    public AssignTask (Sheet sheet,
                       Collection<Glyph> glyphs)
    {
        this(sheet, null, false, glyphs);
    }

    //------------//
    // AssignTask //
    //------------//
    /** No-arg constructor for JAXB only */
    protected AssignTask ()
    {
        shape = null;
        compound = false;
    }

    //~ Methods ----------------------------------------------------------------
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
        sheet.getSymbolsController()
                .syncAssign(this);
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
        Stepping.reprocessSheet(
                Steps.valueOf(Steps.SYMBOLS),
                sheet,
                getImpactedSystems(sheet),
                false);
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

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" assign");

        if (compound) {
            sb.append(" compound");
        }

        if (shape != null) {
            sb.append(" ")
                    .append(shape);
        } else {
            sb.append(" no-shape");
        }

        return sb.toString();
    }
}
