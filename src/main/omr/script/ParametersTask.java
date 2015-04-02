//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r a m e t e r s T a s k                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.image.AdaptiveDescriptor;
import omr.image.FilterDescriptor;
import omr.image.GlobalDescriptor;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Step;

import omr.util.LiveParam;

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
    //~ Instance fields ----------------------------------------------------------------------------

    /** Language code. */
    @XmlElement(name = "language")
    private String language;

    /** Tempo value. */
    //TODO: Really handle tempo information?
    @XmlElement(name = "tempo")
    private Integer tempo;

    /** Pixel filter. */
    @XmlElements({
        @XmlElement(name = "global-filter", type = GlobalDescriptor.class),
        @XmlElement(name = "adaptive-filter", type = AdaptiveDescriptor.class)
    })
    private FilterDescriptor filterDescriptor;

    /** Description data for each part. */
    @XmlElement(name = "part")
    private List<PartData> parts = new ArrayList<PartData>();

    /** Specific sheet parameters. */
    @XmlElement(name = "sheet")
    private List<SheetParameters> sheets = new ArrayList<SheetParameters>();

    //~ Constructors -------------------------------------------------------------------------------
    /** No-arg constructor needed by JAXB. */
    public ParametersTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        Book book = sheet.getBook();
        StringBuilder sb = new StringBuilder();

        // Score Binarization
        if (filterDescriptor != null) {
            if (book.getFilterParam().setSpecific(filterDescriptor)) {
                sb.append(" filter:").append(filterDescriptor);
            }
        }

        // Score Language
        if (language != null) {
            if (book.getLanguageParam().setSpecific(language)) {
                sb.append(" language:").append(language);
            }
        }

        //
        //        // Score Tempo
        //        if (tempo != null) {
        //            if (book.getTempoParam().setSpecific(tempo)) {
        //                sb.append(" tempo:").append(tempo);
        //            }
        //        }
        //
        //        // Score Parts
        //        for (int i = 0; i < parts.size(); i++) {
        //            try {
        //                ScorePart logicalPart = book.getPartList().get(i);
        //                PartData data = parts.get(i);
        //
        //                // Part name
        //                logicalPart.setName(data.name);
        //
        //                // Part midi program
        //                logicalPart.setMidiProgram(data.program);
        //            } catch (Exception ex) {
        //                logger.warn("Error in script Parameters part#" + (i + 1), ex);
        //            }
        //        }
        // Sheets
        for (SheetParameters params : sheets) {
            Sheet sh = book.getSheet(params.index);
            sb.append(" {page:").append(params.index);

            // Page Binarization
            if (params.filterDescriptor != null) {
                if (sh.getFilterParam().setSpecific(params.filterDescriptor)) {
                    sb.append(" filter:").append(params.filterDescriptor);
                }
            }

            // Page Language
            if (params.language != null) {
                if (sh.getLanguageParam().setSpecific(params.language)) {
                    sb.append(" language:").append(params.language);
                }
            }

            sb.append("}");
        }

        if (sb.length() > 0) {
            logger.info("{}parameters{}", book.getLogPrefix(), sb);
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
        Step latestStep = sheet.getLatestStep();

        for (Sheet sh : sheet.getBook().getSheets()) {
            Step from = null;

            // Language
            if (latestStep.compareTo(Step.TEXTS) >= 0) {
                LiveParam<String> param = sh.getLanguageParam();

                if (param.needsUpdate()) {
                    logger.debug("Page {} needs TEXT with {}", sh.getId(), param.getTarget());

                    // Convert the text items as much as possible
                    for (SystemInfo system : sh.getSystems()) {
                        system.getTextBuilder().switchLanguageTexts();
                    }

                    // Reprocess this page from SYMBOLS step
                    from = Step.SYMBOLS; // TODO !!!!!!!!!!!!
                }
            }

            // Binarization
            if (latestStep.compareTo(Step.BINARY) >= 0) {
                LiveParam<FilterDescriptor> param = sh.getFilterParam();

                if (param.needsUpdate()) {
                    logger.debug("Page {} needs BINARY with {}", sh.getId(), param.getTarget());
                    //  Reprocess this page from BINARY step
                    from = Step.BINARY;
                }
            }

            ///Stepping.reprocessSheet(from, sheet, null, true, false);
        }

        // Final PAGE step?
        if (latestStep == Step.PAGE) {
            ///Stepping.reprocessSheet(Step.PAGE, sheet, null, true, true);
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
     * @param sheet            not null for sheet setting
     */
    public void setFilter (FilterDescriptor filterDescriptor,
                           Sheet sheet)
    {
        if (sheet != null) {
            getParams(sheet).filterDescriptor = filterDescriptor;
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
     * @param sheet    not null for sheet setting
     */
    public void setLanguage (String language,
                             Sheet sheet)
    {
        if (sheet != null) {
            getParams(sheet).language = language;
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" parameters");

        if (filterDescriptor != null) {
            sb.append(" filter:").append(filterDescriptor);
        }

        if (language != null) {
            sb.append(" language:").append(language);
        }

        for (PartData data : parts) {
            sb.append(" ").append(data);
        }

        for (SheetParameters params : sheets) {
            sb.append(" ").append(params);
        }

        return sb.toString() + super.internals();
    }

    //-----------//
    // getParams //
    //-----------//
    /**
     * [May create and] report the parameters for the provided sheet.
     *
     * @param sheet the provided sheet
     * @return the page parameters
     */
    private SheetParameters getParams (Sheet sheet)
    {
        int index = sheet.getIndex();

        for (SheetParameters params : sheets) {
            if (params.index == index) {
                return params;
            }
        }

        // Not found, create one
        SheetParameters params = new SheetParameters();
        params.index = index;
        sheets.add(params);

        return params;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // PartData //
    //----------//
    public static class PartData
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Name of the part */
        @XmlAttribute
        public final String name;

        /** Midi Instrument */
        @XmlAttribute
        public final int program;

        //~ Constructors ---------------------------------------------------------------------------
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

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{name:" + name + " program:" + program + "}";
        }
    }

    //-----------------//
    // SheetParameters //
    //-----------------//
    /**
     * Parameters for a sheet.
     */
    public static class SheetParameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Sheet unique index. */
        @XmlAttribute(name = "index")
        private int index;

        /** Language code. */
        @XmlElement(name = "language")
        private String language;

        /** Pixel filter. */
        @XmlElements({
            @XmlElement(name = "global-filter", type = GlobalDescriptor.class),
            @XmlElement(name = "adaptive-filter", type = AdaptiveDescriptor.class)
        })
        private FilterDescriptor filterDescriptor;

        //~ Constructors ---------------------------------------------------------------------------
        //
        /** No-arg constructor needed by JAXB. */
        public SheetParameters ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{sheet#");
            sb.append(index);

            if (filterDescriptor != null) {
                sb.append(" filter:").append(filterDescriptor);
            }

            if (language != null) {
                sb.append(" language:").append(language);
            }

            sb.append("}");

            return sb.toString();
        }
    }
}
