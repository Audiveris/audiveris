//----------------------------------------------------------------------------//
//                                                                            //
//                         a u t o a d v a n c e . j s                        //
//                                                                            //
// See http://tutorialzine.com/2011/01/how-to-make-auto-advancing-slideshows/ //
//----------------------------------------------------------------------------//
$(window).load(function(){

    // The window.load event guarantees that
    // all the images are loaded before the
    // auto-advance begins.

    var timeOut = null;

    $('#slideshow .arrow').click(function(e,simulated){
		
        // The simulated parameter is set by the
        // trigger method.
		
        if(!simulated){
			
            // A real click occured. Cancel the
            // auto advance animation.
			
            clearTimeout(timeOut);
        }
    });

    // A self executing named function expression:
	
    (function autoAdvance(){
		
        // Simulating a click on the next arrow.
        $('#slideshow .next').trigger('click',[true]);
		
        // Schedulling a time out in 5 seconds.
        timeOut = setTimeout(autoAdvance,5000);		
    })();

});