---
title: "Signaler un bug | Pillsner"
description: "Quelque chose ne fonctionne pas dans Pillsner ? Décrivez-le ici et Pillsner ouvre pour vous un rapport de bug prérempli sur GitHub, à soumettre avec votre propre compte. Ce site web ne collecte rien."
slug: "report-a-bug"
type: "issue-report"
params:
  hero:
    eyebrow: "Signaler un bug"
    heading: "Quelque chose ne fonctionne pas ?"
    lead: "Dites-nous ce qui n'a pas fonctionné et nous nous pencherons dessus. Remplissez ceci et Pillsner vous emmène vers un rapport de bug prérempli sur GitHub, où vous le soumettez avec votre propre compte."
  issueTemplate: "bug_report.yml"
  issueLabel: "bug"
  submitLabel: "Continuer sur GitHub"
  fields:
    - id: "title"
      label: "Résumé"
      help: "Une ligne décrivant ce qui n'a pas fonctionné."
      type: "text"
      required: true
      maxlength: 120
    - id: "what-happened"
      label: "Que s'est-il passé ?"
      help: "Ce que vous attendiez, et ce que l'application a fait à la place."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "steps"
      label: "Étapes pour reproduire"
      help: "Ce que vous avez fait, étape par étape, pour que nous puissions le constater nous-mêmes."
      type: "textarea"
      rows: 4
      maxlength: 2000
    - id: "app-version"
      label: "Version de l'application"
      help: "Affichée en bas des Réglages, sous À propos."
      type: "text"
      maxlength: 60
    - id: "device"
      label: "Appareil et version d'Android"
      help: "Par exemple : Pixel 7, Android 15."
      type: "text"
      maxlength: 120
  notes:
    - "Ce site web n'a pas de serveur et ne collecte rien. Vos réponses restent dans votre navigateur jusqu'à ce que vous choisissiez de continuer, et elles ne vont jamais qu'à GitHub, avec vous."
    - "Vous avez besoin d'un compte GitHub pour déposer réellement le rapport. En créer un est gratuit."
  cta:
    heading: "Merci d'avoir pris le temps"
    body: "Chaque rapport rend le prochain rappel un peu plus fiable."
---
