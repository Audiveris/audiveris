//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R a t i o n a l T a s k                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
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
