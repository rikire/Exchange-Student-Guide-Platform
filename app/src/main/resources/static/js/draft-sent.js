/*
 * On the confirmation page: the submission succeeded, so the draft the form kept in this browser is
 * cleared (FR-027). editor.js names the draft when the form is submitted.
 */
(function () {
  'use strict';

  try {
    var sent = sessionStorage.getItem('guide:draft-sent');
    if (sent) {
      localStorage.removeItem(sent);
      sessionStorage.removeItem('guide:draft-sent');
    }
  } catch (e) {
    // Storage is off in this browser, so there is no draft to clear.
  }
})();
