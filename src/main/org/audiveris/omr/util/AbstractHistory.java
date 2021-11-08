//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          H i s t o r y                                         //
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
package org.audiveris.omr.util;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.ui.view.HistoryMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionListener;
import java.nio.file.Path;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

/**
 * Class <code>AbstractHistory</code> handles a limited history of entities.
 *
 * @param <E> entity type
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractHistory<E>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractHistory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying list of names. */
    protected final NameSet nameSet;

    /** Name of last folder used, if any. */
    protected final Constant.String folderConstant;

    /** Related UI menu, if any. Null when no UI is used */
    protected HistoryMenu menu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>AbstractHistory</code> object.
     *
     * @param name           a name for this history instance
     * @param constant       backing constant on disk
     * @param folderConstant backing constant for last folder, or null
     * @param maxSize        maximum entities in history
     * @param predicate      predicate to test names equivalence
     */
    public AbstractHistory (String name,
                            Constant.String constant,
                            Constant.String folderConstant,
                            int maxSize,
                            PairPredicate<String> predicate)
    {
        nameSet = new NameSet(name, constant, maxSize, predicate);
        this.folderConstant = folderConstant;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // add //
    //-----//
    /**
     * Add an entity to history
     *
     * @param entity the entity to include
     */
    public void add (E entity)
    {
        nameSet.add(encode(entity));

        Path parent = getParent(entity);

        if ((parent != null) && (folderConstant != null)) {
            folderConstant.setStringValue(parent.toAbsolutePath().toString());
        }

        if (OMR.gui != null) {
            // Enable input history menu
            SwingUtilities.invokeLater(() -> menu.setEnabled(true));
        }
    }

    //----------//
    // feedMenu //
    //----------//
    /**
     * Populate a menu with path history.
     *
     * @param menu         menu to populate, if null it is allocated
     * @param itemListener listener for each menu item
     * @return the populated menu
     */
    public JMenu feedMenu (JMenu menu,
                           final ActionListener itemListener)
    {
        return nameSet.feedMenu(menu, itemListener);
    }

    //----------//
    // getFirst //
    //----------//
    /**
     * Report the first entity in history.
     *
     * @return first entity, null if none
     */
    public E getFirst ()
    {
        final String first = nameSet.first();

        return (first != null) ? decode(first) : null;
    }

    //---------//
    // isEmpty //
    //---------//
    /**
     * Tell whether history is empty.
     *
     * @return true if so
     */
    public boolean isEmpty ()
    {
        return nameSet.isEmpty();
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove an entity from history
     *
     * @param entity to be removed
     * @return true if actually removed
     */
    public boolean remove (E entity)
    {
        return nameSet.remove(encode(entity));
    }

    //---------//
    // setMenu //
    //---------//
    /**
     * Set the related UI menu
     *
     * @param menu the related menu
     */
    public void setMenu (HistoryMenu menu)
    {
        this.menu = menu;
    }

    /**
     * Report the parent folder for this entity.
     *
     * @param entity provided entity
     * @return parent folder or null
     */
    protected abstract Path getParent (E entity);

    /**
     * Report the storing string representation of the provided entity.
     *
     * @param entity provided entity
     * @return string representation
     */
    protected abstract String encode (E entity);

    /**
     * Decode the provided string representation as an entity.
     *
     * @param str provided representation
     * @return the related entity
     */
    protected abstract E decode (String str);
}
