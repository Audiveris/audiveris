//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A r p e g g i a t o I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.ChordArpeggiatoRelation;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ArpeggiatoInter} represents the arpeggiato notation along the heads
 * of a chord.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "arpeggiato")
public class ArpeggiatoInter
        extends AbstractNotationInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ArpeggiatoInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ArpeggiatoInter} object.
     *
     * @param glyph the arpeggiato glyph
     * @param grade the interpretation quality
     */
    public ArpeggiatoInter (Glyph glyph,
                            double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, Shape.ARPEGGIATO, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ArpeggiatoInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordArpeggiatoRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create an arpeggiato inter.
     *
     * @param glyph            the arpeggiato glyph
     * @param grade            the interpretation quality
     * @param system           the related system
     * @param systemHeadChords abscissa-ordered list of head-chords in this system
     * @return the created arpeggiato or null
     */
    public static ArpeggiatoInter create (Glyph glyph,
                                          double grade,
                                          SystemInfo system,
                                          List<Inter> systemHeadChords)
    {
        // Look for a head-chord on right side of this symbol
        // Use a lookup box (glyph height, predefined width)
        // For intersected head-chords, measure y overlap WRT glyph height
        Rectangle luBox = glyph.getBounds();
        luBox.x += luBox.width;
        luBox.width = system.getSheet().getScale().toPixels(constants.areaDx);

        final List<Inter> chords = SIGraph.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        int bestOverlap = 0;
        HeadChordInter bestChord = null;

        for (Inter chord : chords) {
            HeadChordInter hc = (HeadChordInter) chord;
            Rectangle headsBox = hc.getHeadsBounds();

            if (headsBox.intersects(luBox)) {
                int overlap = GeoUtil.yOverlap(headsBox, luBox);

                if (bestOverlap < overlap) {
                    bestOverlap = overlap;
                    bestChord = hc;
                }
            }
        }

        if (bestChord == null) {
            return null;
        }

        double relGrade = (double) bestOverlap / luBox.height;
        ChordArpeggiatoRelation rel = new ChordArpeggiatoRelation(relGrade);

        if (relGrade < rel.getMinGrade()) {
            return null;
        }

        ArpeggiatoInter arpeggiato = new ArpeggiatoInter(glyph, grade);
        system.getSig().addVertex(arpeggiato);
        system.getSig().addEdge(bestChord, arpeggiato, rel);
        arpeggiato.setStaff(bestChord.getLeadingNote().getStaff());

        return arpeggiato;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Scale.Fraction areaDx = new Scale.Fraction(1.5, "Width of lookup area for embraced notes");
    }
}
