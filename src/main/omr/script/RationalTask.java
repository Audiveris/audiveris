//----------------------------------------------------------------------------//
//                                                                            //
//                          R a t i o n a l T a s k                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Evaluation;
import omr.glyph.Glyph;

import omr.math.Rational;

import omr.sheet.Sheet;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;

/**
 * Class <code>RationalTask</code> records the assignment of a rational value
 * to a collection of glyphs
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class RationalTask
    extends GlyphTask
{
    //~ Instance fields --------------------------------------------------------

    /** Type of the rationalual glyph */
    @XmlElement
    private final Rational rational;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // RationalTask //
    //--------------//
    /**
     * Creates a new RationalTask object.
     *
     * @param rational the custom time sig rational value
     * @param glyphs the impacted glyph(s)
     */
    public RationalTask (Rational          rational,
                         Collection<Glyph> glyphs)
    {
        super(glyphs);
        this.rational = rational;
    }

    //--------------//
    // RationalTask //
    //--------------//
    /** No-arg constructor needed by JAXB */
    private RationalTask ()
    {
        rational = null;
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
        throws Exception
    {
        sheet.getSymbolsController()
             .getModel()
             .assignRational(getInitialGlyphs(), rational, Evaluation.MANUAL);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" rational");

        if (rational != null) {
            sb.append(" ")
              .append(rational);
        }

        return sb + super.internalsString();
    }
}
