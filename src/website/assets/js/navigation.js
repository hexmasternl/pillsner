// Header navigation behaviour — section 8.14 of docs/design-system.md.
// Progressive enhancement only: the menu groups are native <details>, and
// without this script the narrow-screen menu is rendered expanded (see
// layout.css), so every page stays reachable. This adds the Menu button
// toggle, closing on Escape, outside click and link selection, and keeps one
// dropdown open at a time on wide screens. It stores nothing and sends nothing.
(function () {
  "use strict";

  var WIDE_QUERY = "(min-width: 960px)";

  var header = document.querySelector(".site-header");
  var button = header && header.querySelector(".site-header__menu-button");
  var nav = header && header.querySelector(".site-nav");
  if (!header || !button || !nav) {
    return;
  }

  var groups = Array.prototype.slice.call(nav.querySelectorAll(".site-nav__group"));
  var wide = window.matchMedia(WIDE_QUERY);

  function isMenuOpen() {
    return header.classList.contains("is-menu-open");
  }

  function setMenuOpen(open) {
    header.classList.toggle("is-menu-open", open);
    button.setAttribute("aria-expanded", open ? "true" : "false");
    button.setAttribute(
      "aria-label",
      button.getAttribute(open ? "data-label-close" : "data-label-open")
    );
  }

  function closeMenu(returnFocus) {
    if (!isMenuOpen()) {
      return;
    }
    setMenuOpen(false);
    if (returnFocus) {
      button.focus();
    }
  }

  // Narrow screens list every page at once, so every group starts open;
  // wide screens start with every dropdown closed.
  function applyLayout() {
    groups.forEach(function (group) {
      group.open = !wide.matches;
    });
    if (wide.matches) {
      setMenuOpen(false);
    }
  }

  function closeGroups(except) {
    groups.forEach(function (group) {
      if (group !== except) {
        group.open = false;
      }
    });
  }

  button.addEventListener("click", function () {
    setMenuOpen(!isMenuOpen());
  });

  groups.forEach(function (group) {
    group.addEventListener("toggle", function () {
      if (wide.matches && group.open) {
        closeGroups(group);
      }
    });

    group.addEventListener("focusout", function (event) {
      if (wide.matches && !group.contains(event.relatedTarget)) {
        group.open = false;
      }
    });
  });

  nav.addEventListener("click", function (event) {
    if (event.target.closest && event.target.closest("a[href]")) {
      closeMenu(false);
    }
  });

  document.addEventListener("click", function (event) {
    if (!wide.matches) {
      return;
    }
    groups.forEach(function (group) {
      if (group.open && !group.contains(event.target)) {
        group.open = false;
      }
    });
  });

  document.addEventListener("keydown", function (event) {
    if (event.key !== "Escape") {
      return;
    }
    if (wide.matches) {
      groups.forEach(function (group) {
        if (group.open) {
          group.open = false;
          if (group.contains(document.activeElement)) {
            group.querySelector("summary").focus();
          }
        }
      });
    } else {
      closeMenu(true);
    }
  });

  if (wide.addEventListener) {
    wide.addEventListener("change", applyLayout);
  } else if (wide.addListener) {
    wide.addListener(applyLayout);
  }

  applyLayout();
})();
