//----------------------------------------------------------------------------//
//                                                                            //
//                            A s s i g n T a s k                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>AssignTask</code> is a script task which assigns a shape to a
 * collection of glyphs
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class AssignTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Assigned shape */
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
     * @param shape the shape to be assigned
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
    /** No-arg constructor needed for JAXB */
    private AssignTask ()
    {
        shape = null;
        compound = false;
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    @Override
    public void run (Sheet sheet)
        throws StepException
    {
        super.run(sheet);
        sheet.getSymbolsController()
             .asyncAssignGlyphSet(glyphs, shape, compound);
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
            sb.append(" ")
              .append("compound");
        }

        sb.append(" ")
          .append(shape);

        return sb.toString() + super.internalsString();
    }
}
