/**
 * Package for processing of a sheet image.
 */
@XmlJavaTypeAdapters(
{
        @XmlJavaTypeAdapter(type = MusicFamily.MyParam.class, value = MusicFamily.MyParam.JaxbAdapter.class),
        @XmlJavaTypeAdapter(type = TextFamily.MyParam.class, value = TextFamily.MyParam.JaxbAdapter.class),
        @XmlJavaTypeAdapter(type = InputQualityParam.class, value = InputQualityParam.JaxbAdapter.class),
        @XmlJavaTypeAdapter(type = IntegerParam.class, value = IntegerParam.JaxbAdapter.class),
        @XmlJavaTypeAdapter(type = BarlineHeight.class, value = BarlineHeight.JaxbAdapter.class),
        @XmlJavaTypeAdapter(type = StringParam.class, value = StringParam.JaxbAdapter.class),
        @XmlJavaTypeAdapter(type = ProcessingSwitches.class, value = ProcessingSwitches.JaxbAdapter.class) })

package org.audiveris.omr.sheet;

import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.util.param.IntegerParam;
import org.audiveris.omr.util.param.StringParam;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
