//----------------------------------------------------------------------------//
//                                                                            //
//                     S e c t i o n A s s i g n T a s k                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.GlyphSection;
import omr.glyph.Shape;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>SectionAssignTask</code> is a script task which assigns a shape
 * to a glyph directly built from a provided collection of sections
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SectionAssignTask
    extends SectionTask
{
    //~ Instance fields --------------------------------------------------------

    /** Assigned shape */
    @XmlAttribute
    private final Shape shape;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // SectionAssignTask //
    //-------------------//
    /**
     * Create an assignment task
     *
     * @param shape the shape to be assigned
     * @param sections the collection of provided sections
     */
    public SectionAssignTask (Shape                    shape,
                              Collection<GlyphSection> sections)
    {
        super(sections);
        this.shape = shape;
    }

    //-------------------//
    // SectionAssignTask //
    //-------------------//
    /** No-arg constructor needed for JAXB */
    private SectionAssignTask ()
    {
        shape = null;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" section-assign");

        sb.append(" ")
          .append(shape);

        return sb.toString() + super.internalsString();
    }

    //-----//
    // run //
    //-----//
    @Override
    protected void runEpilog (Sheet sheet)
        throws StepException
    {
        sheet.getSymbolsController()
             .asyncAssignSectionSet(sections, shape);
    }
}
