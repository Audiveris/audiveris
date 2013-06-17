//----------------------------------------------------------------------------//
//                                                                            //
//                                 U I U t i l                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur and Brenton Partridge 2000-2013.              //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

/**
 * Class {@code UIUtil} gathers utilities related to User Interface
 *
 * @author Hervé Bitteur and Brenton Partridge
 */
public class UIUtil
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            UIUtil.class);

    /**
     * Customized border for tool buttons, to use consistently in all UI
     * components.
     */
    private static Border toolBorder;

    public static final WindowListener closeWindow = new WindowAdapter()
    {
        @Override
        public void windowClosing (WindowEvent e)
        {
            e.getWindow()
                    .dispose();
        }
    };

    //~ Constructors -----------------------------------------------------------
    private UIUtil ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------------------//
    // directoryChooser //
    //------------------//
    /**
     * Let the user select a directory.
     *
     * @param parent   the parent component for the dialog
     * @param startDir the default directory
     * @return the chosen directory, or null
     */
    public static File directoryChooser (Component parent,
                                         File startDir)
    {
        String oldMacProperty = System.getProperty(
                "apple.awt.fileDialogForDirectories",
                "false");
        System.setProperty("apple.awt.fileDialogForDirectories", "true");

        OmrFileFilter directoryFilter = new OmrFileFilter(
                "directory",
                new String[]{})
        {
            @Override
            public boolean accept (File f)
            {
                return (f.isDirectory());
            }
        };

        File file = fileChooser(false, parent, startDir, directoryFilter);
        System.setProperty(
                "apple.awt.fileDialogForDirectories",
                oldMacProperty);

        return file;
    }

    //---------------//
    // enableActions //
    //---------------//
    /**
     * Given a list of actions, set all these actions (whether they
     * derive from AbstractAction or AbstractButton) enabled or not,
     * according to the bool parameter provided.
     *
     * @param actions list of actions to enable/disable as a whole
     * @param bool    true for enable, false for disable
     */
    public static void enableActions (Collection<?> actions,
                                      boolean bool)
    {
        for (Iterator<?> it = actions.iterator(); it.hasNext();) {
            Object next = it.next();

            if (next instanceof AbstractAction) {
                ((AbstractAction) next).setEnabled(bool);
            } else if (next instanceof AbstractButton) {
                ((AbstractButton) next).setEnabled(bool);
            } else {
                logger.warn("Neither Button nor Action : {}", next);
            }
        }
    }

    //-------------//
    // fileChooser //
    //-------------//
    /**
     * A replacement for standard JFileChooser, to allow better
     * look & feel on the Mac platform.
     *
     * @param save      true for a SAVE dialog, false for a LOAD dialog
     * @param parent    the parent component for the dialog
     * @param startFile default file, or just default directory, or null
     * @param filter    a filter to by applied on files
     * @return the selected file, or null
     */
    public static File fileChooser (boolean save,
                                    Component parent,
                                    File startFile,
                                    OmrFileFilter filter)
    {
        File file = null;

        if (WellKnowns.MAC_OS_X) {
            if ((parent == null) && (omr.Main.getGui() != null)) {
                parent = omr.Main.getGui()
                        .getFrame();
            }

            Component parentFrame = parent;

            while (!(parentFrame instanceof Frame)
                   && (parentFrame.getParent() != null)) {
                parentFrame = parentFrame.getParent();
            }

            try {
                final FileDialog fd = new FileDialog((Frame) parentFrame);

                if (startFile != null) {
                    fd.setDirectory(
                            startFile.isDirectory() ? startFile.getPath()
                            : startFile.getParent());
                }

                fd.setMode(save ? FileDialog.SAVE : FileDialog.LOAD);
                fd.setFilenameFilter(filter);

                String title = save ? "Saving: " : "Loading: ";
                title += filter.getDescription();
                fd.setTitle(title);

                fd.setVisible(true);

                String fileName = fd.getFile();
                String dir = fd.getDirectory();

                if ((dir != null) && (fileName != null)) {
                    String fullName = dir + WellKnowns.FILE_SEPARATOR
                                      + fileName;
                    file = new File(fullName);
                }
            } catch (ClassCastException e) {
                logger.warn("no ancestor is Frame");
            }
        } else {
            ///final JFileChooser fc = new JFileChooser();
            // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6317789
            final JFileChooser fc = new JFileChooser()
            {
                @Override
                public void updateUI ()
                {
                    putClientProperty("FileChooser.useShellFolder", false);
                    super.updateUI();
                }
            };

            // Pre-select the directory, and potentially the file to save to
            if (startFile != null) {
                if (startFile.isDirectory()) {
                    fc.setCurrentDirectory(startFile);
                } else {
                    File parentFile = startFile.getParentFile();
                    fc.setCurrentDirectory(parentFile);
                    fc.setSelectedFile(startFile);
                }
            }

            fc.addChoosableFileFilter(filter);

            int result = save ? fc.showSaveDialog(parent)
                    : fc.showOpenDialog(parent);

            if (result == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
            }
        }

        return file;
    }

    //---------------//
    // getToolBorder //
    //---------------//
    /**
     * Report a standard tool border entity, which is a blank border.
     *
     * @return the standard tool border
     */
    public static Border getToolBorder ()
    {
        if (toolBorder == null) {
            toolBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
        }

        return toolBorder;
    }

    //-------------------//
    // setAbsoluteStroke //
    //-------------------//
    /**
     * Whatever the current scaling of a graphic context, set the stroke
     * to theesired absolute width, and return the saved stroke for
     * later restore.
     *
     * @param g     the current graphics context
     * @param width the absolute stroke width desired
     * @return the previous stroke
     */
    public static Stroke setAbsoluteStroke (Graphics g,
                                            float width)
    {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform AT = g2.getTransform();
        double ratio = AT.getScaleX();
        Stroke oldStroke = g2.getStroke();
        Stroke stroke = new BasicStroke(width / (float) ratio);
        g2.setStroke(stroke);

        return oldStroke;
    }

    //-----------------//
    // suppressBorders //
    //-----------------//
    /**
     * Browse the whole hierarchy of components and nullify their
     * borders.
     */
    public static void suppressBorders (Container container)
    {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JComponent) {
                suppressBorders((JComponent) comp);
            } else if (comp instanceof Container) {
                suppressBorders((Container) comp);
            }
        }
    }

    //-----------------//
    // suppressBorders //
    //-----------------//
    /**
     * Nullify the border of this JComponent, as well as its
     * subcomponents.
     */
    public static void suppressBorders (JComponent comp)
    {
        if (!(comp instanceof JButton) && !(comp instanceof JToggleButton)) {
            comp.setBorder(null);
        }

        suppressBorders((Container) comp);
    }
}
