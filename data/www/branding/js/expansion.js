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
            var exp = exps[i];
            exp.style.myFullHeight = exp.clientHeight;
            collapse(exp);
        }
    }

    // Pre-expand only the ones flagged as .expanded
    preExpand();
}

// Pre-expand the classes flagged as such
function preExpand() {
    if (document.querySelector) {
        var exps = document.querySelectorAll(".expanded");
        for (var i = 0; i < exps.length; i++) {
            expand(exps[i]);
        }
    }
}

// Toggle the expansion of all expandable elements
// that are siblings of the provided expander element
function toggleExpansion(expander) {
    if (document.querySelector) {
        var exps = expander.parentNode.querySelectorAll(".expandable");
        for (var i = 0; i < exps.length; i++) {
            var exp = exps[i];
            if (exp.style.myExpanded) {
                collapse(exp);
            } else {
                expand(exp);
            }
        }
    }
}

// Collapse the provided expandable
function collapse(exp) {
    if (smoothExpansion) {
        exp.style.height = 0;
    } else {
        exp.style.display = "none";
    }
    exp.style.myExpanded = false;
}

// Expand the provided expandable
function expand(exp) {
    if (smoothExpansion) {
        exp.style.height = exp.style.myFullHeight + "px";
    } else {
        exp.style.display = "block";
    }
    exp.style.myExpanded = true;
}

// Toggle the expansion and the expander input
function toggleInput(input) {

    toggleExpansion(input);

    // Toggle input value
    input.value = input.value === '+' ? '-' : '+';
}
