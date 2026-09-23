---
title: "Funktion vorschlagen | Pillsner"
description: "Schlag vor, was Pillsner als Nächstes können soll. Beschreibe deine Idee hier, und Pillsner öffnet einen vorausgefüllten Funktionswunsch auf GitHub, den du mit deinem eigenen Konto einreichst."
slug: "request-a-feature"
type: "issue-report"
params:
  hero:
    eyebrow: "Funktion vorschlagen"
    heading: "Was soll Pillsner als Nächstes können?"
    lead: "Pillsner ist bewusst klein, aber nicht fertig. Beschreibe, was dir fehlt, und Pillsner bringt dich zu einem vorausgefüllten Funktionswunsch auf GitHub, wo du ihn mit deinem eigenen Konto einreichst."
  issueTemplate: "feature_request.yml"
  issueLabel: "feature"
  submitLabel: "Auf GitHub fortfahren"
  fields:
    - id: "title"
      label: "Zusammenfassung"
      help: "Eine Zeile, die die Idee beschreibt."
      type: "text"
      required: true
      maxlength: 120
    - id: "problem"
      label: "Was versuchst du zu tun?"
      help: "Die Situation, in der du bist, und was sie heute umständlich macht."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "idea"
      label: "Was würde helfen?"
      help: "Wie du dir vorstellst, dass Pillsner es löst. Grobe Ideen sind willkommen."
      type: "textarea"
      rows: 4
      maxlength: 2000
  notes:
    - "Diese Website hat keinen Server und erhebt nichts. Deine Antworten bleiben in deinem Browser, bis du fortfährst, und gehen immer nur mit dir zu GitHub."
    - "Du brauchst ein GitHub-Konto, um den Wunsch tatsächlich einzureichen. Eines zu erstellen ist kostenlos."
    - "Pillsner bleibt eine Medikamentenerinnerung, die deine Daten auf deinem Gerät behält. Ideen, die zu diesem Versprechen passen, haben die besten Chancen."
  cta:
    heading: "Gute Ideen sind willkommen"
    body: "Pillsner ist quelloffen: Du kannst auch den Code lesen und einen Pull Request öffnen."
---
