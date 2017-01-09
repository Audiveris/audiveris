//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A b s t r a c t I n t e r V i s i t o r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

/**
 * Class {@code AbstractInterVisitor} is a void implementation for {@link InterVisitor}
 * meant to ease the coding of concrete sub-classes.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractInterVisitor
        implements InterVisitor
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void visit (AbstractBeamInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (AbstractFlagInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (HeadInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BarConnectorInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BarlineInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BraceInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BracketConnectorInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (BracketInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (AbstractChordInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (ClefInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (EndingInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (Inter inter)
    {
        // Void by default
    }

    @Override
    public void visit (KeyAlterInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (KeyInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (LedgerInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (SentenceInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (SlurInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (StemInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (TimeWholeInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (TimePairInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (WedgeInter inter)
    {
        // Void by default
    }

    @Override
    public void visit (WordInter inter)
    {
        // Void by default
    }
}
