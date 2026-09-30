// FR-032 (ADR-0018): every photo on the page, in the article's text or attached, opens full screen in
// PhotoSwipe, zooms, and pages on to the others in the order they appear. The version in these paths
// is the root pom's photoswipe.version.
// trace:FR-032
import PhotoSwipeLightbox from '/webjars/photoswipe/5.4.3/dist/photoswipe-lightbox.esm.min.js';

const photos = Array.from(document.querySelectorAll('.article-body img, .media-photo img'));

if (photos.length > 0) {
  const lightbox = new PhotoSwipeLightbox({
    pswpModule: () => import('/webjars/photoswipe/5.4.3/dist/photoswipe.esm.min.js'),
    // Past the photo's own size, up to twice it; further only blurs (decided 30 Sep).
    secondaryZoomLevel: 1.5,
    maxZoomLevel: 2,
    // The page's photos are the gallery, so a click on the dark background closes rather than zooms.
    bgClickAction: 'close',
  });
  lightbox.init();

  // PhotoSwipe needs each photo's size. It is read from the photo itself, so nothing is stored for it;
  // an attached photo is lazy, so the photos are loaded before the viewer opens.
  const open = async (index) => {
    await Promise.all(photos.map((photo) => {
      photo.loading = 'eager';
      return photo.decode().catch(() => undefined);
    }));
    const slides = photos.map((photo) => ({
      src: photo.currentSrc || photo.src,
      width: photo.naturalWidth,
      height: photo.naturalHeight,
      alt: photo.alt,
      element: photo,
    }));
    lightbox.loadAndOpen(index, slides);
  };

  photos.forEach((photo, index) => {
    // A photo is a control now: reachable with Tab, named for what it does (NFR-007).
    photo.setAttribute('role', 'button');
    photo.setAttribute('tabindex', '0');
    photo.setAttribute('aria-label', photo.alt ? `Open photo: ${photo.alt}` : 'Open photo');
    photo.classList.add('photo-opens');
    photo.addEventListener('click', () => open(index));
    photo.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        open(index);
      }
    });
  });
}
