/* -------------------------------------------------------------------------- */
/*                           s l i d e s h o w . j s                          */
/* -------------------------------------------------------------------------- */

// Global parameters
var slideshow = {
    period: 5000,
    fading: 1000,
    ease: "swing",
    timeout: null
};

$(window).load(function() {
    var slides = $("#slideshow .slides li");
    var labels = $("#slideshow .labels li");
    var status = $("#slideshow #status");

    var currentIndex = slides.length - 1; // Zero-based index of current slide

    // Click on prev/next commands
    $('#slideshow .command').click(function() {

        // Compute index of target slide
        var delta = $(this).hasClass('next') ? 1 : -1;
        var nextIndex = (currentIndex + delta + slides.length) % slides.length;

        moveTo(nextIndex);
    });

    // Simulated click on prev/next commands
    $('#slideshow .command').click(function(e, simulated) {
        if (!simulated) {
            stop();
        }
    });

    // Click on a label
    $("#slideshow ul.labels li").click(function() {
        // Retrieve index for this label
        var index = $(this).index();
        stop();
        moveTo(index);
    });

    // Click on a slide
    $("#slideshow ul.slides li").click(function() {
        if (slideshow.timeout) {
            stop();
        } else {
            start(0);
        }
    });

    function moveTo(nextIndex) {
        if (nextIndex !== currentIndex) {
            var slide = slides.eq(currentIndex);
            var nextSlide = slides.eq(nextIndex);
            var label = labels.eq(currentIndex);
            var nextLabel = labels.eq(nextIndex);

            label.removeClass("active");
            nextLabel.addClass("active");

            nextSlide.fadeIn(slideshow.fading, slideshow.ease, function() {
                currentIndex = nextIndex;

                // Show the next slide
                slide.removeClass("active");
                nextSlide.addClass("active");

                // Fade the current slide
                slide.fadeOut(slideshow.fading, slideshow.ease, function() {
                    nextSlide.show();
                });
            });
        }
    }

    autoAdvance = function() {
        // Simulate a click on the ".next" command.
        $('#slideshow .next').trigger('click', true);

        // Launch the next occurrence
        start(slideshow.period);
    };

    // Start the timer
    function start(period) {
        if (slideshow.period >= 0) {
            slideshow.timeout = setTimeout(autoAdvance, period);
            status.removeClass("paused");
            status.fadeOut();
        }
    }

    // Stop the timer
    function stop() {
        clearTimeout(slideshow.timeout);
        slideshow.timeout = null;
        status.addClass("paused");
        status.fadeIn();
    }

    // Initialize on first slide
    moveTo(0);

    // Launch the timer
    start(slideshow.period);
});