/*
 * Fix 3.6: FilePond over the form's file field (ADR-0022). A drop zone that shows the chosen file's
 * name and size, a preview of a photo, and a way to remove it. storeAsFile keeps the file in a file
 * input of the form, so it is posted as attachment with everything else and nothing is uploaded on
 * its own. The type and the limits are still the server's to check (FEAT-009).
 */
(function () {
  'use strict';

  if (typeof FilePond === 'undefined') {
    return;
  }
  if (typeof FilePondPluginImagePreview !== 'undefined') {
    FilePond.registerPlugin(FilePondPluginImagePreview);
  }
  document.querySelectorAll('input.file-drop').forEach(function (input) {
    FilePond.create(input, {
      storeAsFile: true,
      credits: false,
      labelIdle: 'Drop a photo, document or video here, or <span class="filepond--label-action">choose a file</span>',
      labelButtonRemoveItem: 'Remove this file',
      imagePreviewMaxHeight: 240
    });
  });
})();
