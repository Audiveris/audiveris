//----------------------------------------------------------------------------//
//                                                                            //
//                      R u n s T a b l e F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.lag.AbstractPixelSource;
import omr.lag.PixelSource;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import java.awt.Dimension;
import java.util.Arrays;

/**
 * Class {@code RunsTableFactory} retrieves the runs structure out of a given
 * pixel source and builds the related {@link RunsTable} structure.
 * 
 * @author Hervé Bitteur
 */
public class RunsTableFactory {
	// ~ Static fields/initializers
	// ---------------------------------------------

	/** Usual logger utility */
	private static final Logger logger = Logger
			.getLogger(RunsTableFactory.class);

	// ~ Instance fields
	// --------------------------------------------------------
	/** The source to read runs of pixels from */
	private final PixelSource source;

	/** The desired orientation */
	private final Orientation orientation;

	/** The maximum pixel gray level to be foreground */
	@Deprecated
	private final int maxLevel;

	/** The minimum value for a run length to be considered */
	private final int minLength;

	/** Remember if we have to swap x and y coordinates */
	private final boolean swapNeeded;

	/** The created RunsTable */
	private RunsTable table;

	// ~ Constructors
	// -----------------------------------------------------------
	// ------------------//
	// RunsTableFactory //
	// ------------------//
	/**
	 * Create an RunsTableFactory, with its key parameters.
	 * 
	 * @param orientation
	 *            the desired orientation of runs
	 * @param source
	 *            the source to read runs from. Orientation parameter is used to
	 *            properly access the source pixels.
	 * @param maxLevel
	 *            maximum gray level to be a foreground pixel
	 * @param minLength
	 *            the minimum length for each run
	 */
	public RunsTableFactory(Orientation orientation, PixelSource source,
			int maxLevel, int minLength) {
		this.orientation = orientation;
		this.source = source;
		this.minLength = minLength;
		this.maxLevel = maxLevel;

		swapNeeded = orientation.isVertical();
	}

	// ~ Methods
	// ----------------------------------------------------------------
	// ------------//
	// createTable //
	// ------------//
	/**
	 * Report the RunsTable created with the runs retrieved from the provided
	 * source.
	 * 
	 * @param name
	 *            the name to be assigned to the table
	 * @return a populated RunsTable
	 */
	public RunsTable createTable(String name) {
		table = new RunsTable(name, orientation, new Dimension(
				source.getWidth(), source.getHeight()));

		RunsRetriever retriever = new RunsRetriever(orientation,
				new MyAdapter());

		retriever.retrieveRuns(new PixelRectangle(0, 0, source.getWidth(),
				source.getHeight()));

		return table;
	}

	// ~ Inner Classes
	// ----------------------------------------------------------
	// -----------//
	// MyAdapter //
	// -----------//
	private class MyAdapter implements RunsRetriever.Adapter {
        //~ Constructors -------------------------------------------------------
        //---------//
        // Adapter //
        //---------//
        public MyAdapter ()
        {
        	((AbstractPixelSource)source).computerintegral();
        }
		
		
		
		// ~ Methods
		// ------------------------------------------------------------

		// ---------//
		// backRun //
		// ---------//
		/**
		 * Call-back called when a background run has been built
		 * 
		 * @param coord
		 *            coordinate of run start
		 * @param pos
		 *            position of run start
		 * @param length
		 *            run length
		 */
		@Override
		public final void backRun(int coord, int pos, int length) {
			// No interest in background runs
		}

		// ---------//
		// foreRun //
		// ---------//
		/**
		 * Call-back called when a foreground run has been built
		 * 
		 * @param coord
		 *            coordinate of run start
		 * @param pos
		 *            position of run start
		 * @param length
		 *            run length
		 * @param cumul
		 *            cumulated pixel gray levels on all run points
		 */
		@Override
		public final void foreRun(int coord, int pos, int length, int cumul) {
			// We consider only runs that are longer than minLength
			if (length >= minLength) {
				final int level = ((2 * cumul) + length) / (2 * length);
				table.getSequence(pos).add(
						new Run(coord - length, length, level));
			}
		}

		// ----------//
		// getLevel //
		// ----------//
		/**
		 * Retrieve the pixel gray level of a point in the underlying source
		 * 
		 * @param coord
		 *            coordinate value, relative to lag orientation
		 * @param pos
		 *            position value, relative to lag orientation
		 * 
		 * @return pixel gray level
		 */
		@Override
		public final int getLevel(int coord, int pos) {
			if (swapNeeded) {
				return source.getPixel(pos, coord);
			} else {
				return source.getPixel(coord, pos);
			}
		}

		// --------//
		// isFore //
		// --------//
		/**
		 * Check whether the provide pixel value is foreground or background
		 * 
		 * @param level
		 *            pixel gray level
		 * @deprecated
		 * @return true if foreground, false if background
		 */
		@Override
		public final boolean isFore(int level) {
			throw new UnsupportedOperationException("unsupport");
			// return level <= maxLevel;
		}

		// -----------//
		// terminate //
		// -----------//
		/**
		 * Method called-back when all runs have been read
		 */
		@Override
		public final void terminate() {
			logger.fine("{0} Retrieved runs: {1}",
					new Object[] { table, table.getRunCount() });
		}

		// --------//
		// isForelocaltres //
		// --------//
		/**
		 * Check whether the provide pixel value is foreground or background
		 * 
		 * @param coord coordinate value, relative to lag orientation
		 * @param pos position value, relative to lag orientation
		 * 
		 * @return true if foreground, false if background
		 * TODO: need to move to an abstract adapter class to reduce the duplication with {@code ScaleBuilder} Adapter
		 */
		@Override
		public boolean isForelocaltres(int coord, int pos) {
			double var = 0, mean = 0, sqmean = 0;
			if (swapNeeded) {			
				mean = source.getMean(pos, coord, WINDOWSIZE);
				sqmean = source.getSqrMean(pos, coord, WINDOWSIZE);
			} else {			
				mean = source.getMean(coord, pos, WINDOWSIZE);
				sqmean = source.getSqrMean(coord, pos, WINDOWSIZE);
			}
			var = Math.abs(sqmean - mean * mean);
			int originPixValue = getLevel(coord, pos);
			double threshold = 255 - (255 - mean)
					* (1 + K * (Math.sqrt(var) / 128 - 1));
			boolean isFore = originPixValue < threshold;
			return isFore;
		}
	}
}
