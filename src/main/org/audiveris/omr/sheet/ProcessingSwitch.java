//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P r o c e s s i n g S w i t c h                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;

/**
 * Class <code>ProcessingSwitch</code> is the enumeration of all possible processing
 * switches.
 * <p>
 * Every switch represents a boolean value that the user can modify.
 * The effective boolean value for a given switch complies with the following hierarchy of
 * assignments (from weakest to strongest):
 * <ol>
 * <li>Default value as written in Java source
 * <li>Value as modified by the user for <b>global</b> scope.
 * Switch value is persisted in user <code>config/run.properties</code> file
 * <li>Value as modified by the user for <b>book</b> scope.
 * Switch value is persisted in the <code>omr.xml</code> file
 * within <code>&lt;book&gt;.omr</code> project archive
 * <li>Value as modified by the user for <b>sheet</b> scope.
 * Switch value is persisted in a <code>sheet#N.xml</code> file
 * within <code>&lt;book&gt;.omr</code> project archive
 * </ol>
 */
public enum ProcessingSwitch
{
    poorInputMode(ProcessingSwitches.constants.poorInputMode),
    indentations(ProcessingSwitches.constants.indentations),
    bothSharedHeadDots(ProcessingSwitches.constants.bothSharedHeadDots),
    keepGrayImages(ProcessingSwitches.constants.keepGrayImages),
    articulations(ProcessingSwitches.constants.articulations),
    chordNames(ProcessingSwitches.constants.chordNames),
    fingerings(ProcessingSwitches.constants.fingerings),
    frets(ProcessingSwitches.constants.frets),
    pluckings(ProcessingSwitches.constants.pluckings),
    lyrics(ProcessingSwitches.constants.lyrics),
    lyricsAboveStaff(ProcessingSwitches.constants.lyricsAboveStaff),
    smallBlackHeads(ProcessingSwitches.constants.smallBlackHeads),
    smallVoidHeads(ProcessingSwitches.constants.smallVoidHeads),
    smallWholeHeads(ProcessingSwitches.constants.smallWholeHeads),
    crossHeads(ProcessingSwitches.constants.crossHeads),
    implicitTuplets(ProcessingSwitches.constants.implicitTuplets),
    sixStringTablatures(ProcessingSwitches.constants.sixStringTablatures),
    fourStringTablatures(ProcessingSwitches.constants.fourStringTablatures),
    oneLineStaves(ProcessingSwitches.constants.oneLineStaves),
    partialWholeRests(ProcessingSwitches.constants.partialWholeRests),
    multiWholeHeadChords(ProcessingSwitches.constants.multiWholeHeadChords);

    /** Underlying boolean constant. */
    private final Constant.Boolean constant;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a switch from its dedicated application constant.
     *
     * @param constant the related backing constant
     */
    ProcessingSwitch (Constant.Boolean constant)
    {
        this.constant = constant;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getConstant //
    //-------------//
    /**
     * Report the backing constant for this switch.
     *
     * @return the underlying application constant
     */
    public Constant.Boolean getConstant ()
    {
        return constant;
    }
}
