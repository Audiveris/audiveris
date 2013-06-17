//----------------------------------------------------------------------------//
//                                                                            //
//                          S y m b o l R i p p e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.ui.MainGui;
import omr.ui.field.IntegerListSpinner;
import omr.ui.field.LDoubleField;
import omr.ui.field.LHexaSpinner;
import omr.ui.field.LIntegerSpinner;
import omr.ui.field.LSpinner;
import omr.ui.field.SpinnerUtil;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SymbolRipper} is a stand-alone utility to generate the
 * textual description of a symbol. Symbol appearance is "ripped" from a musical
 * font.
 *
 * @author Hervé Bitteur
 */
public class SymbolRipper
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SymbolRipper.class);

    static {
        // UI Look and Feel
        UILookAndFeel.setUI(null);
    }

    //~ Instance fields --------------------------------------------------------
    /** Related frame */
    private final JFrame frame;

    /** Image being built */
    private BufferedImage image;

    //---------------//
    // paramListener //
    //---------------//
    private ChangeListener paramListener = new ChangeListener()
    {
        @Override
        public void stateChanged (ChangeEvent e)
        {
            JSpinner s = (JSpinner) e.getSource();

            if (s == fontName.getSpinner()) {
                // New font name
                defineFont();
            } else {
                if (s == fontBase) {
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
                } else if ((s == xOffset.getSpinner())
                           || (s == yOffset.getSpinner())) {
                    // New drawing offset
                } else if ((s == width.getSpinner())
                           || (s == height.getSpinner())) {
                    // New drawing dimension
                    resizeDrawing();
                }
            }

            // For all
            image = buildImage();
            frame.repaint();
        }
    };

    // Panel where the icon is drawn
    private JPanel drawing;

    // String used to draw the symbol
    private String string;

    // Current music font
    private Font musicFont;

    // Font name
    private LSpinner fontName = new LSpinner(
            "Font",
            "Name of the font");

    // Font base
    private IntegerListSpinner fontBase = new IntegerListSpinner();

    // Font size
    private LIntegerSpinner fontSize = new LIntegerSpinner(
            "Size",
            "Font size in picas");

    // Point code
    private LIntegerSpinner pointCode = new LIntegerSpinner(
            "Code",
            "Point code");

    // Hexa representation
    private LHexaSpinner hexaCode = new LHexaSpinner(
            "Hexa",
            "Hexa value of the point code");

    // X Offset
    private LIntegerSpinner xOffset = new LIntegerSpinner(
            "xOffset",
            "X offset");

    // Width
    private LIntegerSpinner width = new LIntegerSpinner(
            "Width",
            "Drawing Width");

    // Y Offset
    private LIntegerSpinner yOffset = new LIntegerSpinner(
            "yOffset",
            "Y offset");

    // Height
    private LIntegerSpinner height = new LIntegerSpinner(
            "Height",
            "Drawing Height");

    // x symbol
    private final String f = "%.3f";

    private LDoubleField xSym = new LDoubleField(false, "xSym", "x symbol", f);

    // w symbol
    private LDoubleField wSym = new LDoubleField(false, "wSym", "w symbol", f);

    // y symbol
    private LDoubleField ySym = new LDoubleField(false, "ySym", "y symbol", f);

    // y symbol
    private LDoubleField hSym = new LDoubleField(false, "hSym", "h symbol", f);

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SymbolRipper object.
     */
    public SymbolRipper ()
    {
        // Related frame
        frame = new JFrame();
        frame.setTitle("Symbol Ripper");

        // Actors
        drawing = new Drawing();

        fontBase.setModel(
                new SpinnerListModel(new Integer[]{0, 0xf000, 0x1d100}));
        SpinnerUtil.setRightAlignment(fontBase);
        SpinnerUtil.fixIntegerList(fontBase);

        fontName.setModel(
                new SpinnerListModel(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

        // Initial values
        ///fontName.getSpinner().setValue("MusicalSymbols");
        fontName.getSpinner()
                .setValue("Symbola");
        fontBase.setValue(0); //0);
        fontSize.setValue(200);
        pointCode.setModel(new SpinnerNumberModel(0x1d100, 0, 0x1d1ff, 1));
        width.setValue(400);
        height.setValue(500);
        xOffset.setValue(200);
        yOffset.setValue(300);
        changeCode();
        defineFont();

        // Listeners
        fontName.addChangeListener(paramListener);
        fontBase.addChangeListener(paramListener);
        fontSize.addChangeListener(paramListener);
        pointCode.addChangeListener(paramListener);
        hexaCode.addChangeListener(paramListener);
        xOffset.addChangeListener(paramListener);
        yOffset.addChangeListener(paramListener);
        width.addChangeListener(paramListener);
        height.addChangeListener(paramListener);

        // Global layout
        defineLayout();

        // Frame behavior
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        MainGui.getInstance()
                .show(frame);

        // Actions
        image = buildImage();
        frame.repaint();
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // main //
    //------//
    /**
     * Command line entry point, no arguments are used today.
     */
    public static void main (String... args)
    {
        new SymbolRipper();
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
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

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
        int code = (Integer) pointCode.getSpinner()
                .getValue();
        string = new String(Character.toChars(base + code));
        hexaCode.setValue(base + code);
    }

    //----------------//
    // changeHexaCode //
    //----------------//
    private void changeHexaCode ()
    {
        int base = (Integer) fontBase.getValue();
        int hexa = (Integer) hexaCode.getSpinner()
                .getValue();
        string = new String(Character.toChars(hexa));
        pointCode.setValue(hexa - base);
    }

    //------------//
    // defineFont //
    //------------//
    private void defineFont ()
    {
        String name = (String) fontName.getSpinner()
                .getValue();
        int val = fontSize.getValue();
        musicFont = new Font(name, Font.PLAIN, val);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(getParamPanel(), BorderLayout.WEST);

        resizeDrawing();

        JScrollPane scrollPane = new JScrollPane(drawing);
        pane.add(scrollPane, BorderLayout.CENTER);
    }

    //---------------//
    // getParamPanel //
    //---------------//
    private JPanel getParamPanel ()
    {
        FormLayout layout = Panel.makeFormLayout(
                13,
                2,
                "right:",
                "35dlu",
                "45dlu");

        PanelBuilder builder = new PanelBuilder(layout, new Panel());
        builder.setDefaultDialogBorder();

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
        drawing.setPreferredSize(
                new Dimension(width.getValue(), height.getValue()));
        drawing.revalidate();
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Drawing //
    //---------//
    private class Drawing
            extends Panel
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void paintComponent (Graphics g)
        {
            // For background
            super.paintComponent(g);

            // Meant for visual check
            if (image != null) {
                Graphics2D g2 = (Graphics2D) g;

                g2.drawImage(image, 1, 1, this);

                g.setColor(Color.BLUE);
                g.drawLine(
                        0,
                        yOffset.getValue(),
                        width.getValue(),
                        yOffset.getValue());
                g.drawLine(
                        xOffset.getValue(),
                        0,
                        xOffset.getValue(),
                        height.getValue());

                g.setColor(Color.ORANGE);
                g.drawRect(0, 0, width.getValue(), height.getValue());

                FontRenderContext frc = g2.getFontRenderContext();
                GlyphVector glyphVector = musicFont.createGlyphVector(
                        frc,
                        string);

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
