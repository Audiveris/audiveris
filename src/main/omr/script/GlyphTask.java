//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h T a s k                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.glyph.Glyph;
import omr.glyph.GlyphSignature;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>GlyphTask</code> is a script task which is applied to a
 * collection of glyphs
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class GlyphTask
    extends Task
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of glyphs which are concerned by this task */
    protected List<Glyph> glyphs;

    /** The signatures of these glyphs */
    protected Collection<GlyphSignature> sigs;

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
    @Override
    public void run (Sheet sheet)
        throws StepException
    {
        // Make sure the glyphs are available
        if (glyphs == null) {
            if (sigs == null) {
                throw new StepException("No glyphs defined");
            }

            glyphs = new ArrayList<Glyph>();

            for (GlyphSignature sig : sigs) {
                Glyph glyph = sheet.getVerticalLag()
                                   .getOriginal(sig);

                if (glyph == null) {
                    logger.warning("Cannot find glyph for " + sig);
                } else {
                    glyphs.add(glyph);
                }
            }
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        if (glyphs != null) {
            return Glyph.toString(glyphs);
        }

        if (sigs != null) {
            return " ids:" + sigs.toString();
        }

        return "";
    }

    //---------------//
    // setGlyphsSigs //
    //---------------//
    private void setGlyphsSigs (Collection<GlyphSignature> sigs)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "setGlyphsSigs this.sigs=" + this.sigs + " sigs=" + sigs);
        }

        if (this.sigs != sigs) {
            this.sigs.clear();
            this.sigs.addAll(sigs);
        }
    }

    //---------------//
    // getGlyphsSigs //
    //---------------//
    @XmlElement(name = "glyph")
    private Collection<GlyphSignature> getGlyphsSigs ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getGlyphsSigs this.sigs=" + this.sigs);
        }

        if (sigs == null) {
            sigs = new ArrayList<GlyphSignature>();

            if (glyphs != null) {
                for (Glyph glyph : glyphs) {
                    sigs.add(glyph.getSignature());
                }
            }
        }

        return sigs;
    }
}
