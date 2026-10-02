/*
 * Fix 3.6: Tom Select over the form's tag list (ADR-0022). The server renders a <select multiple>
 * with every tag in use as an option and the submission's own selected; this makes it a field where
 * a word becomes a chip on Enter or a comma, × removes it, and stored tags are suggested while typing.
 * The post is unchanged: one tags field per chip. The limit is the server's (Tags.MOST, data-most).
 */
(function () {
  'use strict';

  if (typeof TomSelect === 'undefined') {
    return;
  }
  document.querySelectorAll('select.tag-select').forEach(function (select) {
    new TomSelect(select, {
      create: true,
      // A word typed but not yet confirmed is kept when the field loses focus, as on submitting.
      createOnBlur: true,
      persist: false,
      delimiter: ',',
      maxItems: Number(select.getAttribute('data-most')),
      plugins: { remove_button: { title: 'Remove this tag' } },
      // Tom Select keeps the search after a suggestion is clicked, so the next word would be typed
      // onto it ("vi" + "hostel"); a tag field wants a fresh word each time.
      onItemAdd: function () {
        this.setTextboxValue('');
        this.refreshOptions(false);
      }
    });
  });
})();
