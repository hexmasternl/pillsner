// Remembers a visitor's manually chosen language so a later visit to the
// root URL (see static/index.html) honours it instead of re-detecting from
// the browser. This is the only client-side storage the site uses, and it
// is never sent anywhere — see docs/design-system.md's privacy stance and
// the design decision recorded in openspec/changes/website-single-page-hugo.
(function () {
  "use strict";

  var STORAGE_KEY = "pillsner-lang";

  function remember(lang) {
    try {
      window.localStorage.setItem(STORAGE_KEY, lang);
    } catch (e) {
      // Private browsing or blocked storage: the switcher link still works,
      // it just won't be remembered next time. Nothing to do here.
    }
  }

  document.addEventListener("click", function (event) {
    var link = event.target.closest && event.target.closest(".language-switcher__menu a[data-lang]");
    if (link) {
      remember(link.getAttribute("data-lang"));
    }
  });
})();
