//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              R e l a t i o n C l a s s A c t i o n                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Relations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Class {@code RelationClassAction} displays the name of a relation class and allows to
 * create the relation instance between provided source and target inter instances.
 *
 * @author Hervé Bitteur
 */
public class RelationClassAction
        extends AbstractAction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            RelationClassAction.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Source inter. */
    private final Inter source;

    /** Target inter. */
    private final Inter target;

    /** Class of relation. */
    private final Class<? extends Relation> relationClass;

    private final Inter focus;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RelationClassAction} object.
     *
     * @param source        provided source inter
     * @param target        provided target inter
     * @param relationClass class of relation to create
     * @param focus         the inter instance which has focus
     */
    public RelationClassAction (Inter source,
                                Inter target,
                                Class<? extends Relation> relationClass,
                                Inter focus)
    {
        this.source = source;
        this.target = target;
        this.relationClass = relationClass;
        this.focus = focus;

        putValue(NAME, "lnk " + Relations.nameOf(relationClass));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void actionPerformed (ActionEvent e)
    {
        try {
            Relation relation = relationClass.newInstance();
            SIGraph sig = source.getSig();
            Sheet sheet = sig.getSystem().getSheet();
            InterController interController = sheet.getInterController();
            interController.link(sig, source, target, relation);
        } catch (Exception ex) {
            logger.error("Error allocating {}", relationClass, ex);
        }
    }

    /**
     * Visualize the potential relation between source and target inters.
     */
    public void publish ()
    {
        if (focus != null) {
            focus.getSig().publish(focus);

            SIGraph sig = source.getSig();
            Sheet sheet = sig.getSystem().getSheet();
            InterController interController = sheet.getInterController();
            interController.setRelationClassAction(this);
        }
    }

    public void unpublish ()
    {
        SIGraph sig = source.getSig();
        Sheet sheet = sig.getSystem().getSheet();
        InterController interController = sheet.getInterController();
        interController.setRelationClassAction(null);
    }

    /**
     * @return the source
     */
    public Inter getSource ()
    {
        return source;
    }

    /**
     * @return the target
     */
    public Inter getTarget ()
    {
        return target;
    }

    /**
     * @return the relationClass
     */
    public Class<? extends Relation> getRelationClass ()
    {
        return relationClass;
    }
}
