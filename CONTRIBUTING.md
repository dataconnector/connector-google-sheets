# Contributing Guidelines

Thank you for considering contributing to the Google Sheets Connector project! This document provides guidelines and conventions for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Process](#development-process)
- [Commit Message Convention](#commit-message-convention)
- [Versioning and Tagging](#versioning-and-tagging)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project adheres to a code of conduct that all contributors are expected to follow. Please be respectful and constructive in all interactions.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/connector-google-sheets.git`
3. Create a new branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Commit your changes following our [commit message convention](#commit-message-convention)
6. Push to your fork: `git push origin feature/your-feature-name`
7. Open a Pull Request

## Development Process

- Always work on a feature branch, never directly on `main`
- Ensure your code compiles and tests pass before submitting
- Update documentation as needed for your changes
- Follow the existing code style and conventions

## Commit Message Convention

This project follows the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#specification) specification. This ensures a clear and consistent commit history, which helps with automated versioning and changelog generation.

### Commit Message Format

```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

The `<type>` field is **required** and must be one of the following:

- **`feat`**: A new feature
- **`fix`**: A bug fix
- **`docs`**: Documentation only changes
- **`style`**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc.)
- **`refactor`**: A code change that neither fixes a bug nor adds a feature
- **`perf`**: A code change that improves performance
- **`test`**: Adding missing tests or correcting existing tests
- **`build`**: Changes that affect the build system or external dependencies (example scopes: maven, gradle)
- **`ci`**: Changes to CI configuration files and scripts
- **`chore`**: Other changes that don't modify src or test files
- **`revert`**: Reverts a previous commit

### Scope

The `<scope>` field is **optional** and should be the area of the codebase affected by the change. Examples:

- `auth`: Authentication-related changes
- `api`: API-related changes
- `connector`: Core connector functionality
- `config`: Configuration-related changes
- `deps`: Dependency updates

### Description

The `<description>` field is **required** and should:

- Use the imperative, present tense: "change" not "changed" nor "changes"
- Not capitalize the first letter
- Not end with a period (.)
- Be concise but descriptive

### Body

The `<body>` field is **optional** and should:

- Provide additional context about the change
- Explain the "what" and "why" vs. the "how"
- Be separated from the description by a blank line

### Footer

The `<footer>` field is **optional** and can contain:

- **Breaking changes**: Indicated by `BREAKING CHANGE:` or `!` in the type/scope
- **Issue references**: `Closes #123`, `Fixes #456`, `Refs #789`

### Breaking Changes

Breaking changes must be indicated in one of two ways:

1. Add `!` after the type/scope: `feat(api)!: remove deprecated method`
2. Add `BREAKING CHANGE:` in the footer:

   ```text
   feat(api): remove deprecated method
   
   BREAKING CHANGE: The `oldMethod()` has been removed. Use `newMethod()` instead.
   ```

### Examples

#### Feature Addition

```text
feat(connector): add support for batch data retrieval

Implement batch processing to improve performance when fetching large datasets from Google Sheets.
```

#### Bug Fix

```text
fix(auth): resolve token expiration handling

The connector now properly refreshes OAuth tokens before they expire, preventing authentication failures.
```

#### Documentation Update

```text
docs: update installation instructions

Add Maven Central repository configuration to README.
```

#### Breaking Change

```text
refactor(api)!: rename getData() to fetchData()

BREAKING CHANGE: The method `getData()` has been renamed to `fetchData()` for consistency. Update your code accordingly.
```

#### Multiple Issues

```text
fix(connector): handle empty sheet responses

Fixes #123
Closes #456
```

## Versioning and Tagging

This project follows [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) (SemVer).

### Version Format

Versions must take the form `X.Y.Z` where:

- **X** (MAJOR): Incremented when you make incompatible API changes
- **Y** (MINOR): Incremented when you add functionality in a backward compatible manner
- **Z** (PATCH): Incremented when you make backward compatible bug fixes

### Version Increment Rules

1. **MAJOR version (X.y.z)**: Increment when:
   - Breaking changes are introduced to the public API
   - Changes that are not backward compatible
   - Example: `1.0.0` → `2.0.0`

2. **MINOR version (x.Y.z)**: Increment when:
   - New functionality is added in a backward compatible manner
   - Public API functionality is marked as deprecated
   - Substantial new functionality or improvements are introduced
   - Example: `1.0.0` → `1.1.0`

3. **PATCH version (x.y.Z)**: Increment when:
   - Backward compatible bug fixes are introduced
   - Example: `1.0.0` → `1.0.1`

### Pre-release Versions

Pre-release versions may be denoted by appending a hyphen and a series of dot-separated identifiers:

- `1.0.0-alpha`
- `1.0.0-alpha.1`
- `1.0.0-beta`
- `1.0.0-rc.1`

### Build Metadata

Build metadata may be appended with a plus sign:

- `1.0.0+20130313144700`
- `1.0.0-beta+exp.sha.5114f85`

### Tagging Conventions

Tags should follow the version format exactly:

```bash
# Create a tag for a release
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push tags to remote
git push origin v1.0.0
```

**Important Notes:**

- Tags should be prefixed with `v` (e.g., `v1.0.0`)
- Tag messages should be descriptive
- Never modify or delete tags that have been pushed to the remote repository
- Tags should be created from the `main` branch after a release is finalized

### Determining Version Bumps from Commits

- **MAJOR**: Commits with `BREAKING CHANGE:` footer or `!` in type/scope
- **MINOR**: Commits with type `feat`
- **PATCH**: Commits with type `fix`

## Pull Request Process

1. **Update Documentation**: Ensure all documentation is updated to reflect your changes
2. **Update CHANGELOG.md**: Add an entry describing your changes (see CHANGELOG.md format)
3. **Write Clear PR Description**: Explain what changes were made and why
4. **Link Issues**: Reference any related issues in the PR description
5. **Wait for Review**: All PRs require at least one approval before merging
6. **Squash Commits**: Maintain a clean commit history; squash commits if requested

### PR Title Convention

PR titles should follow the same convention as commit messages:

```text
feat(connector): add support for custom date formats
```

## Questions?

If you have questions about contributing, please:

- Open an issue for discussion
- Check existing issues and pull requests
- Review the project documentation

Thank you for contributing to the Google Sheets Connector project!
