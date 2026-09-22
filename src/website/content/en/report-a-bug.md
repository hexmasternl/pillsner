---
title: "Report a bug | Pillsner"
description: "Something not working in Pillsner? Describe it here and Pillsner opens a prefilled bug report on GitHub for you to submit with your own account. Nothing is collected by this website."
slug: "report-a-bug"
type: "issue-report"
params:
  hero:
    eyebrow: "Report a bug"
    heading: "Something not working?"
    lead: "Tell us what went wrong and we will look into it. Fill this in and Pillsner takes you to a prefilled bug report on GitHub, where you submit it with your own account."
  issueTemplate: "bug_report.yml"
  issueLabel: "bug"
  submitLabel: "Continue on GitHub"
  fields:
    - id: "title"
      label: "Summary"
      help: "One line describing what went wrong."
      type: "text"
      required: true
      maxlength: 120
    - id: "what-happened"
      label: "What happened?"
      help: "What you expected, and what the app did instead."
      type: "textarea"
      required: true
      rows: 5
      maxlength: 2000
    - id: "steps"
      label: "Steps to reproduce"
      help: "What you did, step by step, so we can see it for ourselves."
      type: "textarea"
      rows: 4
      maxlength: 2000
    - id: "app-version"
      label: "App version"
      help: "Shown at the bottom of Settings, under About."
      type: "text"
      maxlength: 60
    - id: "device"
      label: "Device and Android version"
      help: "For example: Pixel 7, Android 15."
      type: "text"
      maxlength: 120
  notes:
    - "This website has no server and collects nothing. Your answers stay in your browser until you choose to continue, and they only ever go to GitHub, with you."
    - "You need a GitHub account to actually file the report. Creating one is free."
  cta:
    heading: "Thanks for taking the time"
    body: "Every report makes the next reminder a little more reliable."
---
