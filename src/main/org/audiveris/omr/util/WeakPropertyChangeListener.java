//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                      W e a k P r o p e r t y C h a n g e L i s t e n e r                       //
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
package org.audiveris.omr.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * Class {@code WeakPropertyChangeListener} is a wrapper meant to weakly reference a
 * concrete PropertyChangeListener, and thus avoid memory leaks.
 *
 * @author Hervé Bitteur
 */
public class WeakPropertyChangeListener
        implements PropertyChangeListener
{

    /** The concrete listener */
    protected final WeakReference<PropertyChangeListener> weakListener;

    /**
     * Creates a new WeakPropertyChangeListener object from a concrete listener
     *
     * @param listener the concrete listener to weakly reference
     */
    public WeakPropertyChangeListener (PropertyChangeListener listener)
    {
        weakListener = new WeakReference<PropertyChangeListener>(listener);
    }

    //----------------//
    // propertyChange //
    //----------------//
    /**
     * Delegate the call-back to the concrete listener, if still there
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        PropertyChangeListener listener = weakListener.get();

        if (listener != null) {
            listener.propertyChange(evt);
        }
    }
}
