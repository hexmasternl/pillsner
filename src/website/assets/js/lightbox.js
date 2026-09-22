// Shows an app screenshot full size, centred on a dimmed backdrop, when its
// thumbnail is clicked. Clicking anywhere (or pressing Escape) closes it again.
// Without JavaScript the thumbnail is a plain link to the full-size image, so
// nothing is lost. No external requests, no storage, no tracking.
(function () {
  "use strict";

  var overlay = null;
  var lastFocused = null;

  function close() {
    if (!overlay) {
      return;
    }
    document.body.classList.remove("lightbox-open");
    overlay.remove();
    overlay = null;
    if (lastFocused && typeof lastFocused.focus === "function") {
      lastFocused.focus();
    }
  }

  function open(href, alt, closeLabel) {
    close();
    overlay = document.createElement("div");
    overlay.className = "lightbox";
    overlay.setAttribute("role", "dialog");
    overlay.setAttribute("aria-modal", "true");
    overlay.setAttribute("aria-label", alt || "");

    var image = document.createElement("img");
    image.className = "lightbox__image";
    image.src = href;
    image.alt = alt || "";
    overlay.appendChild(image);

    var button = document.createElement("button");
    button.type = "button";
    button.className = "lightbox__close";
    button.setAttribute("aria-label", closeLabel);
    button.textContent = "\u00d7";
    overlay.appendChild(button);

    overlay.addEventListener("click", close);
    document.body.appendChild(overlay);
    document.body.classList.add("lightbox-open");
    button.focus();
  }

  document.addEventListener("click", function (event) {
    var trigger = event.target.closest && event.target.closest("a[data-lightbox]");
    if (!trigger) {
      return;
    }
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) {
      return;
    }
    event.preventDefault();
    lastFocused = trigger;
    open(
      trigger.getAttribute("href"),
      trigger.getAttribute("data-lightbox-alt"),
      document.documentElement.getAttribute("data-lightbox-close") || "Close"
    );
  });

  document.addEventListener("keydown", function (event) {
    if (event.key === "Escape") {
      close();
    }
  });
})();
