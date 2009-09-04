//----------------------------------------------------------------------------//
//                                                                            //
//                           S e c t i o n T a s k                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.GlyphSection;
import omr.glyph.SectionSets;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.util.*;

import javax.xml.bind.annotation.*;

/**
 * Class <code>SectionTask</code> is a script task which is applied to a
 * collection of section sets
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class SectionTask
    extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of sections which are concerned by this task */
    @XmlElement(name = "sets")
    protected SectionSets sectionSets;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SectionTask //
    //-------------//
    /**
     * Creates a new SectionTask object.
     *
     * @param sectionSets the collection of section sets concerned by this task
     */
    public SectionTask (Collection<Collection<GlyphSection>> sectionSets)
    {
        this.sectionSets = new SectionSets(sectionSets);
    }

    //-------------//
    // SectionTask //
    //-------------//
    /**
     * No-arg constructor needed for JAXB
     */
    protected SectionTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    /**
     * Method made final to force the retrieval of section sets beforehand.
     * Additional processing should take place in an overridden runEpilog method
     *
     * @param sheet the related sheet
     * @throws omr.step.StepException
     */
    @Override
    public final void run (Sheet sheet)
        throws StepException
    {
        // Make sure the concrete sections are available
        sectionSets.getSets(sheet);

        // Now the real processing
        runEpilog(sheet);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        if (sectionSets != null) {
            return sectionSets.toString();
        } else {
            return "";
        }
    }

    //-----------//
    // runEpilog //
    //-----------//
    /**
     * Do the real processing
     * @param sheet the related sheet
     * @throws StepException
     */
    protected abstract void runEpilog (Sheet sheet)
        throws StepException;
}
