//----------------------------------------------------------------------------//
//                                                                            //
//                       G l y p h U p d a t e T a s k                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.GlyphSection;
import omr.glyph.GlyphsBuilder;
import omr.glyph.SectionSets;
import omr.glyph.facets.Glyph;

import omr.lag.LagOrientation;

import omr.sheet.SystemInfo;

import java.util.*;

import javax.xml.bind.annotation.*;

/**
 * Class {@code GlyphUpdateTask} is applied to a collection of existing glyphs.
 *
 * <p>The glyphs are designated either by their member sections, or (for the
 * special case of virtual glyphs) simply by their location.
 *
 * <p>Since sections are stable (they are assigned once and for all, the
 * relationship between a section and its containing system can be modified only
 * when system boundaries change in SystemsBuilder) they are used for the
 * underlying persistency of any GlyphUpdateTask. The XML file will thus contain
 * the ids of the member sections of the related glyphs.</p>
 *
 * <h4>Glyphs and sections in a script:<br/>
 *    <img src="doc-files/script.jpg"/>
 * </h4>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class GlyphUpdateTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of underlying section sets (representing glyphs) */
    @XmlElement(name = "glyphs")
    protected final SectionSets sectionSets;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // GlyphUpdateTask //
    //-----------------//
    /**
     * Creates a new GlyphUpdateTask object.
     *
     * @param orientation orientation of the containing lag
     * @param glyphs the collection of glyphs concerned by this task
     */
    public GlyphUpdateTask (LagOrientation    orientation,
                            Collection<Glyph> glyphs)
    {
        super(orientation, glyphs);
        sectionSets = SectionSets.createFromGlyphs(glyphs);
    }

    //-----------------//
    // GlyphUpdateTask //
    //-----------------//
    /**
     * Creates a new GlyphUpdateTask object, for vertical glyphs by default
     *
     * @param glyphs the collection of glyphs concerned by this task
     */
    public GlyphUpdateTask (Collection<Glyph> glyphs)
    {
        this(LagOrientation.VERTICAL, glyphs);
    }

    //-----------------//
    // GlyphUpdateTask //
    //-----------------//
    /**
     * Constructor needed by no-arg constructors of subclasses (for JAXB)
     */
    protected GlyphUpdateTask ()
    {
        sectionSets = null; // Dummy value
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // retrieveGlyphs //
    //----------------//
    @Override
    protected void retrieveGlyphs ()
    {
        glyphs = new TreeSet<Glyph>(Glyph.globalComparator);

        for (Collection<GlyphSection> set : sectionSets.getSets(
            sheet,
            orientation)) {
            Glyph glyph = null;

            if (orientation == LagOrientation.VERTICAL) {
                SystemInfo system = set.iterator()
                                       .next()
                                       .getSystem();
                glyph = system.addGlyph(system.buildGlyph(set));
            } else {
                glyph = GlyphsBuilder.buildGlyph(sheet.getScale(), set);
                glyph = sheet.getHorizontalLag()
                             .addGlyph(glyph);
                logger.info("Recreated " + glyph);
            }

            glyphs.add(glyph);
        }
    }
}
