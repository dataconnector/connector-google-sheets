# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Types of Changes

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes

## [Unreleased]

### Added

- Production-ready `GoogleSheetsConnector` supporting read/write and streaming operations
- OAuth2 access/refresh token flow in addition to service account based authentication
- Configuration validation, batching controls, and header-aware row mapping
- Google API client, Sheets SDK, auth libraries, Lombok, and SLF4J dependencies
- Developer documentation (`README.md`, `CONTRIBUTING.md`) and release process notes
- Optional `columns` configuration for selective column reads/writes across batch and streaming flows

---

## Version History

---

## Release Process

When creating a new release:

1. **Update Version**: Update the version in `pom.xml`
2. **Update CHANGELOG**: Add a new section for the release with the date
3. **Create Tag**: Create a git tag following SemVer: `vX.Y.Z`
4. **Push Tag**: Push the tag to the remote repository
5. **Create Release**: Create a GitHub release (if applicable) with release notes

### Example Release Workflow

```bash
# 1. Update version in pom.xml (e.g., from 0.0.1 to 0.0.2)
# 2. Update CHANGELOG.md with new version section
# 3. Commit changes
git add pom.xml CHANGELOG.md
git commit -m "chore: release version 0.0.2"

# 4. Create and push tag
git tag -a v0.0.2 -m "Release version 0.0.2"
git push origin main
git push origin v0.0.2
```

## Semantic Versioning Guidelines

This project follows [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html):

- **MAJOR.MINOR.PATCH** format (e.g., `1.2.3`)
- **MAJOR**: Breaking changes to the public API
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

### Version 0.y.z (Initial Development)

- The project is in initial development
- The public API is not stable
- Any version may introduce breaking changes
- Version 1.0.0 defines the stable public API

### Version 1.0.0 and Above

- Version 1.0.0 defines the public API
- After 1.0.0, version increments follow SemVer rules strictly
- Breaking changes require a MAJOR version bump
- New features require a MINOR version bump
- Bug fixes require a PATCH version bump

## Linking to Commits

When documenting changes, you can reference commits using their hash or link to them:

- Commit: `abc1234` - `feat(connector): add batch processing`
- Issue: `#123` - Fix authentication token refresh

## Formatting Guidelines

- Use present tense: "Add feature" not "Added feature"
- Use imperative mood: "Fix bug" not "Fixed bug"
- Group related changes together
- List changes in order of importance
- Include breaking changes prominently
- Reference related issues and pull requests

## Example Entry Format

```markdown
## [1.2.0] - 2025-11-21

### Added
- Support for OAuth2 authentication (`feat(auth): add OAuth2 support`)
- Batch data retrieval API (`feat(connector): add batch processing`)

### Changed
- Improved error messages for authentication failures (`refactor(auth): enhance error handling`)

### Fixed
- Resolved token expiration handling issue (`fix(auth): resolve token expiration`)
- Fixed memory leak in data processing (`fix(connector): fix memory leak`)

### Deprecated
- `BasicAuth` class is deprecated, use `OAuth2Auth` instead (`refactor(auth): deprecate BasicAuth`)

## [1.1.0] - 2024-01-01

### Added
- Initial public release

### Changed
- Updated documentation with usage examples (`docs: update API documentation`)

## [1.0.0] - 2023-12-15

### Added
- Initial stable release
- Core connector functionality
- Google Sheets API integration
- Configuration management

### Changed
- **BREAKING**: Renamed `getData()` to `fetchData()` (`refactor(api)!: rename getData method`)
```

---

**Note**: This changelog is maintained manually. All contributors should update this file when making changes that affect users of the library.
