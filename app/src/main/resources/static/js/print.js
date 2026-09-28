/*
 * On the article page: "Save as PDF" opens the browser's print, where the reader chooses to save the
 * page as a PDF (FR-030). The browser draws the PDF, so Hindi and Tamil keep their shaping; the print
 * styles in site.css leave the site around the article out.
 *
 * trace:FR-030
 */
(function () {
  'use strict';

  var button = document.querySelector('button[data-print]');
  if (!button) {
    return;
  }
  button.hidden = false;
  button.addEventListener('click', function () {
    window.print();
  });
})();
