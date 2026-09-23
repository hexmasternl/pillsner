---
title: "Solicitar una función | Pillsner"
description: "Sugiere qué debería hacer Pillsner a continuación. Describe tu idea aquí y Pillsner abre una solicitud de función rellenada en GitHub para que la envíes con tu propia cuenta."
slug: "request-a-feature"
type: "issue-report"
params:
  hero:
    eyebrow: "Solicitar una función"
    heading: "¿Qué debería hacer Pillsner a continuación?"
    lead: "Pillsner es deliberadamente pequeño, pero no está terminado. Describe lo que echas en falta y Pillsner te lleva a una solicitud de función rellenada en GitHub, donde la envías con tu propia cuenta."
  issueTemplate: "feature_request.yml"
  issueLabel: "feature"
  submitLabel: "Continuar en GitHub"
  fields:
    - id: "title"
      label: "Resumen"
      help: "Una línea que describa la idea."
      type: "text"
      required: true
      maxlength: 120
    - id: "problem"
      label: "¿Qué intentas hacer?"
      help: "La situación en la que estás y qué la hace incómoda hoy."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "idea"
      label: "¿Qué ayudaría?"
      help: "Cómo imaginas que Pillsner lo resolvería. Las ideas a grandes rasgos son bienvenidas."
      type: "textarea"
      rows: 4
      maxlength: 2000
  notes:
    - "Este sitio web no tiene servidor y no recopila nada. Tus respuestas se quedan en tu navegador hasta que decides continuar, y solo van a GitHub, contigo."
    - "Necesitas una cuenta de GitHub para presentar la solicitud. Crear una es gratis."
    - "Pillsner sigue siendo un recordatorio de medicación que mantiene tus datos en tu dispositivo. Las ideas que encajan con esa promesa tienen más posibilidades."
  cta:
    heading: "Las buenas ideas son bienvenidas"
    body: "Pillsner es de código abierto: también puedes leer el código y abrir una pull request."
---
