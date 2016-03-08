//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     Z o o m A s s e m b l y                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import java.awt.BorderLayout;
import omr.ui.util.Panel;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Class {@code ZoomAssembly} is a UI assembly of a zoom slider plus a view.
 *
 * @author Hervé Bitteur
 */
public class ZoomAssembly
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The concrete UI component. */
    protected final Panel component = new Panel();

    /** To manually control the zoom ratio. */
    protected final LogSlider slider = new LogSlider(2, 5, LogSlider.VERTICAL, -3, 5, 0);

    /** Zoom, with default ratio set to 1. */
    protected final Zoom zoom = new Zoom(slider, 1);

    /** Mouse adapter. */
    protected final Rubber rubber = new Rubber(zoom);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ZoomAssembly} object.
     */
    public ZoomAssembly ()
    {
        defineLayout();

        // Avoid slider to react on (and consume) page up/down keys or arrow keys
        InputMap inputMap = slider.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "none");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "none");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "none");
        inputMap.put(KeyStroke.getKeyStroke("UP"), "none");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "none");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of this assembly.
     */
    private void defineLayout ()
    {
        component.setLayout(new BorderLayout());
        component.setNoInsets();
        component.add(slider, BorderLayout.WEST);
    }
}
