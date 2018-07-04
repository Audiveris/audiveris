//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A r p e g g i a t o I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sig.relation.ChordArpeggiatoRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
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
        extends AbstractInter
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No chord linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a chord is connected
        setAbnormal(!sig.hasRelation(this, ChordArpeggiatoRelation.class));

        return isAbnormal();
    }

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add an arpeggiato inter.
     *
     * @param glyph            the arpeggiato glyph
     * @param grade            the interpretation quality
     * @param system           the related system
     * @param systemHeadChords abscissa-ordered list of head-chords in this system
     * @return the created arpeggiato or null
     */
    public static ArpeggiatoInter createValidAdded (Glyph glyph,
                                                    double grade,
                                                    SystemInfo system,
                                                    List<Inter> systemHeadChords)
    {
        ArpeggiatoInter arpeggiato = new ArpeggiatoInter(glyph, grade);

        Link link = arpeggiato.lookupLink(systemHeadChords, system);

        if (link != null) {
            system.getSig().addVertex(arpeggiato);
            link.applyTo(arpeggiato);

            return arpeggiato;
        }

        return null;
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

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        // Not very optimized!
        List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        Link link = lookupLink(systemHeadChords, system);

        if (link == null) {
            return Collections.emptyList();
        }

        if (doit) {
            link.applyTo(this);
        }

        return Collections.singleton(link);
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this arpeggiato instance and a HeadChord nearby.
     *
     * @param systemHeadChords ordered collection of head chords in system
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemHeadChords,
                             SystemInfo system)
    {
        // Look for a head-chord on right side of this symbol
        // Use a lookup box (glyph height, predefined width)
        // For intersected head-chords, measure y overlap WRT glyph height
        Rectangle luBox = getBounds();
        luBox.x += luBox.width;
        luBox.width = system.getSheet().getScale().toPixels(constants.areaDx);

        final List<Inter> chords = Inters.intersectedInters(
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

        return new Link(bestChord, rel, false);
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
