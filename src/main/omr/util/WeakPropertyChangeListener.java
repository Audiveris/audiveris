//----------------------------------------------------------------------------//
//                                                                            //
//            W e a k P r o p e r t y C h a n g e L i s t e n e r             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * Class {@code WeakPropertyChangeListener} is a wrapper meant to
 * weakly reference a concrete PropertyChangeListener, and thus avoid
 * memory leaks.
 *
 * @author Hervé Bitteur
 */
public class WeakPropertyChangeListener
        implements PropertyChangeListener
{
    //~ Instance fields --------------------------------------------------------

    /** The concrete listener */
    protected final WeakReference<PropertyChangeListener> weakListener;

    //~ Constructors -----------------------------------------------------------
    //----------------------------//
    // WeakPropertyChangeListener //
    //----------------------------//
    /**
     * Creates a new WeakPropertyChangeListener object from a concrete listener
     *
     * @param listener the concrete listener to weakly reference
     */
    public WeakPropertyChangeListener (PropertyChangeListener listener)
    {
        weakListener = new WeakReference<>(listener);
    }

    //~ Methods ----------------------------------------------------------------
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
