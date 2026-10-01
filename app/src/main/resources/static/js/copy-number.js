// Fix 2.4 (walkthrough F-19): a Copy button beside the submission number, the contributor's only key to
// it. The Clipboard API exists only on HTTPS or localhost; on the plain-HTTP stand (DEBT-014) the
// button selects the number instead and says how to copy it (the human, 1 Oct).
// trace:FR-012
const button = document.querySelector('button.copy-number');
const number = document.querySelector('.submission-number');
const result = document.querySelector('.copy-result');

if (button && number && result) {
  button.hidden = false;
  button.addEventListener('click', async () => {
    const text = number.textContent.trim();
    if (navigator.clipboard && window.isSecureContext) {
      try {
        await navigator.clipboard.writeText(text);
        result.textContent = 'Copied';
        return;
      } catch {
        // Refused by the browser; selecting the number below still lets it be copied by hand.
      }
    }
    const range = document.createRange();
    range.selectNodeContents(number);
    const selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
    result.textContent = 'Selected: press Ctrl+C (⌘C on a Mac) to copy';
  });
}
