/**
 * Package for handling textual aspects in glyphs.
 * <p>
 * <img src="doc-files/Text.png" alt="Text" title="Text data model" />
 * <p>
 * <h3>Detection of text items</h3>
 * <p>
 * The purpose of the TEXTS step is to run Tesseract OCR on a sheet image.
 * We use the SegmentationMode.AUTO mode of OCR, which thus performs the image layout analysis and
 * then interprets each of the detected lines.
 * <p>
 * The OCR output (lines, words and chars) is translated to Audiveris TextLine, TextWord and
 * TextChar instances which are re-composed (lines and words may get merged and split).
 * The final TextLines are kept at SystemInfo level.
 * <p>
 * <h3>Detection of text-shaped glyphs (NOTA: THIS IS OBSOLETE!)</h3>
 * <p>
 * Later, in SYMBOLS step, some glyphs might be recognized as TEXT shaped glyphs (or have been
 * manually assigned the TEXT shape).
 * The OCR is then launched in SINGLE_BLOCK mode on just the glyph image, and the resulting line is
 * then re-composed with the other SystemInfo TextLine instances.
 */
package org.audiveris.omr.text;
