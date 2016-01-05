//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R a t i o n a l T a s k                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.classifier.Evaluation;
import omr.glyph.Glyph;

import omr.score.TimeRational;

import omr.sheet.Sheet;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code RationalTask} records the assignment of a time
 * rational value to a collection of glyphs
 *
 * @author Hervé Bitteur
 */
public class RationalTask
        extends GlyphUpdateTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Type of the rational glyph */
    @XmlElement
    private final TimeRational timeRational;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RationalTask object.
     *
     * @param sheet        the sheet impacted
     * @param timeRational the custom time sig rational value
     * @param glyphs       the impacted glyph(s)
     */
    public RationalTask (Sheet sheet,
                         TimeRational timeRational,
                         Collection<Glyph> glyphs)
    {
        super(sheet, glyphs);
        this.timeRational = timeRational;
    }

    /** No-arg constructor for JAXB only. */
    private RationalTask ()
    {
        timeRational = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // core //
    //------//
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        sheet.getSymbolsController().getModel().assignTimeRational(
                getInitialGlyphs(),
                timeRational,
                Evaluation.MANUAL);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" rational");

        if (timeRational != null) {
            sb.append(" ").append(timeRational);
        }

        return sb.toString();
    }
}
