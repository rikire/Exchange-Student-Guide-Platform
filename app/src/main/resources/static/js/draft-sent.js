/*
 * On the confirmation page: the submission succeeded, so the draft the form kept in this browser is
 * cleared (FR-027). editor.js names the draft when the form is submitted.
 */
(function () {
  'use strict';

  try {
    // The keys of the body's draft and of the title and summary saved beside it (editor.js).
    var sent = JSON.parse(sessionStorage.getItem('guide:draft-sent') || '[]');
    sent.forEach(function (key) {
      localStorage.removeItem(key);
    });
    sessionStorage.removeItem('guide:draft-sent');
  } catch (e) {
    // Storage is off in this browser, or holds a value from before 2 Oct: there is no draft to clear.
  }
})();
