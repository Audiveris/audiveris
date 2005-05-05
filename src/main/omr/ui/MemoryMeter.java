//-----------------------------------------------------------------------//
//                                                                       //
//                         M e m o r y M e t e r                         //
//                                                                       //
//-----------------------------------------------------------------------//
//      $Id$

/*
 * Company:     Auditop SA.
 *                      http://www.auditop.com
 */
package omr.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;

/**
 * A memory meter panel that displays used/total memory allocated to the
 * JVM and allows for running the garbage collector (this file was
 * initially written by Auditop, it is to be re-written someday)
 */
public class MemoryMeter
    extends JPanel
{
    //~ Static variables/initializers -------------------------------------

    /**
     * Default display interval in millis
     */
    public static int DEFAULT_DISPLAY_INTERVAL = 2000;

    /**
     * Default alarm threshold
     */
    public static int MAX_MEMORY_ALARM_THRESHOLD = 49152;

    //~ Instance variables ------------------------------------------------

    /**
     * Description of the Field
     */
    private JProgressBar pbMemory = new JProgressBar();

    /**
     * Description of the Field
     */
    private JButton btnGC = new JButton();

    /**
     * display interval in millis
     */
    private int m_displayInterval = DEFAULT_DISPLAY_INTERVAL;
    private int m_alarmThreshold = MAX_MEMORY_ALARM_THRESHOLD;

    /**
     * Last total memory detected, used to optimize the display
     */
    private int m_lastTotalKb = 0;

    /**
     * Last used memory detected, used to optimize the display
     */
    private int m_lastUsedKb = 0;

    /**
     * Thread for displaying the memory usage
     */
    private Thread m_memoryDisplayThread;

    /**
     * Runnable that displays the memory usage
     */
    private Runnable m_memoryDisplayer;

    /**
     * True if currently displaying memory usage
     */
    private volatile boolean m_displayingMemory;
    private boolean m_displayActive = false;
    private Color m_defaultForeColor;

    //~ Constructors ------------------------------------------------------

    //-------------//
    // MemoryMeter //
    //-------------//
    /**
     * Constructor for the MemoryMeter object
     */
    public MemoryMeter ()
    {
        try {
            jbInit();
            initialize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods -----------------------------------------------------------

    //-------------------//
    // setAlarmThreshold //
    //-------------------//
    public void setAlarmThreshold (int newAlarmThreshold)
    {
        m_alarmThreshold = newAlarmThreshold;
    }

    //-------------------//
    // getAlarmThreshold //
    //-------------------//
    public int getAlarmThreshold ()
    {
        return m_alarmThreshold;
    }

    //--------------------//
    // setDisplayInterval //
    //--------------------//
    public void setDisplayInterval (int newDisplayInterval)
    {
        m_displayInterval = newDisplayInterval;
    }

    //--------------------//
    // getDisplayInterval //
    //--------------------//
    public int getDisplayInterval ()
    {
        return m_displayInterval;
    }

    //--------------------//
    // displayMemoryUsage //
    //--------------------//
    /**
     * Invokes the memory display through the Swing thread
     */
    public void displayMemoryUsage ()
    {
        SwingUtilities.invokeLater(m_memoryDisplayer);
    }

    //----------------//
    // stopDisplaying //
    //----------------//
    public void stopDisplaying ()
    {
        m_displayingMemory = false;
    }

    //-----------------------//
    // btnGC_actionPerformed //
    //-----------------------//
    void btnGC_actionPerformed (ActionEvent e)
    {
        System.gc();
        System.runFinalization();
        System.gc();
        displayMemoryUsage();
    }

    //------------//
    // initialize //
    //------------//
    /**
     * Initializes the memory display
     */
    void initialize ()
    {
        m_memoryDisplayer = new Runnable()
        {
            public void run ()
            {
                int totalKb = (int) Runtime.getRuntime().totalMemory() / 1024;
                int usedKb = (int) (Runtime.getRuntime().totalMemory()
                                    - Runtime.getRuntime().freeMemory()) / 1024;

                if ((totalKb != m_lastTotalKb)
                    || (usedKb != m_lastUsedKb)) {
                    pbMemory.setMaximum(totalKb);
                    pbMemory.setValue(usedKb);
                    pbMemory.setString("" + usedKb + "Kb / " + totalKb
                                       + "Kb");
                    m_lastTotalKb = totalKb;
                    m_lastUsedKb = usedKb;

                    if (usedKb >= getAlarmThreshold()) {
                        pbMemory.setForeground(Color.red);
                    } else {
                        pbMemory.setForeground(m_defaultForeColor);
                    }
                }

                m_displayActive = false;
            }
        };

        m_memoryDisplayThread = new Thread()
        {
            public void run ()
            {
                m_displayingMemory = true;

                while (m_displayingMemory) {
                    if (!m_displayActive) {
                        m_displayActive = true;
                        displayMemoryUsage();
                    }

                    try {
                        sleep(m_displayInterval);
                    } catch (InterruptedException ex1) {
                        m_displayingMemory = false;
                    }
                }
            }
        };

        m_memoryDisplayThread.setDaemon(true);
        m_memoryDisplayThread.setPriority(Thread.MIN_PRIORITY);
        m_memoryDisplayThread.start();
    }

    //--------//
    // jbInit //
    //--------//
    /**
     * Initializes the UI
     */
    void jbInit ()
    {
        this.setLayout(new GridBagLayout());

        pbMemory.setToolTipText("Used memory of total memory");
        pbMemory.setString("0Kb / 0Kb");
        pbMemory.setStringPainted(true);
        this.add(pbMemory,
                 new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets(0, 0, 0, 0), 0, 0));

        btnGC.setBorder(BorderFactory.createRaisedBevelBorder());
        btnGC.setToolTipText("Run Garbage Collector");

        //         try {
        //             URL iconUrl = new URL("file:///u:/soft/omr/lib/ico/Delete16.gif");
//         final URL iconUrl = MemoryMeter.class.getResource("/icons/Delete16.gif");
//         btnGC.setIcon(new ImageIcon(iconUrl));
        final URL iconUrl = MemoryMeter.class.getResource("/icons/Delete16.gif");
        btnGC.setIcon(IconUtil.buttonIconOf("general/Delete"));

        //         } catch (MalformedURLException ex) {
        //         }
        btnGC.setMargin(new Insets(0, 0, 0, 0));
        btnGC.addActionListener(new MemoryMeter_btnGC_actionAdapter(this));
        this.add(btnGC,
                 new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets(0, 0, 0, 0), 0, 0));

        m_defaultForeColor = pbMemory.getForeground();
    }

    //---------------------------------//
    // MemoryMeter_btnGC_actionAdapter //
    //---------------------------------//
    private class MemoryMeter_btnGC_actionAdapter
            implements java.awt.event.ActionListener
    {
        //~ Instance variables --------------------------------------------

        MemoryMeter adaptee;

        //~ Constructors --------------------------------------------------

        //---------------------------------//
        // MemoryMeter_btnGC_actionAdapter //
        //---------------------------------//
        MemoryMeter_btnGC_actionAdapter (MemoryMeter adaptee)
        {
            this.adaptee = adaptee;
        }

        //~ Methods -------------------------------------------------------

        //-----------------//
        // actionPerformed //
        //-----------------//
        public void actionPerformed (ActionEvent e)
        {
            adaptee.btnGC_actionPerformed(e);
        }
    }
}
