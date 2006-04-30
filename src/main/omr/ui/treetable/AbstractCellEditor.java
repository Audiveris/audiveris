//      $Id$
package omr.ui.treetable;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import java.util.EventObject;

public class AbstractCellEditor
    implements CellEditor
{
    //~ Instance variables ---------------------------------------------------

    protected EventListenerList listenerList = new EventListenerList();

    //~ Methods --------------------------------------------------------------

    public boolean isCellEditable (EventObject e)
    {
        return true;
    }

    public Object getCellEditorValue ()
    {
        return null;
    }

    public void addCellEditorListener (CellEditorListener l)
    {
        listenerList.add(CellEditorListener.class, l);
    }

    public void cancelCellEditing ()
    {
    }

    public void removeCellEditorListener (CellEditorListener l)
    {
        listenerList.remove(CellEditorListener.class, l);
    }

    public boolean shouldSelectCell (EventObject anEvent)
    {
        return false;
    }

    public boolean stopCellEditing ()
    {
        return true;
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type.
     * @see EventListenerList
     */
    protected void fireEditingCanceled ()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                ((CellEditorListener) listeners[i + 1]).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type.
     * @see EventListenerList
     */
    protected void fireEditingStopped ()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                ((CellEditorListener) listeners[i + 1]).editingStopped(new ChangeEvent(this));
            }
        }
    }
}
