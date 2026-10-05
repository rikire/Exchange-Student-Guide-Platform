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
    var field = new TomSelect(select, {
      create: true,
      // A word typed but not yet confirmed is kept when the field loses focus, as on submitting.
      createOnBlur: true,
      persist: false,
      // The list opens under the field, over "Submit for review"; left open after a tag, a click on
      // the button picked the stored tag under it and sent nothing (the demo rehearsal, 5 Oct).
      closeAfterSelect: true,
      delimiter: ',',
      maxItems: Number(select.getAttribute('data-most')),
      // Tom Select refilters the list 300 ms after typing, and Enter acts on the option lit before
      // that; a run on 2 Oct turned "hostel" into "arrival" (DEBT-023). The list is the page's own,
      // at most 500 names, so refiltering on every key costs nothing.
      refreshThrottle: 0,
      plugins: { remove_button: { title: 'Remove this tag' } },
      // Tom Select keeps the search after a suggestion is clicked, so the next word would be typed
      // onto it ("vi" + "hostel"); a tag field wants a fresh word each time.
      onItemAdd: function () {
        this.setTextboxValue('');
        this.refreshOptions(false);
      }
    });
    addButtonTo(field);
  });

  // Fix 3.7 (the human, 8 Oct): an "Add tag" button beside the field, for whoever does not know that
  // Enter or a comma makes the chip. Pressing it moves the focus off the field, and createOnBlur has
  // already made the typed word a chip by the time the click arrives; with nothing typed, the click
  // puts the cursor in the field. createItem covers a click that did not take the focus first.
  function addButtonTo(field) {
    var button = document.createElement('button');
    button.type = 'button';
    button.className = 'button-secondary tag-add';
    button.textContent = 'Add tag';
    button.addEventListener('click', function () {
      if (field.inputValue().trim()) {
        field.createItem();
      }
      field.focus();
    });
    var row = document.createElement('div');
    row.className = 'tag-row';
    field.wrapper.before(row);
    row.append(field.wrapper, button);
  }
})();
