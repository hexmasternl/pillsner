---
title: "Fehler melden | Pillsner"
description: "Etwas funktioniert in Pillsner nicht? Beschreibe es hier, und Pillsner öffnet einen vorausgefüllten Fehlerbericht auf GitHub, den du mit deinem eigenen Konto einreichst. Diese Website erhebt nichts."
slug: "report-a-bug"
type: "issue-report"
params:
  hero:
    eyebrow: "Fehler melden"
    heading: "Etwas funktioniert nicht?"
    lead: "Sag uns, was schiefgelaufen ist, und wir schauen es uns an. Fülle das hier aus, und Pillsner bringt dich zu einem vorausgefüllten Fehlerbericht auf GitHub, wo du ihn mit deinem eigenen Konto einreichst."
  issueTemplate: "bug_report.yml"
  issueLabel: "bug"
  submitLabel: "Auf GitHub fortfahren"
  fields:
    - id: "title"
      label: "Zusammenfassung"
      help: "Eine Zeile, die beschreibt, was schiefgelaufen ist."
      type: "text"
      required: true
      maxlength: 120
    - id: "what-happened"
      label: "Was ist passiert?"
      help: "Was du erwartet hast und was die App stattdessen getan hat."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "steps"
      label: "Schritte zum Nachstellen"
      help: "Was du getan hast, Schritt für Schritt, damit wir es selbst sehen können."
      type: "textarea"
      rows: 4
      maxlength: 2000
    - id: "app-version"
      label: "App-Version"
      help: "Unten in den Einstellungen unter Über angezeigt."
      type: "text"
      maxlength: 60
    - id: "device"
      label: "Gerät und Android-Version"
      help: "Zum Beispiel: Pixel 7, Android 15."
      type: "text"
      maxlength: 120
  notes:
    - "Diese Website hat keinen Server und erhebt nichts. Deine Antworten bleiben in deinem Browser, bis du fortfährst, und gehen immer nur mit dir zu GitHub."
    - "Du brauchst ein GitHub-Konto, um den Bericht tatsächlich einzureichen. Eines zu erstellen ist kostenlos."
  cta:
    heading: "Danke, dass du dir die Zeit nimmst"
    body: "Jeder Bericht macht die nächste Erinnerung ein bisschen zuverlässiger."
---
