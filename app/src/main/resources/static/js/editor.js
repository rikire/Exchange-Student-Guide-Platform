/*
 * FR-027: EasyMDE on the body of the submission form (ADR-0013). Without this script the form is a
 * plain textarea and still submits.
 *
 * - The preview is the server's rendering (POST /contribute/preview), asked for when typing pauses;
 *   an answer that arrives after a newer one is dropped.
 * - The draft is EasyMDE's autosave, one per form: a new article, or an edit of one address.
 *   EasyMDE clears it when the form is submitted, before the server has answered; FR-027 keeps it
 *   until the submission succeeds, so that listener is suppressed and the confirmation page clears
 *   the draft named here (draft-sent.js). The case it saves: a form over the container's limit comes
 *   back as an empty form (413, UploadTooLargeAdvice), and the draft puts the text back into it.
 * - Icons are our own sprite, not Font Awesome from a CDN; nothing is downloaded from elsewhere.
 */
(function () {
  'use strict';

  var textarea = document.querySelector('textarea[data-draft]');
  if (!textarea || typeof EasyMDE === 'undefined') {
    return;
  }
  var form = textarea.form;
  var token = form.querySelector('input[name="_csrf"]');
  var draft = 'guide:' + textarea.getAttribute('data-draft');
  var PAUSE = 300;

  function icon(name) {
    return '<svg class="editor-icon" aria-hidden="true" focusable="false">'
      + '<use href="/img/editor-icons.svg#' + name + '"></use></svg>';
  }

  function button(name, action, title) {
    return { name: name, action: action, title: title, icon: icon(name) };
  }

  // Fix 3.6: [[ ]] around the selection, or empty with the cursor between them to type a title.
  function wikiLink(editor) {
    var cm = editor.codemirror;
    var selected = cm.getSelection();
    cm.replaceSelection('[[' + selected + ']]');
    if (!selected) {
      var cursor = cm.getCursor();
      cm.setCursor({ line: cursor.line, ch: cursor.ch - 2 });
    }
    cm.focus();
  }

  var timer;
  var asked = 0;
  var shown = 0;

  function renderOnPause(text, preview) {
    clearTimeout(timer);
    timer = setTimeout(function () {
      var mine = ++asked;
      var body = new URLSearchParams();
      body.append('_csrf', token ? token.value : '');
      body.append('body', text);
      fetch('/contribute/preview', { method: 'POST', body: body, credentials: 'same-origin' })
        .then(function (response) { return response.text(); })
        .then(function (html) {
          if (mine > shown) {
            shown = mine;
            preview.innerHTML = html;
          }
        })
        .catch(function () {
          if (mine > shown) {
            shown = mine;
            preview.textContent = 'The preview is not available right now. Your text is kept.';
          }
        });
    }, PAUSE);
    return null;
  }

  var editor = new EasyMDE({
    element: textarea,
    forceSync: true,
    // inputStyle is left to CodeMirror: a hidden textarea on a desktop, contenteditable on a phone.
    // Forced to contenteditable, Hindi, Tamil and emoji typed in desktop Chromium were dropped
    // (BrowserEditorTest, 28 Sep); whether a phone's keyboard fares better is the phone check's.
    autoDownloadFontAwesome: false,
    spellChecker: false,
    nativeSpellcheck: true,
    status: false,
    sideBySideFullscreen: false,
    previewClass: ['editor-preview', 'article-body'],
    previewRender: renderOnPause,
    // binded: EasyMDE's own "clear on submit" listener stays off; see the comment at the top.
    autosave: { enabled: true, uniqueId: draft, delay: 1000, binded: true },
    toolbar: [
      button('bold', EasyMDE.toggleBold, 'Bold'),
      button('italic', EasyMDE.toggleItalic, 'Italic'),
      button('heading', EasyMDE.toggleHeadingSmaller, 'Heading'),
      '|',
      button('quote', EasyMDE.toggleBlockquote, 'Quote'),
      button('unordered-list', EasyMDE.toggleUnorderedList, 'Bulleted list'),
      button('ordered-list', EasyMDE.toggleOrderedList, 'Numbered list'),
      '|',
      button('link', EasyMDE.drawLink, 'Link'),
      button('wikilink', wikiLink, 'Link to an article in the guide')
    ]
  });

  // The hidden textarea cannot show the browser's "fill this in" message, and a required field it
  // cannot focus blocks the submit silently; the server refuses an empty body instead.
  textarea.required = false;

  // CodeMirror's own input has no label; the one on the page belongs to the hidden textarea.
  editor.codemirror.getInputField().setAttribute('aria-label', 'Body (Markdown)');

  // EasyMDE binds Tab to indenting, so a keyboard user could not leave the editor (WCAG 2.1.2).
  // False hands both keys back to the browser; a list is nested with spaces instead.
  var keys = Object.assign({}, editor.codemirror.getOption('extraKeys'), { Tab: false, 'Shift-Tab': false });
  editor.codemirror.setOption('extraKeys', keys);

  // EasyMDE takes its buttons out of the tab order; the keyboard shortcuts alone are not discoverable.
  // Its title is a tooltip only; a screen reader is given the same name (fix 3.6).
  Object.keys(editor.toolbarElements).forEach(function (name) {
    var control = editor.toolbarElements[name];
    control.tabIndex = 0;
    control.setAttribute('aria-label', control.title);
  });

  form.addEventListener('submit', function () {
    try {
      sessionStorage.setItem('guide:draft-sent', 'smde_' + draft);
    } catch (e) {
      // Without sessionStorage the draft outlives the submission; the text is still sent.
    }
  });

  editor.toggleSideBySide();
})();
