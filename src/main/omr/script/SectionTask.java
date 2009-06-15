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

import omr.lag.Sections;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>SectionTask</code> is a script task which is applied to a
 * collection of sections
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
    protected List<GlyphSection> sections;

    /** The ids of these sections */
    protected Collection<Integer> ids;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SectionTask //
    //-------------//
    /**
     * Creates a new SectionTask object.
     *
     * @param sections the collection of sections concerned by this task
     */
    public SectionTask (Collection<GlyphSection> sections)
    {
        this.sections = new ArrayList<GlyphSection>(sections);
    }

    //-------------//
    // SectionTask //
    //-------------//
    /**
     * Constructor needed by no-arg constructors of subclasses (for JAXB)
     */
    protected SectionTask ()
    {
        sections = null; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    /**
     * Method made final to force the retrieval of sections. Additional
     * processing should take place in an overridden runEpilog method
     * @param sheet the related sheet
     * @throws omr.step.StepException
     */
    @Override
    public final void run (Sheet sheet)
        throws StepException
    {
        // Make sure the sections are available
        if (sections == null) {
            if (ids == null) {
                throw new StepException("No sections defined");
            }

            sections = new ArrayList<GlyphSection>();

            for (int id : ids) {
                GlyphSection section = sheet.getVerticalLag()
                                            .getVertexById(id);

                if (section == null) {
                    logger.warning(
                        "Cannot find section for " + id,
                        new Throwable());
                } else {
                    sections.add(section);
                }
            }
        }

        // Now the real processing
        runEpilog(sheet);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        if (sections != null) {
            return Sections.toString(sections);
        }

        if (ids != null) {
            return " ids:" + ids.toString();
        }

        return "";
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

    //----------------//
    // setSectionsIds //
    //----------------//
    private void setSectionsIds (Collection<Integer> ids)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setGlyphsIds this.sigs=" + this.ids + " sigs=" + ids);
        }

        if (this.ids != ids) {
            this.ids.clear();
            this.ids.addAll(ids);
        }
    }

    //----------------//
    // getSectionsIds //
    //----------------//
    @XmlElement(name = "section")
    private Collection<Integer> getSectionsIds ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getSectionsIds this.ids=" + this.ids);
        }

        if (ids == null) {
            ids = new ArrayList<Integer>();

            if (sections != null) {
                for (GlyphSection section : sections) {
                    ids.add(section.getId());
                }
            }
        }

        return ids;
    }
}
