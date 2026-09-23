---
title: "Proposer une fonctionnalité | Pillsner"
description: "Suggérez ce que Pillsner devrait faire ensuite. Décrivez votre idée ici et Pillsner ouvre pour vous une demande de fonctionnalité préremplie sur GitHub, à soumettre avec votre propre compte."
slug: "request-a-feature"
type: "issue-report"
params:
  hero:
    eyebrow: "Proposer une fonctionnalité"
    heading: "Que devrait faire Pillsner ensuite ?"
    lead: "Pillsner est délibérément petite, mais elle n'est pas finie. Décrivez ce qui vous manque et Pillsner vous emmène vers une demande de fonctionnalité préremplie sur GitHub, où vous la soumettez avec votre propre compte."
  issueTemplate: "feature_request.yml"
  issueLabel: "feature"
  submitLabel: "Continuer sur GitHub"
  fields:
    - id: "title"
      label: "Résumé"
      help: "Une ligne décrivant l'idée."
      type: "text"
      required: true
      maxlength: 120
    - id: "problem"
      label: "Qu'essayez-vous de faire ?"
      help: "La situation dans laquelle vous êtes, et ce qui la rend malcommode aujourd'hui."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "idea"
      label: "Qu'est-ce qui aiderait ?"
      help: "Comment vous imaginez Pillsner la résoudre. Les idées à l'état brut sont les bienvenues."
      type: "textarea"
      rows: 4
      maxlength: 2000
  notes:
    - "Ce site web n'a pas de serveur et ne collecte rien. Vos réponses restent dans votre navigateur jusqu'à ce que vous choisissiez de continuer, et elles ne vont jamais qu'à GitHub, avec vous."
    - "Vous avez besoin d'un compte GitHub pour déposer réellement la demande. En créer un est gratuit."
    - "Pillsner reste un rappel de médicaments qui garde vos données sur votre appareil. Les idées qui respectent cette promesse ont les meilleures chances."
  cta:
    heading: "Les bonnes idées sont les bienvenues"
    body: "Pillsner est open source : vous pouvez aussi lire le code et ouvrir une pull request."
---
