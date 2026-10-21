// Loaded in <head> so this listener is in place before any image starts loading.
// Error events don't bubble, so listen in the capture phase. Removing a broken image
// reveals the placeholder rendered underneath it (no inline handlers, so a future CSP still works).
document.addEventListener('error', function (event) {
    var target = event.target;
    if (target instanceof HTMLImageElement && target.hasAttribute('data-fallback')) {
        target.remove();
    }
}, true);
