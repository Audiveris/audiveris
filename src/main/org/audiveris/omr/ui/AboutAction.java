//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      A b o u t A c t i o n                                     //
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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.OMR;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.text.tesseract.TesseractOCR;
import org.audiveris.omr.ui.util.BrowserLinkListener;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.UriUtil;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URI;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;

/**
 * Class {@code AboutAction} implements the About dialog.
 *
 * @author Hervé Bitteur
 */
public class AboutAction
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AboutAction.class);

    // Resource injection
    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(AboutAction.class);

    // Panel
    private JPanel aboutBox = null;

    //-----------------//
    // actionPerformed //
    //-----------------//
    public void actionPerformed (ActionEvent e)
    {
        if (aboutBox == null) {
            aboutBox = createAboutBox();
        }

        JOptionPane.showMessageDialog(
                OMR.gui.getFrame(),
                aboutBox,
                resources.getString("AboutDialog.title"),
                JOptionPane.INFORMATION_MESSAGE);

    }

    //----------------//
    // createAboutBox //
    //----------------//
    private JPanel createAboutBox ()
    {
        // Layout
        final StringBuilder rows = new StringBuilder("pref,10dlu,pref,5dlu");

        for (int i = 0; i < (Topic.values().length); i++) {
            rows.append(",pref,3dlu");
        }

        final FormLayout layout = new FormLayout("right:pref, 5dlu, pref", rows.toString());
        final Panel panel = new Panel();
        final PanelBuilder builder = new PanelBuilder(layout, panel);
        final CellConstraints cst = new CellConstraints();

        // Splash logo
        int iRow = 1;
        final URI uri = UriUtil.toURI(WellKnowns.RES_URI, "splash.png");

        try {
            JPanel logoPanel = new ImagePanel(uri);
            builder.add(logoPanel, cst.xyw(1, iRow, 3));
        } catch (MalformedURLException ex) {
            logger.warn("Error on " + uri, ex);
        }

        // Software title (Audiveris)
        iRow += 2;
        final JLabel titleLabel = new JLabel();
        titleLabel.setFont(new Font("Arial",
                                    Font.BOLD,
                                    UIUtil.adjustedSize(constants.titleFontSize.getValue())));
        titleLabel.setName("aboutTitleLabel");
        builder.add(titleLabel, cst.xyw(1, iRow, 3));

        // Each topic in turn (description, version, etc)
        final HyperlinkListener linkListener = new BrowserLinkListener();

        for (Topic topic : Topic.values()) {
            iRow += 2;

            // Label on left
            final JLabel label = new JLabel();
            label.setName(topic + "Label");
            builder.add(label, cst.xy(1, iRow));

            // Content on right
            topic.comp.setName(topic + "TextField");
            if (topic.comp instanceof JTextComponent) {
                ((JTextComponent) topic.comp).setEditable(false);
            }

            if (topic.comp instanceof JEditorPane) {
                ((JEditorPane) topic.comp).addHyperlinkListener(linkListener);
            } else {
                topic.comp.setFocusable(false);
            }

            builder.add(topic.comp, cst.xy(3, iRow));
        }

        panel.setInsets(10, 10, 10, 10);
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setName("AboutPanel");

        // Manual injection
        resources.injectComponents(panel);

        ((JLabel) Topic.version.comp)
                .setText(WellKnowns.TOOL_REF + ":" + WellKnowns.TOOL_BUILD);

        ((JLabel) Topic.classes.comp)
                .setText(WellKnowns.CLASS_CONTAINER.toString());

        String homePage = resources.getString("Application.homepage");
        ((JTextComponent) Topic.home.comp).setText(UIUtil.htmlLink(homePage));

        String projectPage = resources.getString("Application.projectpage");
        ((JTextComponent) Topic.project.comp).setText(UIUtil.htmlLink(projectPage));

        ((JLabel) Topic.license.comp)
                .setText("GNU Affero GPL v3");

        ((JLabel) Topic.ocr.comp)
                .setText(TesseractOCR.getInstance().identify());

        ((JLabel) Topic.javaVendor.comp)
                .setText(System.getProperty("java.vendor"));

        ((JLabel) Topic.javaVersion.comp)
                .setText(System.getProperty("java.version"));

        ((JLabel) Topic.javaRuntime.comp)
                .setText(System.getProperty("java.runtime.name")
                                 + " (build " + System.getProperty("java.runtime.version") + ")");

        ((JLabel) Topic.javaVm.comp)
                .setText(System.getProperty("java.vm.name") + " (build "
                                 + System.getProperty("java.vm.version") + ", "
                                 + System.getProperty("java.vm.info") + ")");

        ((JLabel) Topic.os.comp)
                .setText(System.getProperty("os.name") + " " + System.getProperty("os.version"));

        ((JLabel) Topic.osArch.comp)
                .setText(System.getProperty("os.arch"));

        return panel;
    }

    //-------//
    // Topic //
    //-------//
    private static enum Topic
    {
        /** Longer application description */
        description(new JLabel()),
        /** Current version */
        version(new JLabel()),
        /** Precise classes */
        classes(new JLabel()),
        /** Link to web site */
        home(new JEditorPane("text/html", "")),
        /** Link to project site */
        project(new JEditorPane("text/html", "")),
        /** License */
        license(new JLabel()),
        /** OCR version */
        ocr(new JLabel()),
        /** Java vendor */
        javaVendor(new JLabel()),
        /** Java version */
        javaVersion(new JLabel()),
        /** Java runtime */
        javaRuntime(new JLabel()),
        /** Java VM */
        javaVm(new JLabel()),
        /** OS */
        os(new JLabel()),
        /** Arch */
        osArch(new JLabel());

        public final JComponent comp;

        Topic (JComponent comp)
        {
            this.comp = comp;
        }
    }

    //------------//
    // ImagePanel //
    //------------//
    private static class ImagePanel
            extends JPanel
    {

        private Image img;

        ImagePanel (Image img)
        {
            this.img = img;
            Dimension size = new Dimension(1 + img.getWidth(null),
                                           1 + img.getHeight(null));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
            setLayout(null);
        }

        ImagePanel (URI uri)
                throws MalformedURLException
        {
            this(new ImageIcon(uri.toURL()).getImage());
        }

        @Override
        public void paintComponent (Graphics g)
        {
            g.drawImage(img, 1, 1, null);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer titleFontSize = new Constant.Integer(
                "Points",
                14,
                "Font size for title in about box");

        private final Constant.Integer urlFontSize = new Constant.Integer(
                "Points",
                10,
                "Font size for URL in about box");
    }
}
