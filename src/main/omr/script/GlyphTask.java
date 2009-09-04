//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h T a s k                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.Glyph;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.SectionSets;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.StepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>GlyphTask</code> is a script task which is applied to a
 * collection of glyphs.
 * <p>Since sections are stable (they are assigned once and for all, the
 * relationship between a section and its containing system can be modified only
 * when system boundaries change in SystemsBuilder) they are used for the
 * underlying persistency of any GlyphTask. The XML file will thus contain the
 * ids of the member sections of the related glyphs.</p>
 *
 * <h4>Glyphs and sections in a script:<br/>
 *    <img src="doc-files/script.jpg"/>
 * </h4>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class GlyphTask
    extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of glyphs which are concerned by this task */
    protected List<Glyph> glyphs;

    /** The collection of underlying section sets */
    @XmlElement(name = "glyphs")
    private SectionSets sectionSets;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // GlyphTask //
    //-----------//
    /**
     * Creates a new GlyphTask object.
     *
     * @param glyphs the collection of glyphs concerned by this task
     */
    public GlyphTask (Collection<Glyph> glyphs)
    {
        this.glyphs = new ArrayList<Glyph>(glyphs);
        sectionSets = SectionSets.createFromGlyphs(glyphs);
    }

    //-----------//
    // GlyphTask //
    //-----------//
    /**
     * Constructor needed by no-arg constructors of subclasses (for JAXB)
     */
    protected GlyphTask ()
    {
        glyphs = null; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    /**
     * Method made final to make sure the concrete sections - and thus the
     * related concrete glyphs - are available before processing be launched
     * (this is needed for the case of unmarshalled instance).
     * The actual processing should take place in an overridden runEpilog method
     * @param sheet the related sheet
     * @throws omr.step.StepException
     */
    @Override
    public final void run (Sheet sheet)
        throws StepException
    {
        // Make sure the concrete sections and glyphs are available
        if (glyphs == null) {
            glyphs = new ArrayList<Glyph>();

            for (Collection<GlyphSection> set : sectionSets.getSets(sheet)) {
                SystemInfo system = set.iterator()
                                       .next()
                                       .getSystem();
                Glyph      glyph = system.addGlyph(system.buildGlyph(set));
                glyphs.add(glyph);
            }
        }

        // Now the real processing
        try {
            runEpilog(sheet);
        } catch (Exception ex) {
            logger.warning(
                "Error in running " + this.getClass().getSimpleName(),
                ex);
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        if (glyphs != null) {
            return " " + Glyphs.toString(glyphs);
        } else {
            return " no-glyphs";
        }
    }

    //-----------//
    // runEpilog //
    //-----------//
    /**
     * Do the real processing
     * @param sheet the related sheet
     * @throws Exception
     */
    protected abstract void runEpilog (Sheet sheet)
        throws Exception;
}
