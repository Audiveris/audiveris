//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P l u c k i n g I n t e r                                   //
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
import org.audiveris.omr.sig.relation.HeadPluckingRelation;
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
 * Class <code>PluckingInter</code> represents the fingering for guitar right-hand.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "plucking")
public class PluckingInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PluckingInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The letter attribute is a char ('p', 'i', 'm' or 'a') that indicates the right-hand
     * finger to be used.
     */
    @XmlAttribute
    private final char letter;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private PluckingInter ()
    {
        this.letter = 0;
    }

    /**
     * Creates a new <code>PluckingInter</code> object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public PluckingInter (Glyph glyph,
                          Shape shape,
                          Double grade)
    {
        super(glyph, null, shape, grade);
        this.letter = valueOf(shape);
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
        setAbnormal(!sig.hasRelation(this, HeadPluckingRelation.class));

        return isAbnormal();
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, HeadPluckingRelation.class)) {
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
        return String.valueOf(letter);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + letter;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, HeadPluckingRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this plucking instance and a head in a HeadChord nearby.
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
                scale.toPixels(HeadPluckingRelation.getXOutGapMaximum(profile)),
                scale.toPixels(HeadPluckingRelation.getYGapMaximum(profile)));

        if (head == null) {
            return null;
        }

        return new Link(head, new HeadPluckingRelation(), false);
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
        return searchObsoletelinks(links, HeadPluckingRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid PluckingInter.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created plucking or null
     */
    public static PluckingInter createValidAdded (Glyph glyph,
                                                  Shape shape,
                                                  double grade,
                                                  SystemInfo system,
                                                  List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP PluckingInter create {} as {}", glyph, shape);
        }

        PluckingInter plucking = new PluckingInter(glyph, shape, grade);
        Link link = plucking.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(plucking);
            link.applyTo(plucking);

            return plucking;
        }

        return null;
    }

    //---------//
    // valueOf //
    //---------//
    public static char valueOf (OmrShape omrShape)
    {
        return switch (omrShape) {
            case fingeringPLower -> 'p';
            case fingeringILower -> 'i';
            case fingeringMLower -> 'm';
            case fingeringALower -> 'a';

            default -> throw new IllegalArgumentException("Invalid plucking shape " + omrShape);
        };
    }

    //---------//
    // valueOf //
    //---------//
    public static char valueOf (Shape shape)
    {
        return switch (shape) {
            case PLUCK_P -> 'p';
            case PLUCK_I -> 'i';
            case PLUCK_M -> 'm';
            case PLUCK_A -> 'a';

            default -> throw new IllegalArgumentException("Invalid plucking shape " + shape);
        };
    }
}
