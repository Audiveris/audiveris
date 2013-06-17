//----------------------------------------------------------------------------//
//                                                                            //
//                        P a r a m e t e r s T a s k                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.run.AdaptiveDescriptor;
import omr.run.FilterDescriptor;
import omr.run.GlobalDescriptor;

import omr.score.Score;
import omr.score.entity.Page;
import omr.score.entity.ScorePart;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Step;
import omr.step.Stepping;
import omr.step.Steps;

import omr.util.LiveParam;
import omr.util.Param;
import omr.util.TreeNode;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * Class {@code ParametersTask} handles global parameters as the
 * results of dialog {@link omr.score.ui.ScoreParameters}.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ParametersTask
        extends ScriptTask
{
    //~ Instance fields --------------------------------------------------------

    /** Language code. */
    @XmlElement(name = "language")
    private String language;

    /** Tempo value. */
    @XmlElement(name = "tempo")
    private Integer tempo;

    /** Pixel filter. */
    @XmlElements({
        @XmlElement(name = "global-filter",
                    type = GlobalDescriptor.class),
        @XmlElement(name = "adaptive-filter",
                    type = AdaptiveDescriptor.class)
    })
    private FilterDescriptor filterDescriptor;

    /** Description data for each part. */
    @XmlElement(name = "part")
    private List<PartData> parts = new ArrayList<>();

    /** Specific page parameters. */
    @XmlElement(name = "page")
    private List<PageParameters> pages = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    //
    //----------------//
    // ParametersTask //
    //----------------//
    /** No-arg constructor needed by JAXB */
    public ParametersTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //---------//
    // addPart //
    //---------//
    /**
     * Add data for one part
     *
     * @param name    the part name
     * @param program the midi program
     */
    public void addPart (String name,
                         int program)
    {
        parts.add(new PartData(name, program));
    }

    //------//
    // core //
    //------//
    /**
     * This is the place where non-default settings are performed.
     *
     * @param sheet the current sheet, if any
     * @throws Exception
     */
    @Override
    public void core (Sheet sheet)
            throws Exception
    {
        Score score = sheet.getScore();
        StringBuilder sb = new StringBuilder();

        // Score Binarization
        if (filterDescriptor != null) {
            if (score.getFilterParam().setSpecific(filterDescriptor)) {
                sb.append(" filter:")
                        .append(filterDescriptor);
            }
        }

        // Score Language
        if (language != null) {
            if (score.getTextParam().setSpecific(language)) {
                sb.append(" language:")
                        .append(language);
            }
        }

        // Score Tempo
        if (tempo != null) {
            if (score.getTempoParam().setSpecific(tempo)) {
                sb.append(" tempo:")
                        .append(tempo);
            }
        }

        // Score Parts
        for (int i = 0; i < parts.size(); i++) {
            try {
                ScorePart scorePart = score.getPartList().get(i);
                PartData data = parts.get(i);

                // Part name
                scorePart.setName(data.name);

                // Part midi program
                scorePart.setMidiProgram(data.program);
            } catch (Exception ex) {
                logger.warn(
                        "Error in script Parameters part#" + (i + 1),
                        ex);
            }
        }

        // Pages
        for (PageParameters params : pages) {
            Page page = score.getPage(params.index);
            sb.append(" {page:").append(params.index);

            // Page Binarization
            if (params.filterDescriptor != null) {
                if (page.getFilterParam().setSpecific(params.filterDescriptor)) {
                    sb.append(" filter:")
                            .append(params.filterDescriptor);
                }
            }

            // Page Language
            if (params.language != null) {
                Param<String> context = page.getTextParam();
                if (page.getTextParam().setSpecific(params.language)) {
                    sb.append(" language:")
                            .append(params.language);
                }
            }

            sb.append("}");
        }

        if (sb.length() > 0) {
            logger.info("{}parameters{}", score.getLogPrefix(), sb);
        }
    }

    //--------//
    // epilog //
    //--------//
    /**
     * Determine from which step we should rebuild, page per page.
     *
     * @param sheet the related sheet
     */
    @Override
    public void epilog (Sheet sheet)
    {

        final Step scaleStep = Steps.valueOf(Steps.SCALE);
        final Step textsStep = Steps.valueOf(Steps.TEXTS);
        final Step symbolsStep = Steps.valueOf(Steps.SYMBOLS);
        final Step scoreStep = Steps.valueOf(Steps.SCORE);

        Step latestStep = Stepping.getLatestMandatoryStep(sheet);

        for (TreeNode pn : new ArrayList<>(sheet.getScore().getPages())) {
            final Page page = (Page) pn;
            Step from = null;

            // Language
            if (Steps.compare(latestStep, textsStep) >= 0) {
                LiveParam<String> param = page.getTextParam();
                if (param.needsUpdate()) {
                    logger.debug("Page {} needs TEXT with {}",
                            page.getId(), param.getTarget());
                    // Convert the text items as much as possible
                    final Sheet theSheet = page.getSheet();
                    for (SystemInfo system : theSheet.getSystems()) {
                        system.getTextBuilder().switchLanguageTexts();
                    }

                    // Reprocess this page from SYMBOLS step
                    from = symbolsStep;
                }
            }

            // Binarization
            if (Steps.compare(latestStep, scaleStep) >= 0) {
                LiveParam<FilterDescriptor> param = page.getFilterParam();
                if (param.needsUpdate()) {
                    logger.debug("Page {} needs SCALE with {}",
                            page.getId(), param.getTarget());
                    //  Reprocess this page from SCALE step
                    from = scaleStep;
                }

            }

            Stepping.reprocessSheet(from, sheet, null, true, false);
        }

        // Final SCORE (merge) step?
        if (latestStep == scoreStep) {
            Stepping.reprocessSheet(scoreStep, sheet, null, true, true);
        }

        super.epilog(sheet);
    }

    //-----------//
    // setFilter //
    //-----------//
    /**
     * Set binarization filter at proper scope level.
     *
     * @param filterDescriptor the filter to use for pixels binarization
     * @param page             not null for page setting
     */
    public void setFilter (FilterDescriptor filterDescriptor,
                           Page page)
    {
        if (page != null) {
            getParams(page).filterDescriptor = filterDescriptor;
        } else {
            this.filterDescriptor = filterDescriptor;
        }
    }

    //-------------//
    // setLanguage //
    //-------------//
    /**
     * Set language at proper scope level.
     *
     * @param language the language code to set
     * @param page     not null for page setting
     */
    public void setLanguage (String language,
                             Page page)
    {
        if (page != null) {
            getParams(page).language = language;
        } else {
            this.language = language;
        }
    }

    //----------//
    // setTempo //
    //----------//
    /**
     * Set score tempo.
     *
     * @param tempo the tempo value
     */
    public void setTempo (Integer tempo)
    {
        this.tempo = tempo;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(" parameters");

        if (filterDescriptor != null) {
            sb.append(" filter:")
                    .append(filterDescriptor);
        }

        if (language != null) {
            sb.append(" language:")
                    .append(language);
        }

        for (PartData data : parts) {
            sb.append(" ")
                    .append(data);
        }

        for (PageParameters params : pages) {
            sb.append(" ")
                    .append(params);
        }

        return sb.toString() + super.internalsString();
    }

    //-----------//
    // getParams //
    //-----------//
    /**
     * [May create] and report the parameters for the provided page.
     *
     * @param page the provided page
     * @return the page parameters
     */
    private PageParameters getParams (Page page)
    {
        int index = page.getIndex();
        for (PageParameters params : pages) {
            if (params.index == index) {
                return params;
            }
        }

        // Not found, create one
        PageParameters params = new PageParameters();
        params.index = index;
        pages.add(params);

        return params;
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // PartData //
    //----------//
    public static class PartData
    {
        //~ Instance fields ----------------------------------------------------

        /** Name of the part */
        @XmlAttribute
        public final String name;

        /** Midi Instrument */
        @XmlAttribute
        public final int program;

        //~ Constructors -------------------------------------------------------
        public PartData (String name,
                         int program)
        {
            this.name = name;
            this.program = program;
        }

        private PartData ()
        {
            name = null;
            program = 0;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{name:" + name + " program:" + program + "}";
        }
    }

    //----------------//
    // PageParameters //
    //----------------//
    /**
     * Parameters for a page.
     */
    public static class PageParameters
    {

        /** Page unique index. */
        @XmlAttribute(name = "index")
        private int index;

        /** Language code. */
        @XmlElement(name = "language")
        private String language;

        /** Pixel filter. */
        @XmlElements({
            @XmlElement(name = "global-filter",
                        type = GlobalDescriptor.class),
            @XmlElement(name = "adaptive-filter",
                        type = AdaptiveDescriptor.class)
        })
        private FilterDescriptor filterDescriptor;

        //~ Constructors -------------------------------------------------------
        //
        /** No-arg constructor needed by JAXB */
        public PageParameters ()
        {
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{page#");
            sb.append(index);

            if (filterDescriptor != null) {
                sb.append(" filter:")
                        .append(filterDescriptor);
            }

            if (language != null) {
                sb.append(" language:")
                        .append(language);
            }

            sb.append("}");
            return sb.toString();
        }
    }
}
