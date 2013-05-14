//----------------------------------------------------------------------------//
//                                                                            //
//                        F o l d e r S e l e c t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class {@code FolderSelector} displays the default install folder,
 * and let the user choose another one, if any.
 * 
 * NOTA: THIS CLASS IS NOT USED FOR THE TIME BEING
 * Since the notion of installation folder is not clear!
 * - With Java Web Start, appli "program data" is located in Java cache
 * - On Linux, appli "user config" and appli "user data" do not have the same parent
 *
 * @author Hervé Bitteur
 */
@Deprecated
public class FolderSelector
        implements ActionListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FolderSelector.class);

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    //~ Instance fields --------------------------------------------------------
    /** The containing bundle. */
    private final Bundle bundle;

    /** The swing component. */
    private JComponent component;

    /** Text field for folder path. */
    private JTextField path = new JTextField();

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // FolderSelector //
    //----------------//
    /**
     * Creates a new FolderSelector object.
     */
    public FolderSelector (Bundle bundle)
    {
        this.bundle = bundle;

        component = defineLayout();

        ///////path.setText(descriptor.getInstallFolder().getAbsolutePath());
        path.addActionListener(this);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Triggered from path JTextField.
     *
     * @param e the action event
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        logger.debug("Got event {}", e);
        checkFolder(new File(path.getText()));
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * @return the component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //-------------//
    // checkFolder //
    //-------------//
    /**
     * Make sure the provided candidate is OK
     *
     * @param file the candidate folder
     */
    private void checkFolder (File candidate)
    {
        try {
            if (!candidate.exists()) {
                // Make sure all directories are created
                if (candidate.mkdirs()) {
                    logger.info("Created folder {}", candidate.getAbsolutePath());
                }
            } else {
                if (!candidate.isDirectory()) {
                    throw new IllegalStateException("Not a directory");
                }
            }

            // All tests are OK
            path.setText(candidate.getAbsolutePath());
            ////////bundle.setInstallFolder(candidate);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    Installer.getFrame(),
                    "Invalid folder: " + ex,
                    "Folder selection",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    //--------------//
    // defineLayout //
    private JComponent defineLayout ()
    {
        final JPanel comp = new JPanel();
        final FormLayout layout = new FormLayout(
                "right:36dlu, $lcgap, fill:0:grow, $lcgap, 31dlu",
                "pref");
        final CellConstraints cst = new CellConstraints();
        final PanelBuilder builder = new PanelBuilder(layout, comp);

        // Label on left side
        builder.addROLabel("Folder", cst.xy(1, 1));

        // Path to folder
        builder.add(path, cst.xy(3, 1));

        // "Select" button on left side
        builder.add(new JButton(new BrowseAction()), cst.xy(5, 1));

        return comp;
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------------//
    // BrowseAction //
    //--------------//
    private class BrowseAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public BrowseAction ()
        {
            putValue(AbstractAction.NAME, "Select");
            putValue(
                    AbstractAction.SHORT_DESCRIPTION,
                    "Select another installation folder");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
//            // We always launch browsing from default installation folder
//            JFileChooser chooser = new JFileChooser(
//                    descriptor.getInstallFolder());
//            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//
//            int opt = chooser.showDialog(
//                    Installer.getFrame(),
//                    "Select install folder");
//
//            if (opt == JFileChooser.APPROVE_OPTION) {
//                checkFolder(chooser.getSelectedFile());
//            }
        }
    }
}
