---
title: "Informar de un fallo | Pillsner"
description: "¿Algo no funciona en Pillsner? Descríbelo aquí y Pillsner abre un informe de fallo rellenado en GitHub para que lo envíes con tu propia cuenta. Este sitio web no recopila nada."
slug: "report-a-bug"
type: "issue-report"
params:
  hero:
    eyebrow: "Informar de un fallo"
    heading: "¿Algo no funciona?"
    lead: "Cuéntanos qué salió mal y lo investigaremos. Rellena esto y Pillsner te lleva a un informe de fallo rellenado en GitHub, donde lo envías con tu propia cuenta."
  issueTemplate: "bug_report.yml"
  issueLabel: "bug"
  submitLabel: "Continuar en GitHub"
  fields:
    - id: "title"
      label: "Resumen"
      help: "Una línea que describa qué salió mal."
      type: "text"
      required: true
      maxlength: 120
    - id: "what-happened"
      label: "¿Qué pasó?"
      help: "Lo que esperabas y lo que hizo la app en su lugar."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "steps"
      label: "Pasos para reproducirlo"
      help: "Lo que hiciste, paso a paso, para que podamos verlo nosotros mismos."
      type: "textarea"
      rows: 4
      maxlength: 2000
    - id: "app-version"
      label: "Versión de la app"
      help: "Aparece al final de los Ajustes, en Acerca de."
      type: "text"
      maxlength: 60
    - id: "device"
      label: "Dispositivo y versión de Android"
      help: "Por ejemplo: Pixel 7, Android 15."
      type: "text"
      maxlength: 120
  notes:
    - "Este sitio web no tiene servidor y no recopila nada. Tus respuestas se quedan en tu navegador hasta que decides continuar, y solo van a GitHub, contigo."
    - "Necesitas una cuenta de GitHub para presentar el informe. Crear una es gratis."
  cta:
    heading: "Gracias por dedicarle un momento"
    body: "Cada informe hace que el siguiente recordatorio sea un poco más fiable."
---
