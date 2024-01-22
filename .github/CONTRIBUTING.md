# Contributing to diffson

Thank you for your interest in diffson.
This guide gives you guidelines on how you can contribute to the project.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents

- [Getting help](#getting-help)
- [File a new issue](#file-a-new-issue)
  - [Question](#question)
  - [Bug report](#bug-report)
  - [Feature request](#feature-request)
- [Submitting new features or bug fixes](#submitting-new-features-or-bug-fixes)
- [Submitting a Json library update](#submitting-a-json-library-update)
- [Licensing](#licensing)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Getting help

There are several ways of getting help.
You can contact us [on the chat](https://gitter.im/gnieh/diffson) or file an issue to ask your question (see below).

## File a new issue

Whether you found a bug, have a question, or want to request a new feature, please feel free to file a new issue.

### Question

Please provide a short descriptive title, and ask the full question in the issue description.
Make sure that you give the context of your question in the description to make it easier to answer it (e.g. the used diffson version and Json library).

### Bug report

Please provide a short descriptive title, and give full information in the issues description.
To help fixing the bug give the following information:
 - version of diffson ;
 - Json library ;
 - performed operation ;
 - expected result ;
 - actual result ;
 - if any, the exception with stacktrace ;
 - when possible, a short code example to reproduce the bug.

### Feature request

Please provide a short descriptive title, and give details in the description.
Ideally, details should include following information:
 - a description of the feature ;
 - use cases for this feature ;
 - whether it changes current behavior or just adds a new one.

## Submitting new features or bug fixes

Diffson uses [semantic versioning](https://semver.org/).
When submitting code change, please target the correct branch depending on the nature of the change.
If the pull request solves an issue, please refer to the issue in the description.
The _PR_ description must contain an explanatory text about how the problem is solved.

## Submitting a Json library update

Underlying Json libraries may be upgraded while diffson has no new changes.
The target version for such upgrades depends on the nature of changes in the Json library:
 - in case of total source code compatibility, target the next patch version of diffson ;
 - in the case some code changes are to be made in the internal of diffson, without impacting the API, target the next patch version of diffson ;
 - in the case the external API is impacted by the Json library version upgrade, target the next major version of diffson.

## Licensing

Diffson is licensed under the Apache Software License 2.0. Opening a pull request is to be considered affirmative consent to incorporate your changes into the project, granting an unrestricted license to the diffson project maintainers to distribute and derive new work from your changes, as per the contribution terms of ASL 2.0. You also affirm that you own the rights to the code you are contributing. All contributors retain the copyright to their own work.
