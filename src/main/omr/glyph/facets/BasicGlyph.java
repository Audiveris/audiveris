//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c G l y p h                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Result;

import omr.glyph.Evaluation;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphSignature;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.text.TextInfo;

import omr.lag.Lag;

import omr.math.Moments;
import omr.math.Rational;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

/**
 * Class {@code BasicGlyph} is the basic Glyph implementation for any glyph
 * found, such as stem, ledger, accidental, note head, etc...
 *
 * <p>A Glyph is basically a collection of sections. It can be split into
 * smaller glyphs, which may later be re-assembled into another instance of
 * glyph. There is a means, based on a simple signature (weight and bounding
 * box), to detect if the glyph at hand is identical to a previous one, which is
 * then reused.
 *
 * <p>A Glyph can be stored on disk and reloaded
 *
 * <p>From an implementation point of view, this {@code BasicGlyph} is just a
 * shell around specialized Glyph facets, and most of the methods are simply
 * forwarding to the proper facet.
 *
 * @author Hervé Bitteur
 */
public class BasicGlyph
    implements Glyph
{
    //~ Instance fields --------------------------------------------------------

    /** All needed facets */
    final GlyphAdministration administration;
    final GlyphComposition composition;
    final GlyphDisplay     display;
    final GlyphEnvironment environment;
    final GlyphGeometry    geometry;
    final GlyphRecognition recognition;
    final GlyphTranslation translation;

    // Sequence of all facets
    final Set<GlyphFacet> facets = new LinkedHashSet<GlyphFacet>();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a new BasicGlyph object
     * @param interline the scaling interline value
     */
    public BasicGlyph (int interline)
    {
        addFacet(administration = new BasicAdministration(this));
        addFacet(composition = new BasicComposition(this));
        addFacet(display = new BasicDisplay(this));
        addFacet(environment = new BasicEnvironment(this));
        addFacet(geometry = new BasicGeometry(this, interline));
        addFacet(recognition = new BasicRecognition(this));
        addFacet(translation = new BasicTranslation(this));
    }

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a new BasicGlyph object from a GlyphValue instance (typically
     * unmarshalled from XML)
     *
     * @param value the GlyphValue "builder" object
     */
    public BasicGlyph (GlyphValue value)
    {
        this(value.interline);
        this.setId(value.id);
        this.setShape(value.shape);
        this.setStemNumber(value.stemNumber);
        this.setWithLedger(value.withLedger);
        this.setPitchPosition(value.pitchPosition);
        this.getMembers()
            .addAll(value.members);
    }

    //~ Methods ----------------------------------------------------------------

    public boolean isActive ()
    {
        return composition.isActive();
    }

    public SystemInfo getAlienSystem (SystemInfo system)
    {
        return composition.getAlienSystem(system);
    }

    public PixelPoint getAreaCenter ()
    {
        return geometry.getAreaCenter();
    }

    public boolean isBar ()
    {
        return recognition.isBar();
    }

    public Rectangle getBounds ()
    {
        return geometry.getBounds();
    }

    public PixelPoint getCentroid ()
    {
        return geometry.getCentroid();
    }

    public boolean isClef ()
    {
        return recognition.isClef();
    }

    public Color getColor ()
    {
        return display.getColor();
    }

    public PixelRectangle getContourBox ()
    {
        return geometry.getContourBox();
    }

    public double getDensity ()
    {
        return geometry.getDensity();
    }

    public double getDoubt ()
    {
        return recognition.getDoubt();
    }

    public void setEvaluation (Evaluation evaluation)
    {
        recognition.setEvaluation(evaluation);
    }

    public Evaluation getEvaluation ()
    {
        return recognition.getEvaluation();
    }

    public GlyphSection getFirstSection ()
    {
        return composition.getFirstSection();
    }

    public void setId (int id)
    {
        administration.setId(id);
    }

    public int getId ()
    {
        return administration.getId();
    }

    public BufferedImage getImage ()
    {
        return display.getImage();
    }

    public int getInterline ()
    {
        return geometry.getInterline();
    }

    public boolean isKnown ()
    {
        return recognition.isKnown();
    }

    public void setLag (GlyphLag lag)
    {
        administration.setLag(lag);
    }

    public GlyphLag getLag ()
    {
        return administration.getLag();
    }

    public void setLeftStem (Glyph leftStem)
    {
        environment.setLeftStem(leftStem);
    }

    public Glyph getLeftStem ()
    {
        return environment.getLeftStem();
    }

    public PixelPoint getLocation ()
    {
        return geometry.getLocation();
    }

    public boolean isManualShape ()
    {
        return recognition.isManualShape();
    }

    public SortedSet<GlyphSection> getMembers ()
    {
        return composition.getMembers();
    }

    public Moments getMoments ()
    {
        return geometry.getMoments();
    }

    public double getNormalizedHeight ()
    {
        return geometry.getNormalizedHeight();
    }

    public double getNormalizedWeight ()
    {
        return geometry.getNormalizedWeight();
    }

    public double getNormalizedWidth ()
    {
        return geometry.getNormalizedWidth();
    }

    public void setPartOf (Glyph compound)
    {
        composition.setPartOf(compound);
    }

    public Glyph getPartOf ()
    {
        return composition.getPartOf();
    }

    public void setParts (Collection<?extends Glyph> parts)
    {
        composition.setParts(parts);
    }

    public Set<Glyph> getParts ()
    {
        return composition.getParts();
    }

    public void setPitchPosition (double pitchPosition)
    {
        environment.setPitchPosition(pitchPosition);
    }

    public double getPitchPosition ()
    {
        return environment.getPitchPosition();
    }

    public void setRational (Rational rational)
    {
        recognition.setRational(rational);
    }

    public Rational getRational ()
    {
        return recognition.getRational();
    }

    public void setResult (Result result)
    {
        composition.setResult(result);
    }

    public Result getResult ()
    {
        return composition.getResult();
    }

    public void setRightStem (Glyph rightStem)
    {
        environment.setRightStem(rightStem);
    }

    public Glyph getRightStem ()
    {
        return environment.getRightStem();
    }

    public void setShape (Shape  shape,
                          double doubt)
    {
        recognition.setShape(shape, doubt);
    }

    public void setShape (Shape shape)
    {
        recognition.setShape(shape);
    }

    public Shape getShape ()
    {
        return recognition.getShape();
    }

    public boolean isShapeForbidden (Shape shape)
    {
        return recognition.isShapeForbidden(shape);
    }

    public GlyphSignature getSignature ()
    {
        return geometry.getSignature();
    }

    public boolean isStem ()
    {
        return recognition.isStem();
    }

    public void setStemNumber (int stemNumber)
    {
        environment.setStemNumber(stemNumber);
    }

    public int getStemNumber ()
    {
        return environment.getStemNumber();
    }

    public boolean isSuccessful ()
    {
        return composition.isSuccessful();
    }

    public void getSymbolsAfter (Predicate<Glyph> predicate,
                                 Set<Glyph>       goods,
                                 Set<Glyph>       bads)
    {
        environment.getSymbolsAfter(predicate, goods, bads);
    }

    public void getSymbolsBefore (Predicate<Glyph> predicate,
                                  Set<Glyph>       goods,
                                  Set<Glyph>       bads)
    {
        environment.getSymbolsBefore(predicate, goods, bads);
    }

    public boolean isText ()
    {
        return recognition.isText();
    }

    public TextInfo getTextInfo ()
    {
        return recognition.getTextInfo();
    }

    public boolean isTransient ()
    {
        return administration.isTransient();
    }

    public boolean isTranslated ()
    {
        return translation.isTranslated();
    }

    public void setTranslation (Object entity)
    {
        translation.setTranslation(entity);
    }

    public Collection<Object> getTranslations ()
    {
        return translation.getTranslations();
    }

    public boolean isVirtual ()
    {
        return administration.isVirtual();
    }

    public int getWeight ()
    {
        return geometry.getWeight();
    }

    public boolean isWellKnown ()
    {
        return recognition.isWellKnown();
    }

    public void setWithLedger (boolean withLedger)
    {
        environment.setWithLedger(withLedger);
    }

    public boolean isWithLedger ()
    {
        return environment.isWithLedger();
    }

    public void addGlyphSections (Glyph   other,
                                  Linking linkSections)
    {
        composition.addGlyphSections(other, linkSections);
    }

    public void addSection (GlyphSection section,
                            Linking      link)
    {
        composition.addSection(section, link);
    }

    public void addTranslation (Object entity)
    {
        translation.addTranslation(entity);
    }

    public void allowShape (Shape shape)
    {
        recognition.allowShape(shape);
    }

    public void clearTranslations ()
    {
        translation.clearTranslations();
    }

    public void colorize (int                      viewIndex,
                          Collection<GlyphSection> sections,
                          Color                    color)
    {
        display.colorize(viewIndex, sections, color);
    }

    public void colorize (Lag   lag,
                          int   viewIndex,
                          Color color)
    {
        display.colorize(lag, viewIndex, color);
    }

    public void colorize (int   viewIndex,
                          Color color)
    {
        display.colorize(viewIndex, color);
    }

    public void computeMoments ()
    {
        geometry.computeMoments();
    }

    public void copyStemInformation (Glyph glyph)
    {
        environment.copyStemInformation(glyph);
    }

    public void cutSections ()
    {
        composition.cutSections();
    }

    public void drawAscii ()
    {
        display.drawAscii();
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        for (GlyphFacet facet : facets) {
            facet.dump();
        }
    }

    public void forbidShape (Shape shape)
    {
        recognition.forbidShape(shape);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    public void invalidateCache ()
    {
        for (GlyphFacet facet : facets) {
            facet.invalidateCache();
        }
    }

    public void linkAllSections ()
    {
        composition.linkAllSections();
    }

    public void recolorize (int viewIndex)
    {
        display.recolorize(viewIndex);
    }

    //-----------//
    // translate //
    //-----------//
    public void translate (PixelPoint vector)
    {
        geometry.translate(vector);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);
        sb.append("{")
          .append(getClass().getSimpleName());
        sb.append("#")
          .append(getId());

        if (getShape() != null) {
            sb.append(" ")
              .append(recognition.getEvaluation());

            if (getShape()
                    .getNakedShape() != getShape()) {
                sb.append(" training=")
                  .append(getShape().getNakedShape());
            }

            if (getShape()
                    .isText()) {
                String textContent = getTextInfo()
                                         .getContent();

                if (textContent != null) {
                    sb.append(" \"")
                      .append(textContent)
                      .append("\"");
                }
            }
        }

        if (getPartOf() != null) {
            sb.append(" partOf#")
              .append(getPartOf().getId());
        }

        if (!getParts()
                 .isEmpty()) {
            sb.append(Glyphs.toString(" parts", getParts()));
        }

        if (getCentroid() != null) {
            sb.append(" centroid=[")
              .append(getCentroid().x)
              .append(",")
              .append(getCentroid().y)
              .append("]");
        }

        if (isTranslated()) {
            sb.append(" trans=[")
              .append(getTranslations())
              .append("]");
        }

        // Is that all?
        if (this.getClass()
                .getName()
                .equals(BasicGlyph.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //----------//
    // addFacet //
    //----------//
    /**
     * Register a facet
     * @param facet the facet to register
     */
    void addFacet (GlyphFacet facet)
    {
        facets.add(facet);
    }
}
