//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     M e m o r y M e t e r                                      //
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
package org.audiveris.omr.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.Memory;

import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Class {@code MemoryMeter} encapsulates the display of a linear memory meter in MB
 * (both used and total), together with a garbage-collection button.
 * <P>
 * There is a alarm threshold that triggers a color switch to red whenever the used memory exceeds
 * the threshold.
 *
 * @author Hervé Bitteur
 */
public class MemoryMeter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** A mega as 2**20 */
    private static final double MEGA = 1_024 * 1_024;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Default foreground color, when under alarm threshold */
    private Color defaultForeground;

    /** Related concrete component */
    private JPanel component;

    /** Progress bar */
    private JProgressBar progressBar = new JProgressBar();

    /** Runnable that displays the memory usage */
    private Runnable displayer;

    /** Flag on monitoring activity */
    private volatile boolean monitoring;

    /** Last value for global memory, in order to save on display */
    private int lastTotal = 0;

    /** Last value for used memory, in order to save on display */
    private int lastUsed = 0;

    /** Last value for threshold, in order to save on display */
    private int lastThreshold = 0;

    //~ Constructors -------------------------------------------------------------------------------
    //-------------//
    // MemoryMeter //
    //-------------//
    /**
     * Basic Memory Meter, with default alarm threshold and display period.
     */
    public MemoryMeter ()
    {
        component = new JPanel();

        try {
            defineUI();
            initialize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // collectGarbage //
    //----------------//
    /**
     * Manually trigger garbage collection.
     */
    @Action
    public void collectGarbage ()
    {
        System.gc();
        System.runFinalization();
        System.gc();
        displayMemory();
    }

    //---------------//
    // displayMemory //
    //---------------//
    /**
     * Trigger an immediate memory display
     */
    public void displayMemory ()
    {
        SwingUtilities.invokeLater(displayer);
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //------//
    // stop //
    //------//
    /**
     * Stop the memory monitoring
     */
    public void stop ()
    {
        monitoring = false;
    }

    //----------//
    // defineUI //
    //----------//
    private void defineUI ()
    {
        ApplicationContext applicationContext = OmrGui.getApplication().getContext();
        component.setLayout(new BorderLayout());

        // Progress bar
        progressBar.setPreferredSize(new Dimension(UIUtil.adjustedSize(90), 20));
        progressBar.setName("progressBar");
        progressBar.setToolTipText("Used memory / Global memory");
        progressBar.setStringPainted(true);
        component.add(progressBar, BorderLayout.CENTER);

        // Garbage collector button
        JButton button = new JButton(applicationContext.getActionMap(this).get("collectGarbage"));
        button.setBorder(UIUtil.getToolBorder());
        component.add(button, BorderLayout.EAST);

        // Remember the default foreground color
        defaultForeground = progressBar.getForeground();

        // Resource injection
        ResourceMap resource = applicationContext.getResourceMap(getClass());
        resource.injectComponents(component);
    }

    //------------//
    // initialize //
    //------------//
    private void initialize ()
    {
        // Displayer
        displayer = () -> {
            int total = (int) Math.rint(Memory.total() / MEGA);
            int used = (int) Math.rint(Memory.occupied() / MEGA);
            int threshold = (int) Math.rint(constants.alarmThreshold.getValue() * total);

            if ((total != lastTotal) || (used != lastUsed) || (threshold != lastThreshold)) {
                progressBar.setMaximum(total);
                progressBar.setValue(used);
                progressBar.setString(String.format("%d/%d MB", used, total));
                lastTotal = total;
                lastUsed = used;
                lastThreshold = threshold;

                if (used > threshold) {
                    progressBar.setForeground(Color.red);
                } else {
                    progressBar.setForeground(defaultForeground);
                }
            }
        };

        // Monitoring thread
        Thread monitorThread = new Thread()
        {
            @Override
            public void run ()
            {
                monitoring = true;

                while (monitoring) {
                    displayMemory();

                    try {
                        sleep(constants.samplingPeriod.getValue());
                    } catch (InterruptedException ex1) {
                        monitoring = false;
                    }
                }
            }
        };

        monitorThread.setName(getClass().getName());
        monitorThread.setPriority(Thread.MIN_PRIORITY);
        monitorThread.start();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer samplingPeriod = new Constant.Integer(
                "MilliSeconds",
                2_000,
                "Memory display period");

        private final Constant.Ratio alarmThreshold = new Constant.Ratio(
                0.75,
                "Memory alarm threshold, expressed in ratio of total memory");
    }
}
