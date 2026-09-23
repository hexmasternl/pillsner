---
title: "Een fout melden | Pillsner"
description: "Werkt er iets niet in Pillsner? Beschrijf het hier en Pillsner opent een vooraf ingevuld bugrapport op GitHub dat je met je eigen account indient. Deze website verzamelt niets."
slug: "report-a-bug"
type: "issue-report"
params:
  hero:
    eyebrow: "Een fout melden"
    heading: "Werkt er iets niet?"
    lead: "Laat ons weten wat er misging en we zoeken het uit. Vul dit in en Pillsner brengt je naar een vooraf ingevuld bugrapport op GitHub, waar je het met je eigen account indient."
  issueTemplate: "bug_report.yml"
  issueLabel: "bug"
  submitLabel: "Doorgaan op GitHub"
  fields:
    - id: "title"
      label: "Samenvatting"
      help: "Eén regel die beschrijft wat er misging."
      type: "text"
      required: true
      maxlength: 120
    - id: "what-happened"
      label: "Wat gebeurde er?"
      help: "Wat je verwachtte, en wat de app in plaats daarvan deed."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "steps"
      label: "Stappen om het te reproduceren"
      help: "Wat je deed, stap voor stap, zodat we het zelf kunnen zien."
      type: "textarea"
      rows: 4
      maxlength: 2000
    - id: "app-version"
      label: "App-versie"
      help: "Onderaan Instellingen te vinden, onder Over."
      type: "text"
      maxlength: 60
    - id: "device"
      label: "Toestel en Android-versie"
      help: "Bijvoorbeeld: Pixel 7, Android 15."
      type: "text"
      maxlength: 120
  notes:
    - "Deze website heeft geen server en verzamelt niets. Je antwoorden blijven in je browser tot je ervoor kiest om door te gaan, en ze gaan alleen ooit naar GitHub, met jou."
    - "Je hebt een GitHub-account nodig om het rapport echt in te dienen. Er een aanmaken is gratis."
  cta:
    heading: "Bedankt dat je de tijd neemt"
    body: "Elk rapport maakt de volgende herinnering een beetje betrouwbaarder."
---
