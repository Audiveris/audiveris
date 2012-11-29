/* -------------------------------------------------------------------------- */
/*                           e x p a n s i o n . j s                          */
/* -------------------------------------------------------------------------- */

// Choice between smooth/immediate expansions
// Can be overwritten in the HTML source file
smoothExpansion = true;

// The mechanism uses additional items (myFullHeight and myExpanded)
// created in each expandable element

// Remember height of all expandable elements in the document
// And precollapse all of them
function collapseAll() {
    if (document.querySelector) {
        var exps = document.querySelectorAll(".expandable");
        for (var i = 0; i < exps.length; i++) {
            if (smoothExpansion) {
                exps[i].style.myFullHeight = exps[i].clientHeight;
                exps[i].style.height = 0;
            } else {
                exps[i].style.display = "none";
            }
            exps[i].style.myExpanded = false;
        }
    }
}

// Toggle the expansion of all expandable elements
// that are siblings of the provided expander element
function toggleExpansion(expander) {
    if (document.querySelector) {
        var exps = expander.parentNode.querySelectorAll(".expandable");
        for (var i = 0; i < exps.length; i++) {
            if (exps[i].style.myExpanded) {
                // Collapse 
                if (smoothExpansion) {
                    exps[i].style.height = 0;
                } else {
                    exps[i].style.display = "none";
                }
                exps[i].style.myExpanded = false;
            } else {
                // Expand 
                if (smoothExpansion) {
                    exps[i].style.height = exps[i].style.myFullHeight + "px";
                } else {
                    exps[i].style.display = "block";
                }
                exps[i].style.myExpanded = true;
            }
        }
    }
}

// Toggle the expansion and the expander input
function toggleInput(input) {

    toggleExpansion(input);
    
    // Toggle input value
    input.value = input.value === '+' ? '-' : '+';
}
