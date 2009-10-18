//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h T a s k                              //
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
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.SectionSets;
import omr.glyph.Shape;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Step;

import java.util.*;

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

    /** The collection of underlying section sets (representing glyphs) */
    @XmlElement(name = "glyphs")
    protected final SectionSets sectionSets;

    /** The collection of glyphs which are concerned by this task */
    protected SortedSet<Glyph> glyphs;

    /** The set of (pre) impacted systems, using status before action */
    protected SortedSet<SystemInfo> initialSystems;

    /** The related sheet */
    protected Sheet sheet;

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
        this.glyphs = new TreeSet<Glyph>();
        this.glyphs.addAll(glyphs);

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
        sectionSets = null; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getImpactedSystems //
    //--------------------//
    /**
     * Report the set of systems that are impacted by the action
     * @return the ordered set of impacted systems
     */
    public SortedSet<SystemInfo> getImpactedSystems ()
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();
        impactedSystems.addAll(initialSystems);
        impactedSystems.addAll(retrieveCurrentImpact());

        return impactedSystems;
    }

    //------------------//
    // getInitialGlyphs //
    //------------------//
    /**
     * Report the collection of initial glyphs
     * @return the impactedGlyphs
     */
    public SortedSet<Glyph> getInitialGlyphs ()
    {
        return glyphs;
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        sheet.getSheetSteps()
             .rebuildFrom(Step.PATTERNS, getImpactedSystems(), false);

        super.epilog(sheet);
    }

    //------------------//
    // impactAllSystems //
    //------------------//
    /**
     * Set the impacted systems as all systems
     */
    public void impactAllSystems ()
    {
        initialSystems.addAll(sheet.getSystems());
    }

    //--------//
    // prolog //
    //--------//
    @Override
    public void prolog (Sheet sheet)
    {
        super.prolog(sheet);
        this.sheet = sheet;
        initialSystems = retrieveCurrentImpact();

        // Make sure the concrete sections and glyphs are available
        if (glyphs == null) {
            glyphs = new TreeSet<Glyph>();

            for (Collection<GlyphSection> set : sectionSets.getSets(sheet)) {
                SystemInfo system = set.iterator()
                                       .next()
                                       .getSystem();
                Glyph      glyph = system.addGlyph(system.buildGlyph(set));
                glyphs.add(glyph);
            }
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (glyphs != null) {
            sb.append(" ")
              .append(Glyphs.toString(glyphs));
        } else {
            sb.append(" no-glyphs");
        }

        return sb + super.internalsString();
    }

    //-----------------------//
    // retrieveCurrentImpact //
    //-----------------------//
    /**
     * Report the set of systems that are impacted by the action, as determined
     * by the *current status* of the glyphs *currently* pointed by the sections
     * @return the ordered set of impacted systems
     */
    protected SortedSet<SystemInfo> retrieveCurrentImpact ()
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();

        for (Collection<GlyphSection> set : sectionSets.getSets(sheet)) {
            for (GlyphSection section : set) {
                SystemInfo system = section.getSystem();
                Glyph      glyph = section.getGlyph();

                if (system != null) {
                    impactedSystems.add(system);
                }

                if (glyph != null) {
                    Shape shape = glyph.getShape();

                    if ((shape != null) && shape.isPersistent()) {
                        // Include all following systems
                        impactedSystems.addAll(remaining(system));
                    }
                }
            }
        }

        return impactedSystems;
    }

    //-----------//
    // remaining //
    //-----------//
    /**
     * Report the collection of systems that follow the provided one
     * @param system the one after which we pick any system
     * @return the remaining portion of the sequence of systems in the sheet
     */
    private Collection<SystemInfo> remaining (SystemInfo system)
    {
        List<SystemInfo> all = sheet.getSystems();

        return all.subList(all.indexOf(system) + 1, all.size());
    }
}
