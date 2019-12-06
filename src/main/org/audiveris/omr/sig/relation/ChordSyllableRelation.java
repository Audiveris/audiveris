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

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;

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
        if (isManual()) {
            // Discard any competing syllable
            final HeadChordInter chord = (HeadChordInter) e.getEdgeSource();
            final LyricItemInter item = (LyricItemInter) e.getEdgeTarget();
            final SIGraph sig = chord.getSig();
            final LyricLineInter line = (LyricLineInter) item.getEnsemble();

            for (Relation rel : sig.getRelations(chord, ChordSyllableRelation.class)) {
                LyricItemInter other = (LyricItemInter) sig.getOppositeInter(chord, rel);

                if ((other != item) && (other.getEnsemble() == line)) {
                    logger.info("{} preferred to {} in chord-lyric link.", item, other);
                    sig.removeEdge(rel);
                }
            }
        }
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return true;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        // A chord can be linked to several lyric items, from different lyric lines.
        return false;
    }

    @Override
    public Object clone ()
            throws CloneNotSupportedException
    {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }
}
