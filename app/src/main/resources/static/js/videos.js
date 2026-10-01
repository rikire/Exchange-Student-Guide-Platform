// Walkthrough fix 1.4: a video the browser cannot decode (a MOV of HEVC in Chrome, an MKV of a codec
// it lacks) is swapped for its card, which says to download it; the Download link under it stays. The
// player may have failed before this module ran, so a failure already recorded counts too.
// trace:FR-001
for (const video of document.querySelectorAll('video.media-video')) {
  const card = video.nextElementSibling;
  const showCard = () => {
    video.hidden = true;
    card.hidden = false;
  };
  if (video.error) {
    showCard();
  } else {
    video.addEventListener('error', showCard);
  }
}
