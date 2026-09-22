---
title: "Comunicar um erro | Pillsner"
description: "Algo não funciona no Pillsner? Descreva-o aqui e o Pillsner abre um relatório de erro pré-preenchido no GitHub para que o submeta com a sua própria conta. Nada é recolhido por este site."
slug: "report-a-bug"
type: "issue-report"
params:
  hero:
    eyebrow: "Comunicar um erro"
    heading: "Algo não funciona?"
    lead: "Conte-nos o que correu mal e nós vamos investigar. Preencha isto e o Pillsner leva-o a um relatório de erro pré-preenchido no GitHub, onde o submete com a sua própria conta."
  issueTemplate: "bug_report.yml"
  issueLabel: "bug"
  submitLabel: "Continuar no GitHub"
  fields:
    - id: "title"
      label: "Resumo"
      help: "Uma linha a descrever o que correu mal."
      type: "text"
      required: true
      maxlength: 120
    - id: "what-happened"
      label: "O que aconteceu?"
      help: "O que esperava e o que a aplicação fez em vez disso."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "steps"
      label: "Passos para reproduzir"
      help: "O que fez, passo a passo, para que o possamos ver por nós próprios."
      type: "textarea"
      rows: 4
      maxlength: 2000
    - id: "app-version"
      label: "Versão da aplicação"
      help: "Mostrada no fundo das Definições, em Acerca de."
      type: "text"
      maxlength: 60
    - id: "device"
      label: "Dispositivo e versão do Android"
      help: "Por exemplo: Pixel 7, Android 15."
      type: "text"
      maxlength: 120
  notes:
    - "Este site não tem servidor e não recolhe nada. As suas respostas ficam no seu navegador até decidir continuar, e só alguma vez vão para o GitHub, consigo."
    - "Precisa de uma conta GitHub para efetivamente registar o relatório. Criar uma é gratuito."
  cta:
    heading: "Obrigado pelo seu tempo"
    body: "Cada relatório torna o próximo lembrete um pouco mais fiável."
---
