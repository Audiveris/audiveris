package omr.lag;

/**
 * Abstract {@code AbstractPixelSource} implements Interface {@code PixelSource}
 * to provide an template class for obtaining a integral image so that it can 
 * calculate local threshold at a constant
 * time.
 * @author ryo/twitter @xiaot_Tag
 */
public abstract class AbstractPixelSource implements PixelSource {
    /** Default value window size */
    public static final int WINDOWSIZE = 19;
    /** hold integral image sum pixs for calculating mean*/
	private int[][] sumpixs;
	/** hold integral image square sum pixs for calculating mean */
	private int[][] sqrsumpixs; 

    //-------------//
    // getSumPixs //
    //-------------//
    /**
     * get integral image sum pixels
     */
	private int[][] getSumPixs(){
	    	int picrow = getWidth();
	    	int piccolumn = getHeight();
			int[][] sums = new int[picrow][piccolumn];
			int srcPixelValue;
			for (int row = 0; row < picrow; row++) {
				for (int column = 0; column < piccolumn; column++) {
					srcPixelValue = getPixel(row, column);
					// For the leftmost pixel, just copy value from original
					if (row == 0 && column == 0) {
						sums[row][column] = srcPixelValue;
					}

					// For the first row, just add the value to the left of this
					// pixel
					else if (row == 0) {
						sums[row][column] = srcPixelValue
								+ sums[row][column - 1];
					}

					// For the first column, just add the value to the top of this
					// pixel
					else if (column == 0) {
						sums[row][column] = srcPixelValue
								+ sums[row - 1][column];
					}

					// For a pixel that has pixels to its left, above it, and to the
					// left and above diagonally,
					// add the left and above values and subtract the value to the
					// left and above diagonally
					else {
						sums[row][column] = srcPixelValue
								+ sums[row][column - 1]
								+ sums[row - 1][column]
								- sums[row - 1][column - 1];
					}
				}
			}
			return sums;
	    }
	
    	//-------------//
		// getSqrSumPixs //
    	//-------------//
    	/**
    	 * get integral image square sum pixels
    	 */
	    private int[][] getSqrSumPixs() {
			int [][] sum  = this.sumpixs;
			int [][] sqrsums = new int[sum.length][];	
			for (int row = 0; row < sqrsums.length; row++) {
				sqrsums[row] = new int[sum[row].length];
				for (int column = 0; column < sqrsums[row].length; column++) {
					sqrsums[row][column] = sum[row][column]*sum[row][column];
				}
			}
			return sqrsums;
		}
	    
	    public void computerintegral(){
	    	this.sumpixs = getSumPixs();
	    	this.sqrsumpixs = getSqrSumPixs();
	    }
	    
    	//-------------//
		// getMean //
    	//-------------//
		public double getMean(int x, int y, int windowSize) {
			int [][] sumpixs = this.sumpixs;
			double mean = 0;
			int x1 = x-windowSize/2;
			int x2 = x+windowSize/2;
			int y1 = y-windowSize/2;
			int y2 =y+windowSize/2;
			
			//straight forward way to handle out of bound x1,y1. code is ugly but do the work.
			double a,b,c,d;
			try {
			    a = sumpixs[x1][y1];
			} catch ( IndexOutOfBoundsException e ) {
				a=0;  
			}
			try {
			    b = sumpixs[x2][y1];
			} catch ( IndexOutOfBoundsException e ) {
				b=0;  
			}
			try {
			    c = sumpixs[x1][y2];
			} catch ( IndexOutOfBoundsException e ) {
				c=0;  
			}
			try {
			    d = sumpixs[x2][y2];
			} catch ( IndexOutOfBoundsException e ) {
				d=0;  
			}		
			
			x1 = Math.max(x-windowSize/2, 0);
			x2 = Math.min(x+windowSize/2+1, sumpixs.length);
			y1 = Math.max(y-windowSize/2, 0);
			y2 = Math.min(y+windowSize/2+1, sqrsumpixs[x2-1].length);
			mean =  (a + d - b - c)/((y2-y1)*(x2-x1));
			return mean;
		}
		
    	//-------------//
		// getSqrMean //
    	//-------------//
		public double getSqrMean(int x, int y, int windowSize){
			int [][] sqrsumpixs = this.sqrsumpixs;
			double sqmean = 0;
			int x1 = x-windowSize/2;
			int x2 = x+windowSize/2;
			int y1 = y-windowSize/2;
			int y2 =y+windowSize/2;
			
			//same as above 
			double a,b,c,d;
			try {
			    a = sqrsumpixs[x1][y1];
			} catch ( IndexOutOfBoundsException e ) {
				a=0;  
			}
			try {
			    b = sqrsumpixs[x2][y1];
			} catch ( IndexOutOfBoundsException e ) {
				b=0;  
			}
			try {
			    c = sqrsumpixs[x1][y2];
			} catch ( IndexOutOfBoundsException e ) {
				c=0;  
			}
			try {
			    d = sqrsumpixs[x2][y2];
			} catch ( IndexOutOfBoundsException e ) {
				d=0;  
			}		
			x1 = Math.max(x-windowSize/2, 0);
			x2 = Math.min(x+windowSize/2+1, sqrsumpixs.length);
			y1 = Math.max(y-windowSize/2, 0);
			y2 = Math.min(y+windowSize/2+1, sqrsumpixs[x2-1].length);
			sqmean =  (a + d - b - c)/((y2-y1)*(x2-x1)*(y2-y1)*(x2-x1));
			return sqmean;
		}
}
