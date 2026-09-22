---
title: "Request a feature | Pillsner"
description: "Suggest what Pillsner should do next. Describe your idea here and Pillsner opens a prefilled feature request on GitHub for you to submit with your own account."
slug: "request-a-feature"
type: "issue-report"
params:
  hero:
    eyebrow: "Request a feature"
    heading: "What should Pillsner do next?"
    lead: "Pillsner is deliberately small, but it is not finished. Describe what you are missing and Pillsner takes you to a prefilled feature request on GitHub, where you submit it with your own account."
  issueTemplate: "feature_request.yml"
  issueLabel: "feature"
  submitLabel: "Continue on GitHub"
  fields:
    - id: "title"
      label: "Summary"
      help: "One line describing the idea."
      type: "text"
      required: true
      maxlength: 120
    - id: "problem"
      label: "What are you trying to do?"
      help: "The situation you are in, and what makes it awkward today."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "idea"
      label: "What would help?"
      help: "How you imagine Pillsner solving it. Rough ideas are welcome."
      type: "textarea"
      rows: 4
      maxlength: 2000
  notes:
    - "This website has no server and collects nothing. Your answers stay in your browser until you choose to continue, and they only ever go to GitHub, with you."
    - "You need a GitHub account to actually file the request. Creating one is free."
    - "Pillsner stays a medication reminder that keeps your data on your device. Ideas that fit that promise have the best chance."
  cta:
    heading: "Good ideas are welcome"
    body: "Pillsner is open source: you can also read the code and open a pull request."
---
