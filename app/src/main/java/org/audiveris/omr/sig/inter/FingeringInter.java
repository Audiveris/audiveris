//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   F i n g e r i n g I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.HeadFingeringRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>FingeringInter</code> represents the fingering for guitar left-hand.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fingering")
public class FingeringInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FingeringInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Integer value for the number. (0, 1, 2, 3, 4) */
    @XmlAttribute
    private final int value;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private FingeringInter ()
    {
        this.value = 0;
    }

    /**
     * Creates a new FingeringInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public FingeringInter (Glyph glyph,
                           Shape shape,
                           Double grade)
    {
        super(glyph, null, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : (-1);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No head linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a note head is connected
        setAbnormal(!sig.hasRelation(this, HeadFingeringRelation.class));

        return isAbnormal();
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, HeadFingeringRelation.class)) {
                final HeadInter head = (HeadInter) sig.getOppositeInter(this, rel);

                return staff = head.getStaff();
            }
        }

        return staff;
    }

    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        return String.valueOf(value);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * @return the value
     */
    public int getValue ()
    {
        return value;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, HeadFingeringRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this fingering instance and a head in a HeadChord nearby.
     *
     * @param systemHeadChords abscissa-ordered collection of head chords in system
     * @param profile          desired profile level
     * @return the link found or null
     */
    protected Link lookupLink (List<Inter> systemHeadChords,
                               int profile)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final HeadInter head = TechnicalHelper.lookupHead(
                this,
                systemHeadChords,
                scale.toPixels(HeadFingeringRelation.getXOutGapMaximum(profile)),
                scale.toPixels(HeadFingeringRelation.getYGapMaximum(profile)));

        if (head == null) {
            return null;
        }

        return new Link(head, new HeadFingeringRelation(), false);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());
        final List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        Link link = lookupLink(systemHeadChords, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, HeadFingeringRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid FingeringInter.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created fingering or null
     */
    public static FingeringInter createValidAdded (Glyph glyph,
                                                   Shape shape,
                                                   double grade,
                                                   SystemInfo system,
                                                   List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP FingeringInter create {} as {}", glyph, shape);
        }

        FingeringInter fingering = new FingeringInter(glyph, shape, grade);
        Link link = fingering.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(fingering);
            link.applyTo(fingering);

            return fingering;
        }

        return null;
    }

    //---------//
    // valueOf //
    //---------//
    public static int valueOf (OmrShape omrShape)
    {
        return switch (omrShape) {
            case fingering0 -> 0;
            case fingering1 -> 1;
            case fingering2 -> 2;
            case fingering3 -> 3;
            case fingering4 -> 4;
            case fingering5 -> 5;

            default -> throw new IllegalArgumentException("No fingering value for " + omrShape);
        };
    }

    //---------//
    // valueOf //
    //---------//
    public static int valueOf (Shape shape)
    {
        return switch (shape) {
            case DIGIT_0 -> 0;
            case DIGIT_1 -> 1;
            case DIGIT_2 -> 2;
            case DIGIT_3 -> 3;
            case DIGIT_4 -> 4;
            case DIGIT_5 -> 5;

            default -> throw new IllegalArgumentException("No fingering value for " + shape);
        };
    }
}
