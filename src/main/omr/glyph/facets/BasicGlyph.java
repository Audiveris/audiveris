//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c G l y p h                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Result;

import omr.glyph.Evaluation;
import omr.glyph.GlyphSignature;
import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.text.BasicContent;
import omr.glyph.text.OcrChar;
import omr.glyph.text.OcrLine;
import omr.glyph.text.TextArea;
import omr.glyph.text.TextRole;

import omr.lag.Lag;
import omr.lag.Section;

import omr.log.Logger;

import omr.math.Circle;
import omr.math.Line;
import omr.math.PointsCollector;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Text.CreatorText.CreatorType;
import omr.score.entity.TimeRational;

import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

/**
 * Class {@code BasicGlyph} is the basic Glyph implementation.
 *
 * <p>From an implementation point of view, this {@code BasicGlyph} is just a
 * shell around specialized Glyph facets, and most of the methods are simply
 * using delegation to the proper facet.
 *
 * @author Hervé Bitteur
 */
public class BasicGlyph
    implements Glyph
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicGlyph.class);

    //~ Instance fields --------------------------------------------------------

    /** All needed facets */
    final GlyphAdministration administration;
    final GlyphComposition composition;
    final GlyphDisplay     display;
    final GlyphEnvironment environment;
    final GlyphGeometry    geometry;
    final GlyphRecognition recognition;
    final GlyphTranslation translation;
    final GlyphAlignment   alignment;

    // The content facet is not final to allow lazy allocation
    protected GlyphContent content;

    // Set all facets
    final Set<GlyphFacet> facets = new LinkedHashSet<>();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a new BasicGlyph object.
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
        addFacet(alignment = new BasicAlignment(this));
    }

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a new BasicGlyph object from a GlyphValue instance
     * (typically unmarshalled from XML).
     * @param value the GlyphValue "builder" object
     */
    public BasicGlyph (GlyphValue value)
    {
        this(value.interline);

        setId(value.id);
        setShape(value.shape);
        setStemNumber(value.stemNumber);
        setWithLedger(value.withLedger);
        setPitchPosition(value.pitchPosition);

        for (Section section : value.members) {
            addSection(section, Linking.NO_LINK_BACK);
        }
    }

    //------------//
    // BasicGlyph //
    //------------//
    /**
     * Create a glyph with a specific alignment class.
     * @param interline      the scaling information
     * @param alignmentClass the specific alignment class
     */
    protected BasicGlyph (int                            interline,
                          Class<?extends GlyphAlignment> alignmentClass)
    {
        addFacet(administration = new BasicAdministration(this));
        addFacet(composition = new BasicComposition(this));
        addFacet(display = new BasicDisplay(this));
        addFacet(environment = new BasicEnvironment(this));
        addFacet(geometry = new BasicGeometry(this, interline));
        addFacet(recognition = new BasicRecognition(this));
        addFacet(translation = new BasicTranslation(this));

        GlyphAlignment theAlignment = null;

        try {
            Constructor constructor = alignmentClass.getConstructor(
                new Class[] { Glyph.class });
            theAlignment = (GlyphAlignment) constructor.newInstance(
                new Object[] { this });
        } catch (Exception ex) {
            logger.severe("Cannot instantiate BasicGlyph with {0} ex:{1}", new Object[]{alignmentClass, ex});
        }

        addFacet(alignment = theAlignment);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void addAttachment (String         id,
                               java.awt.Shape attachment)
    {
        display.addAttachment(id, attachment);
    }

    @Override
    public void addSection (Section section,
                            Linking link)
    {
        composition.addSection(section, link);
    }

    @Override
    public void addTranslation (Object entity)
    {
        translation.addTranslation(entity);
    }

    @Override
    public void allowShape (Shape shape)
    {
        recognition.allowShape(shape);
    }

    @Override
    public void clearTranslations ()
    {
        translation.clearTranslations();
    }

    @Override
    public void colorize (Collection<Section> sections,
                          Color               color)
    {
        display.colorize(sections, color);
    }

    @Override
    public void colorize (Color color)
    {
        display.colorize(color);
    }

    @Override
    public boolean containsSection (int id)
    {
        return composition.containsSection(id);
    }

    @Override
    public void copyStemInformation (Glyph glyph)
    {
        environment.copyStemInformation(glyph);
    }

    @Override
    public void cutSections ()
    {
        composition.cutSections();
    }

    @Override
    public void drawAscii ()
    {
        display.drawAscii();
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        for (GlyphFacet facet : facets) {
            facet.dump();
        }
    }

    @Override
    public void forbidShape (Shape shape)
    {
        recognition.forbidShape(shape);
    }

    @Override
    public ARTMoments getARTMoments ()
    {
        return geometry.getARTMoments();
    }

    @Override
    public int getAlienPixelsFrom (Lag                lag,
                                   PixelRectangle     absRoi,
                                   Predicate<Section> predicate)
    {
        return environment.getAlienPixelsFrom(lag, absRoi, predicate);
    }

    @Override
    public SystemInfo getAlienSystem (SystemInfo system)
    {
        return composition.getAlienSystem(system);
    }

    @Override
    public Glyph getAncestor ()
    {
        return composition.getAncestor();
    }

    @Override
    public PixelPoint getAreaCenter ()
    {
        return geometry.getAreaCenter();
    }

    @Override
    public double getAspect (Orientation orientation)
    {
        return alignment.getAspect(orientation);
    }

    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        return display.getAttachments();
    }

    @Override
    public PixelRectangle getBounds ()
    {
        return geometry.getBounds();
    }

    @Override
    public PixelPoint getCentroid ()
    {
        return geometry.getCentroid();
    }

    @Override
    public Circle getCircle ()
    {
        return geometry.getCircle();
    }

    @Override
    public Color getColor ()
    {
        return display.getColor();
    }

    @Override
    public Set<Glyph> getConnectedNeighbors ()
    {
        return environment.getConnectedNeighbors();
    }

    //------------//
    // getContent //
    //------------//
    public GlyphContent getContent ()
    {
        // Lazy allocation, to avoid too many allocations
        // (less than 3% of all glyphs need a content facet)
        if (content == null) {
            addFacet(content = new BasicContent(this));
        }

        return content;
    }

    @Override
    public CreatorType getCreatorType ()
    {
        return getContent()
                   .getCreatorType();
    }

    @Override
    public double getDensity ()
    {
        return geometry.getDensity();
    }

    @Override
    public Evaluation getEvaluation ()
    {
        return recognition.getEvaluation();
    }

    @Override
    public Section getFirstSection ()
    {
        return composition.getFirstSection();
    }

    @Override
    public Glyph getFirstStem ()
    {
        return environment.getFirstStem();
    }

    @Override
    public int getFirstStuck ()
    {
        return alignment.getFirstStuck();
    }

    @Override
    public Float getFontSize ()
    {
        return getContent()
                   .getFontSize();
    }

    @Override
    public GeometricMoments getGeometricMoments ()
    {
        return geometry.getGeometricMoments();
    }

    @Override
    public double getGrade ()
    {
        return recognition.getGrade();
    }

    @Override
    public int getId ()
    {
        return administration.getId();
    }

    @Override
    public BufferedImage getImage ()
    {
        return display.getImage();
    }

    @Override
    public int getInterline ()
    {
        return geometry.getInterline();
    }

    @Override
    public double getInvertedSlope ()
    {
        return alignment.getInvertedSlope();
    }

    @Override
    public int getLastStuck ()
    {
        return alignment.getLastStuck();
    }

    @Override
    public int getLength (Orientation orientation)
    {
        return alignment.getLength(orientation);
    }

    @Override
    public Line getLine ()
    {
        return alignment.getLine();
    }

    @Override
    public PixelPoint getLocation ()
    {
        return geometry.getLocation();
    }

    @Override
    public String getManualValue ()
    {
        return getContent()
                   .getManualValue();
    }

    @Override
    public double getMeanDistance ()
    {
        return alignment.getMeanDistance();
    }

    @Override
    public double getMeanThickness (Orientation orientation)
    {
        return alignment.getMeanThickness(orientation);
    }

    @Override
    public SortedSet<Section> getMembers ()
    {
        return composition.getMembers();
    }

    @Override
    public int getMidPos (Orientation orientation)
    {
        return alignment.getMidPos(orientation);
    }

    @Override
    public Nest getNest ()
    {
        return administration.getNest();
    }

    @Override
    public double getNormalizedHeight ()
    {
        return geometry.getNormalizedHeight();
    }

    @Override
    public double getNormalizedWeight ()
    {
        return geometry.getNormalizedWeight();
    }

    @Override
    public double getNormalizedWidth ()
    {
        return geometry.getNormalizedWidth();
    }

    @Override
    public String getOcrLanguage ()
    {
        return getContent()
                   .getOcrLanguage();
    }

    @Override
    public OcrLine getOcrLine ()
    {
        return getContent()
                   .getOcrLine();
    }

    @Override
    public List<OcrLine> getOcrLines (String language)
    {
        return getContent()
                   .getOcrLines(language);
    }

    @Override
    public String getOcrValue ()
    {
        return getContent()
                   .getOcrValue();
    }

    @Override
    public Glyph getPartOf ()
    {
        return composition.getPartOf();
    }

    @Override
    public double getPitchPosition ()
    {
        return environment.getPitchPosition();
    }

    @Override
    public PointsCollector getPointsCollector ()
    {
        return geometry.getPointsCollector();
    }

    @Override
    public double getPositionAt (double      coord,
                                 Orientation orientation)
    {
        return alignment.getPositionAt(coord, orientation);
    }

    @Override
    public String getPseudoValue ()
    {
        return getContent()
                   .getPseudoValue();
    }

    @Override
    public Point2D getRectangleCentroid (PixelRectangle absRoi)
    {
        return alignment.getRectangleCentroid(absRoi);
    }

    @Override
    public GlyphSignature getRegisteredSignature ()
    {
        return geometry.getRegisteredSignature();
    }

    @Override
    public Result getResult ()
    {
        return composition.getResult();
    }

    @Override
    public Shape getShape ()
    {
        return recognition.getShape();
    }

    @Override
    public GlyphSignature getSignature ()
    {
        return geometry.getSignature();
    }

    @Override
    public double getSlope ()
    {
        return alignment.getSlope();
    }

    @Override
    public Point2D getStartPoint (Orientation orientation)
    {
        return alignment.getStartPoint(orientation);
    }

    @Override
    public Glyph getStem (HorizontalSide side)
    {
        return environment.getStem(side);
    }

    @Override
    public int getStemNumber ()
    {
        return environment.getStemNumber();
    }

    @Override
    public Point2D getStopPoint (Orientation orientation)
    {
        return alignment.getStopPoint(orientation);
    }

    @Override
    public void getSymbolsAfter (Predicate<Glyph> predicate,
                                 Set<Glyph>       goods,
                                 Set<Glyph>       bads)
    {
        environment.getSymbolsAfter(predicate, goods, bads);
    }

    @Override
    public void getSymbolsBefore (Predicate<Glyph> predicate,
                                  Set<Glyph>       goods,
                                  Set<Glyph>       bads)
    {
        environment.getSymbolsBefore(predicate, goods, bads);
    }

    @Override
    public SystemInfo getSystem ()
    {
        return environment.getSystem();
    }

    @Override
    public TextArea getTextArea ()
    {
        return getContent()
                   .getTextArea();
    }

    @Override
    public int getTextHeight ()
    {
        return getContent()
                   .getTextHeight();
    }

    @Override
    public TextRole getTextRole ()
    {
        return getContent()
                   .getTextRole();
    }

    @Override
    public PixelPoint getTextStart ()
    {
        return getContent()
                   .getTextStart();
    }

    @Override
    public String getTextValue ()
    {
        return getContent()
                   .getTextValue();
    }

    @Override
    public int getThickness (Orientation orientation)
    {
        return alignment.getThickness(orientation);
    }

    @Override
    public double getThicknessAt (double      coord,
                                  Orientation orientation)
    {
        return alignment.getThicknessAt(coord, orientation);
    }

    @Override
    public TimeRational getTimeRational ()
    {
        return recognition.getTimeRational();
    }

    @Override
    public Collection<Object> getTranslations ()
    {
        return translation.getTranslations();
    }

    @Override
    public int getWeight ()
    {
        return geometry.getWeight();
    }

    @Override
    public String idString ()
    {
        return administration.idString();
    }

    @Override
    public boolean intersects (PixelRectangle rectangle)
    {
        return geometry.intersects(rectangle);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        // Invalidate all allocated facets
        for (GlyphFacet facet : facets) {
            facet.invalidateCache();
        }
    }

    @Override
    public boolean isActive ()
    {
        return composition.isActive();
    }

    @Override
    public boolean isBar ()
    {
        return recognition.isBar();
    }

    @Override
    public boolean isClef ()
    {
        return recognition.isClef();
    }

    @Override
    public boolean isElision ()
    {
        return getContent()
                   .isElision();
    }

    @Override
    public boolean isExtension ()
    {
        return getContent()
                   .isExtension();
    }

    @Override
    public boolean isHyphen ()
    {
        return getContent()
                   .isHyphen();
    }

    @Override
    public boolean isKnown ()
    {
        return recognition.isKnown();
    }

    @Override
    public boolean isManualShape ()
    {
        return recognition.isManualShape();
    }

    @Override
    public boolean isProcessed ()
    {
        return administration.isProcessed();
    }

    @Override
    public boolean isShapeForbidden (Shape shape)
    {
        return recognition.isShapeForbidden(shape);
    }

    @Override
    public boolean isStem ()
    {
        return recognition.isStem();
    }

    @Override
    public boolean isSuccessful ()
    {
        return composition.isSuccessful();
    }

    @Override
    public boolean isText ()
    {
        return recognition.isText();
    }

    @Override
    public boolean isTransient ()
    {
        return administration.isTransient();
    }

    @Override
    public boolean isTranslated ()
    {
        return translation.isTranslated();
    }

    @Override
    public boolean isVip ()
    {
        return administration.isVip();
    }

    @Override
    public boolean isVirtual ()
    {
        return administration.isVirtual();
    }

    @Override
    public boolean isWellKnown ()
    {
        return recognition.isWellKnown();
    }

    @Override
    public boolean isWithLedger ()
    {
        return environment.isWithLedger();
    }

    @Override
    public void linkAllSections ()
    {
        composition.linkAllSections();
    }

    @Override
    public void recolorize ()
    {
        display.recolorize();
    }

    @Override
    public int removeAttachments (String prefix)
    {
        return display.removeAttachments(prefix);
    }

    @Override
    public boolean removeSection (Section section,
                                  Linking link)
    {
        return composition.removeSection(section, link);
    }

    @Override
    public void renderAttachments (Graphics2D g)
    {
        display.renderAttachments(g);
    }

    @Override
    public void renderLine (Graphics2D g)
    {
        alignment.renderLine(g);
    }

    @Override
    public void resetEvaluation ()
    {
        recognition.resetEvaluation();
    }

    @Override
    public List<OcrLine> retrieveOcrLines (String language)
    {
        return getContent()
                   .retrieveOcrLines(language);
    }

    @Override
    public SortedSet<Section> retrieveSections (List<OcrChar> chars)
    {
        return getContent()
                   .retrieveSections(chars);
    }

    @Override
    public List<Glyph> retrieveWordGlyphs ()
    {
        return getContent()
                   .retrieveWordGlyphs();
    }

    @Override
    public void setCircle (Circle circle)
    {
        geometry.setCircle(circle);
    }

    @Override
    public void setContourBox (PixelRectangle contourBox)
    {
        geometry.setContourBox(contourBox);
    }

    @Override
    public void setCreatorType (CreatorType creatorType)
    {
        getContent()
            .setCreatorType(creatorType);
    }

    @Override
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        alignment.setEndingPoints(pStart, pStop);
    }

    @Override
    public void setEvaluation (Evaluation evaluation)
    {
        recognition.setEvaluation(evaluation);
    }

    @Override
    public void setId (int id)
    {
        administration.setId(id);
    }

    @Override
    public void setManualValue (String manualValue)
    {
        getContent()
            .setManualValue(manualValue);
    }

    @Override
    public void setNest (Nest nest)
    {
        administration.setNest(nest);
    }

    @Override
    public void setOcrLines (String        ocrLanguage,
                             List<OcrLine> ocrLines)
    {
        getContent()
            .setOcrLines(ocrLanguage, ocrLines);
    }

    @Override
    public void setPartOf (Glyph compound)
    {
        composition.setPartOf(compound);
    }

    @Override
    public void setPitchPosition (double pitchPosition)
    {
        environment.setPitchPosition(pitchPosition);
    }

    @Override
    public void setProcessed (boolean processed)
    {
        administration.setProcessed(processed);
    }

    @Override
    public void setRegisteredSignature (GlyphSignature sig)
    {
        geometry.setRegisteredSignature(sig);
    }

    @Override
    public void setResult (Result result)
    {
        composition.setResult(result);
    }

    @Override
    public void setShape (Shape  shape,
                          double grade)
    {
        recognition.setShape(shape, grade);
    }

    @Override
    public void setShape (Shape shape)
    {
        recognition.setShape(shape);
    }

    @Override
    public void setStem (Glyph          stem,
                         HorizontalSide side)
    {
        environment.setStem(stem, side);
    }

    @Override
    public void setStemNumber (int stemNumber)
    {
        environment.setStemNumber(stemNumber);
    }

    @Override
    public void setTextRole (TextRole type)
    {
        getContent()
            .setTextRole(type);
    }

    @Override
    public void setTimeRational (TimeRational timeRational)
    {
        recognition.setTimeRational(timeRational);
    }

    @Override
    public void setTranslation (Object entity)
    {
        translation.setTranslation(entity);
    }

    @Override
    public void setVip ()
    {
        administration.setVip();
    }

    @Override
    public void setWithLedger (boolean withLedger)
    {
        environment.setWithLedger(withLedger);
    }

    @Override
    public void stealSections (Glyph that)
    {
        composition.stealSections(that);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{")
          .append(getClass().getSimpleName())
          .append("#")
          .append(this.getId());

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    @Override
    public void translate (PixelPoint vector)
    {
        geometry.translate(vector);
    }

    //--------------//
    // getAlignment //
    //--------------//
    protected GlyphAlignment getAlignment ()
    {
        return alignment;
    }

    //----------------//
    // getComposition //
    //----------------//
    protected GlyphComposition getComposition ()
    {
        return composition;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for
     * inclusion in a toString.
     * The overriding methods, if any, should return a string that begins with
     * a " " followed by some content.
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(25);

        if (getShape() != null) {
            sb.append(" ")
              .append(recognition.getEvaluation());

            if (getShape()
                    .getPhysicalShape() != getShape()) {
                sb.append(" physical=")
                  .append(getShape().getPhysicalShape());
            }
        }

        if (getPartOf() != null) {
            sb.append(" partOf#")
              .append(getPartOf().getId());
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

        if (getResult() != null) {
            sb.append(" ")
              .append(getResult());
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
    final void addFacet (GlyphFacet facet)
    {
        facets.add(facet);
    }
}
