//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r A c t i o n                                     //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;

/**
 * Class {@code InterAction} is the base for Inter actions, with or without relations.
 *
 * @author Hervé Bitteur
 */
public class InterAction
        extends AbstractAction
{

    private static final Logger logger = LoggerFactory.getLogger(InterAction.class);

    /** The underlying interpretation. */
    private final Inter inter;

    /**
     * Creates a new InterAction object.
     *
     * @param inter the underlying inter
     */
    public InterAction (Inter inter)
    {
        this(inter, null);
    }

    /**
     * Creates a new InterAction object.
     *
     * @param inter the underlying inter
     * @param text  specific item text, if any
     */
    public InterAction (Inter inter,
                        String text)
    {
        this.inter = inter;

        Shape shape = inter.getShape();
        putValue(NAME, (text != null) ? text : inter.toString());

        if (shape != null) {
            putValue(SMALL_ICON, shape.getDecoratedSymbol());
        }

        final String details = inter.getDetails();

        if (!details.isEmpty()) {
            putValue(SHORT_DESCRIPTION, details);
        }
    }

    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        logger.info(inter.toString());

        publish();
    }

    //---------//
    // publish //
    //---------//
    public void publish ()
    {
        final SIGraph sig = inter.getSig();
        sig.publish(inter);
    }
}
