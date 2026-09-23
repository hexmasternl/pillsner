---
title: "Een functie aanvragen | Pillsner"
description: "Stel voor wat Pillsner hierna zou moeten doen. Beschrijf je idee hier en Pillsner opent een vooraf ingevuld functieverzoek op GitHub dat je met je eigen account indient."
slug: "request-a-feature"
type: "issue-report"
params:
  hero:
    eyebrow: "Een functie aanvragen"
    heading: "Wat moet Pillsner hierna doen?"
    lead: "Pillsner is bewust klein, maar niet af. Beschrijf wat je mist en Pillsner brengt je naar een vooraf ingevuld functieverzoek op GitHub, waar je het met je eigen account indient."
  issueTemplate: "feature_request.yml"
  issueLabel: "feature"
  submitLabel: "Doorgaan op GitHub"
  fields:
    - id: "title"
      label: "Samenvatting"
      help: "Eén regel die het idee beschrijft."
      type: "text"
      required: true
      maxlength: 120
    - id: "problem"
      label: "Wat probeer je te doen?"
      help: "De situatie waarin je zit, en wat het vandaag lastig maakt."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "idea"
      label: "Wat zou helpen?"
      help: "Hoe je je voorstelt dat Pillsner het oplost. Ruwe ideeën zijn welkom."
      type: "textarea"
      rows: 4
      maxlength: 2000
  notes:
    - "Deze website heeft geen server en verzamelt niets. Je antwoorden blijven in je browser tot je ervoor kiest om door te gaan, en ze gaan alleen ooit naar GitHub, met jou."
    - "Je hebt een GitHub-account nodig om het verzoek echt in te dienen. Er een aanmaken is gratis."
    - "Pillsner blijft een medicatieherinnering die je gegevens op je toestel houdt. Ideeën die bij die belofte passen maken de beste kans."
  cta:
    heading: "Goede ideeën zijn welkom"
    body: "Pillsner is opensource: je kunt ook de code lezen en een pull request openen."
---
