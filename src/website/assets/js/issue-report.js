// Builds a prefilled GitHub "new issue" URL from the report-a-bug and
// request-a-feature forms and sends the visitor there to finish filing it with
// their own GitHub account. Nothing is sent anywhere else: there is no backend,
// no token and no fetch call, and no input is stored by the site.
//
// Without JavaScript the form still works: it is a plain GET form whose action
// is GitHub's "new issue" page, and whose hidden fields carry the issue-form
// template and label. This script only exists to keep over-long values from
// producing an unusable URL.
(function () {
  "use strict";

  // GitHub silently truncates very long query strings, so cap what we send.
  var MAX_FIELD_LENGTH = 2000;
  var MAX_URL_LENGTH = 6000;

  function truncate(value) {
    if (value.length <= MAX_FIELD_LENGTH) {
      return value;
    }
    return value.slice(0, MAX_FIELD_LENGTH - 1) + "\u2026";
  }

  function buildUrl(form) {
    var base = form.getAttribute("data-issue-url") || form.getAttribute("action");
    var params = [];

    Array.prototype.forEach.call(form.elements, function (element) {
      if (!element.name || element.disabled || element.type === "submit") {
        return;
      }
      var value = (element.value || "").trim();
      if (!value) {
        return;
      }
      params.push(
        encodeURIComponent(element.name) + "=" + encodeURIComponent(truncate(value))
      );
    });

    var url = base + (params.length ? "?" + params.join("&") : "");
    if (url.length > MAX_URL_LENGTH) {
      url = url.slice(0, MAX_URL_LENGTH);
    }
    return url;
  }

  var forms = document.querySelectorAll("form[data-issue-form]");
  Array.prototype.forEach.call(forms, function (form) {
    form.addEventListener("submit", function (event) {
      if (typeof form.reportValidity === "function" && !form.reportValidity()) {
        return;
      }
      event.preventDefault();
      window.location.href = buildUrl(form);
    });
  });
})();
