//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            C h o r d S y l l a b l e R e l a t i o n                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.relation;

import java.util.ArrayList;
import java.util.List;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UnlinkTask;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordSyllableRelation} represents a support relation between a chord
 * and a lyric item (of syllable kind).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "chord-syllable")
public class ChordSyllableRelation
        extends Support
{

    private static final Logger logger = LoggerFactory.getLogger(ChordSyllableRelation.class);

    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final HeadChordInter chord = (HeadChordInter) e.getEdgeSource();
        final LyricItemInter item = (LyricItemInter) e.getEdgeTarget();
        final boolean above = item.getCenter().y < chord.getCenter().y;
        final LyricLineInter line = (LyricLineInter) item.getEnsemble();
        final Part chordPart = chord.getPart();
        final Part linePart = line.getPart();
        final Staff chordStaff = above ? chord.getTopStaff() : chord.getBottomStaff();

        if (linePart != chordPart) {
            if (linePart != null) {
                linePart.removeLyric(line);
            }

            chordPart.addLyric(line);
            line.setStaff(chordStaff);

            for (Inter inter : line.getMembers()) {
                LyricItemInter it = (LyricItemInter) inter;
                it.setPart(null);
                it.setStaff(chordStaff);
            }

            // Re-numbering of lyric lines
            chordPart.sortLyricLines();
            chord.getSig().getSystem().numberLyricLines();
        }

        item.checkAbnormal();
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        // Just one chord can be linked to a given syllable.
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        // A chord can be linked to several syllables (from different lyric verses).
        return false;
    }

    @Override
    public Object clone ()
            throws CloneNotSupportedException
    {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    //---------//
    // preLink //
    //---------//
    @Override
    public List<? extends UITask> preLink (RelationPair pair)
    {
        final List<UITask> tasks = new ArrayList<>();
        final HeadChordInter chord = (HeadChordInter) pair.source;
        final LyricItemInter item = (LyricItemInter) pair.target;
        final LyricLineInter line = (LyricLineInter) item.getEnsemble();

        // Discard any competing syllable
        final SIGraph sig = chord.getSig();
        final SystemInfo system = sig.getSystem();

        for (Relation rel : sig.getRelations(chord, ChordSyllableRelation.class)) {
            final LyricItemInter other = (LyricItemInter) sig.getOppositeInter(chord, rel);

            if ((other != item) && (other.getEnsemble() == line)) {
                logger.info("{} preferred to {} in chord-syllable link.", item, other);
                tasks.add(new UnlinkTask(sig, rel));
            }
        }

        // If lyric item moves from one chord part to a different chord part,
        // then we have to switch all lyric items and lyric line as well
        if (line.getPart() != chord.getPart()) {
            final boolean above = item.getCenter().y < chord.getCenter().y;
            final Staff chordStaff = above ? chord.getTopStaff() : chord.getBottomStaff();

            for (Inter inter : line.getMembers()) {
                final LyricItemInter it = (LyricItemInter) inter;

                if ((it != item) && it.isSyllable()) {
                    final HeadChordInter ch = it.getHeadChord();

                    if ((ch != null) && (ch.getPart() != chord.getPart())) {
                        final Relation rel = sig.getRelation(ch, it, this.getClass());

                        if (rel != null) {
                            tasks.add(new UnlinkTask(sig, rel));
                        }
                    }

                    final Link link = it.lookupLink(chordStaff, null);

                    if (link != null) {
                        tasks.add(new LinkTask(sig, link.partner, it, link.relation));
                    }
                }
            }
        }

        return tasks;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final LyricItemInter item = (LyricItemInter) e.getEdgeTarget();

        if (!item.isRemoved()) {
            item.checkAbnormal();
        }
    }
}
