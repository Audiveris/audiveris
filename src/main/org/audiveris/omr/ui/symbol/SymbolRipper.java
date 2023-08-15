//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l R i p p e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.field.IntegerListSpinner;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LHexaSpinner;
import org.audiveris.omr.ui.field.LIntegerSpinner;
import org.audiveris.omr.ui.field.LSpinner;
import org.audiveris.omr.ui.field.SpinnerUtil;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UILookAndFeel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>SymbolRipper</code> is a stand-alone utility to generate the
 * textual description of a symbol.
 * <p>
 * Symbol appearance is "ripped" from a music font.
 *
 * @author Hervé Bitteur
 */
public class SymbolRipper
        extends SingleFrameApplication
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolRipper.class);

    /** Stand-alone run (vs part of Audiveris). */
    private static boolean standAlone = false;

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Related frame.
     * We need a frame rather than a dialog because this class can be run in standalone.
     */
    private JFrame frame;

    /** Image being built. */
    private BufferedImage image;

    private final ChangeListener paramListener = new ChangeListener()
    {
        @Override
        public void stateChanged (ChangeEvent e)
        {
            JSpinner s = (JSpinner) e.getSource();

            if (s == fontName.getSpinner()) {
                // New font name
                defineFont();
            } else if (s == fontBase) {
                // New font base
                changeCode();
            } else if (s == fontSize.getSpinner()) {
                // New font size
                defineFont();
            } else if (s == pointCode.getSpinner()) {
                // New point code
                changeCode();
            } else if (s == hexaCode.getSpinner()) {
                // New hexa point code
                changeHexaCode();
            } else if ((s == xOffset.getSpinner()) || (s == yOffset.getSpinner())) {
                // New drawing offset
            } else if ((s == width.getSpinner()) || (s == height.getSpinner())) {
                // New drawing dimension
                resizeDrawing();
            }

            // For all
            image = buildImage();
            frame.repaint();
        }
    };

    // Panel where the icon is drawn
    private final JPanel drawing;

    // String used to draw the symbol
    private String string;

    // Current music font
    private Font musicFont;

    // Font name
    private LSpinner fontName = new LSpinner("Font", "Name of the font");

    // Font base
    private IntegerListSpinner fontBase = new IntegerListSpinner();

    // Font size
    private LIntegerSpinner fontSize = new LIntegerSpinner("Size", "Font size in picas");

    // Point code
    private LIntegerSpinner pointCode = new LIntegerSpinner("Code", "Point code");

    // Hexa representation
    private LHexaSpinner hexaCode = new LHexaSpinner("Hexa", "Hexa value of the point code");

    // X Offset
    private LIntegerSpinner xOffset = new LIntegerSpinner("xOffset", "X offset");

    // Width
    private LIntegerSpinner width = new LIntegerSpinner("Width", "Drawing Width");

    // Y Offset
    private LIntegerSpinner yOffset = new LIntegerSpinner("yOffset", "Y offset");

    // Height
    private LIntegerSpinner height = new LIntegerSpinner("Height", "Drawing Height");

    // x symbol
    private final String f = "%.3f";

    private final LDoubleField xSym = new LDoubleField(false, "xSym", "x symbol", f);

    // w symbol
    private final LDoubleField wSym = new LDoubleField(false, "wSym", "w symbol", f);

    // y symbol
    private final LDoubleField ySym = new LDoubleField(false, "ySym", "y symbol", f);

    // y symbol
    private final LDoubleField hSym = new LDoubleField(false, "hSym", "h symbol", f);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SymbolRipper object.
     */
    public SymbolRipper ()
    {
        // Actors
        drawing=new Drawing();

        fontBase.setModel(new SpinnerListModel(new Integer[]{0,0xf000,0x1_d100}));SpinnerUtil.setRightAlignment(fontBase);SpinnerUtil.fixIntegerList(fontBase);

        fontName.setModel(new SpinnerListModel(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

        pointCode.setModel(new SpinnerNumberModel(0x1_d100,0,0x1_d1ff,1));

        // Initial values
        if(true){fontName.getSpinner().setValue("Bravura");fontBase.setValue(0); // (for Bravura)
        pointCode.setValue(0xE0A4); // Quarter note (in Bravura)
        }else{fontName.getSpinner().setValue("MusicalSymbols");fontBase.setValue(fontBase.getModel().getNextValue()); // (for MusicalSymbols)
        pointCode.setValue(113); // Quarter note (in MusicalSymbols)
        }

        fontSize.setValue(200);width.setValue(400);height.setValue(500);xOffset.setValue(200);yOffset.setValue(300);

        changeCode();defineFont();

        // Listeners
        fontName.addChangeListener(paramListener);fontBase.addChangeListener(paramListener);fontSize.addChangeListener(paramListener);pointCode.addChangeListener(paramListener);hexaCode.addChangeListener(paramListener);xOffset.addChangeListener(paramListener);yOffset.addChangeListener(paramListener);width.addChangeListener(paramListener);height.addChangeListener(paramListener);

        // Global layout
        if(!standAlone){frame=defineLayout(new JFrame("Symbol Ripper"));OmrGui.getApplication().show(frame);}
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // buildImage //
    //------------//
    private BufferedImage buildImage ()
    {
        BufferedImage img = (BufferedImage) drawing.createImage(
                width.getValue(),
                height.getValue());

        Graphics2D g2 = img.createGraphics();
        g2.setBackground(Color.white);
        g2.setColor(Color.white);
        g2.fillRect(0, 0, width.getValue(), height.getValue());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.black);
        g2.setFont(musicFont);
        g2.drawString(string, xOffset.getValue(), yOffset.getValue());

        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(string, musicFont, frc);
        Rectangle2D rect = layout.getBounds();
        xSym.setValue(rect.getX());
        ySym.setValue(rect.getY());
        wSym.setValue(rect.getWidth());
        hSym.setValue(rect.getHeight());

        return img;
    }

    //------------//
    // changeCode //
    //------------//
    private void changeCode ()
    {
        int base = (Integer) fontBase.getValue();
        int code = (Integer) pointCode.getSpinner().getValue();
        string = new String(Character.toChars(base + code));
        hexaCode.setValue(base + code);
    }

    //----------------//
    // changeHexaCode //
    //----------------//
    private void changeHexaCode ()
    {
        int base = (Integer) fontBase.getValue();
        int hexa = (Integer) hexaCode.getSpinner().getValue();
        string = new String(Character.toChars(hexa));
        pointCode.setValue(hexa - base);
    }

    //------------//
    // defineFont //
    //------------//
    private void defineFont ()
    {
        String name = (String) fontName.getSpinner().getValue();
        int val = fontSize.getValue();
        musicFont = new Font(name, Font.PLAIN, val);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private JFrame defineLayout (final JFrame frame)
    {
        frame.setName("SymbolRipperFrame"); // For SAF life cycle

        ResourceMap resources = OmrGui.getApplication().getContext().getResourceMap(getClass());
        resources.injectComponents(frame);
        frame.setIconImage(OmrGui.getApplication().getMainFrame().getIconImage());

        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(getParamPanel(), BorderLayout.WEST);

        resizeDrawing();

        JScrollPane scrollPane = new JScrollPane(drawing);
        pane.add(scrollPane, BorderLayout.CENTER);

        return frame;
    }

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the related User Interface frame of this entity
     *
     * @return the window frame
     */
    public JFrame getFrame ()
    {
        return frame;
    }

    //---------------//
    // getParamPanel //
    //---------------//
    private JPanel getParamPanel ()
    {
        FormLayout layout = Panel.makeFormLayout(13, 2, "right:", "35dlu", "45dlu");

        PanelBuilder builder = new PanelBuilder(layout, new Panel());

        ///builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.addSeparator("Font", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
        builder.add(fontName.getLabel(), cst.xy(1, r));
        builder.add(fontName.getSpinner(), cst.xyw(3, r, 5));

        r += 2; // --------------------------------
        builder.addLabel("Base", cst.xy(1, r));
        builder.add(fontBase, cst.xy(3, r));

        builder.add(fontSize.getLabel(), cst.xy(5, r));
        builder.add(fontSize.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.add(pointCode.getLabel(), cst.xy(1, r));
        builder.add(pointCode.getSpinner(), cst.xy(3, r));

        builder.add(hexaCode.getLabel(), cst.xy(5, r));
        builder.add(hexaCode.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.addSeparator("Drawing", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
        builder.add(xOffset.getLabel(), cst.xy(1, r));
        builder.add(xOffset.getSpinner(), cst.xy(3, r));

        builder.add(width.getLabel(), cst.xy(5, r));
        builder.add(width.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.add(yOffset.getLabel(), cst.xy(1, r));
        builder.add(yOffset.getSpinner(), cst.xy(3, r));

        builder.add(height.getLabel(), cst.xy(5, r));
        builder.add(height.getSpinner(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.addSeparator("Symbol", cst.xyw(1, r, 7));

        r += 2; // --------------------------------
        builder.add(xSym.getLabel(), cst.xy(1, r));
        builder.add(xSym.getField(), cst.xy(3, r));

        builder.add(wSym.getLabel(), cst.xy(5, r));
        builder.add(wSym.getField(), cst.xy(7, r));

        r += 2; // --------------------------------
        builder.add(ySym.getLabel(), cst.xy(1, r));
        builder.add(ySym.getField(), cst.xy(3, r));

        builder.add(hSym.getLabel(), cst.xy(5, r));
        builder.add(hSym.getField(), cst.xy(7, r));

        return builder.getPanel();
    }

    //---------------//
    // resizeDrawing //
    //---------------//
    private void resizeDrawing ()
    {
        drawing.setPreferredSize(new Dimension(width.getValue(), height.getValue()));
        drawing.revalidate();
    }

    //---------//
    // startup //
    //---------//
    @Override
    protected void startup ()
    {
        logger.debug("SymbolRipper. 2/startup");

        frame = defineLayout(getMainFrame());

        show(frame); // Here we go...
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // main //
    //------//
    /**
     * Command line entry point, no arguments are used today.
     *
     * @param args unused
     */
    public static void main (String... args)
    {
        standAlone = true;

        // Set UI Look and Feel
        UILookAndFeel.setUI(null);
        Locale.setDefault(Locale.ENGLISH);

        // Off we go...
        Application.launch(SymbolRipper.class, args);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Drawing //
    //---------//
    private class Drawing
            extends Panel
    {

        @Override
        public void paintComponent (Graphics g)
        {
            // For background
            super.paintComponent(g);

            if (image == null) {
                image = buildImage();
            }

            // Meant for visual check
            if (image != null) {
                Graphics2D g2 = (Graphics2D) g;

                g2.drawImage(image, 1, 1, this);

                g.setColor(Color.BLUE);
                g.drawLine(0, yOffset.getValue(), width.getValue(), yOffset.getValue());
                g.drawLine(xOffset.getValue(), 0, xOffset.getValue(), height.getValue());

                g.setColor(Color.ORANGE);
                g.drawRect(0, 0, width.getValue(), height.getValue());

                FontRenderContext frc = g2.getFontRenderContext();
                GlyphVector glyphVector = musicFont.createGlyphVector(frc, string);

                Rectangle rect = glyphVector.getPixelBounds(
                        frc,
                        xOffset.getValue(),
                        yOffset.getValue());
                g.setColor(Color.RED);
                g2.draw(rect);

                // Debug
                TextLayout layout = new TextLayout(string, musicFont, frc);
                logger.debug(
                        "getAdvance(): {} getVisibleAdvance(): {}",
                        layout.getAdvance(),
                        layout.getVisibleAdvance());
            }
        }
    }
}
