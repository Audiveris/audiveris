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
	    	int picx = getWidth();
	    	int picy = getHeight();
			int[][] sums = new int[picx][picy];
			int srcPixelValue;
			for (int x = 0; x < picx; x++) {
				for (int y = 0; y < picy; y++) {
					srcPixelValue = getPixel(x, y);
					// For the leftmost pixel, just copy value from original
					if (x == 0 && y == 0) {
						sums[x][y] = srcPixelValue;
					}

					// For the first x, just add the value to the left of this
					// pixel
					else if (x == 0) {
						sums[x][y] = srcPixelValue
								+ sums[x][y - 1];
					}

					// For the first y, just add the value to the top of this
					// pixel
					else if (y == 0) {
						sums[x][y] = srcPixelValue
								+ sums[x - 1][y];
					}

					// For a pixel that has pixels to its left, above it, and to the
					// left and above diagonally,
					// add the left and above values and subtract the value to the
					// left and above diagonally
					else {
						sums[x][y] = srcPixelValue
								+ sums[x][y - 1]
								+ sums[x - 1][y]
								- sums[x - 1][y - 1];
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
			for (int x = 0; x < sqrsums.length; x++) {
				sqrsums[x] = new int[sum[x].length];
				for (int y = 0; y < sqrsums[x].length; y++) {
					sqrsums[x][y] = sum[x][y]*sum[x][y];
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
			y2 = Math.min(y+windowSize/2+1, sumpixs[x2-1].length);
			mean =  (a + d - b - c)/((y2-y1)*(x2-x1));
			return mean;
		}
		
    	//-------------//
		// getSqrMean //
    	//-------------//
		public double getSqrMean(int x, int y, int windowSize){
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
