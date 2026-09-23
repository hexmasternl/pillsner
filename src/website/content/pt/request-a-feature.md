---
title: "Sugerir uma funcionalidade | Pillsner"
description: "Sugira o que o Pillsner deve fazer a seguir. Descreva a sua ideia aqui e o Pillsner abre um pedido de funcionalidade pré-preenchido no GitHub para que o submeta com a sua própria conta."
slug: "request-a-feature"
type: "issue-report"
params:
  hero:
    eyebrow: "Sugerir uma funcionalidade"
    heading: "O que deve o Pillsner fazer a seguir?"
    lead: "O Pillsner é deliberadamente pequeno, mas não está terminado. Descreva o que lhe falta e o Pillsner leva-o a um pedido de funcionalidade pré-preenchido no GitHub, onde o submete com a sua própria conta."
  issueTemplate: "feature_request.yml"
  issueLabel: "feature"
  submitLabel: "Continuar no GitHub"
  fields:
    - id: "title"
      label: "Resumo"
      help: "Uma linha a descrever a ideia."
      type: "text"
      required: true
      maxlength: 120
    - id: "problem"
      label: "O que está a tentar fazer?"
      help: "A situação em que se encontra, e o que a torna incómoda hoje."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "idea"
      label: "O que ajudaria?"
      help: "Como imagina o Pillsner a resolvê-lo. Ideias em rascunho são bem-vindas."
      type: "textarea"
      rows: 4
      maxlength: 2000
  notes:
    - "Este site não tem servidor e não recolhe nada. As suas respostas ficam no seu navegador até decidir continuar, e só alguma vez vão para o GitHub, consigo."
    - "Precisa de uma conta GitHub para efetivamente registar o pedido. Criar uma é gratuito."
    - "O Pillsner continua a ser um lembrete de medicação que mantém os seus dados no seu dispositivo. As ideias que respeitam essa promessa têm a melhor hipótese."
  cta:
    heading: "As boas ideias são bem-vindas"
    body: "O Pillsner é de código aberto: também pode ler o código e abrir um pull request."
---
