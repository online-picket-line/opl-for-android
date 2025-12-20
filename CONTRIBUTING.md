# Contributing to Online Picket Line Android

Thank you for your interest in contributing to the Online Picket Line Android app! This document provides guidelines and instructions for contributing.

## Code of Conduct

This project aims to support workers and labor movements. We ask all contributors to:
- Be respectful and professional
- Support the goals of worker solidarity
- Prioritize user privacy and security
- Write clean, maintainable code

## How to Contribute

### Reporting Bugs

Before creating a bug report:
1. Check if the issue already exists in the [Issues](https://github.com/online-picket-line/opl-for-android/issues) page
2. Verify you're using the latest version

When creating a bug report, include:
- **Description**: Clear description of the bug
- **Steps to Reproduce**: Detailed steps to recreate the issue
- **Expected Behavior**: What you expected to happen
- **Actual Behavior**: What actually happened
- **Environment**: Android version, device model, app version
- **Logs**: Relevant logcat output if available

### Suggesting Enhancements

Enhancement suggestions are welcome! Include:
- **Use Case**: Why this feature would be useful
- **Proposed Solution**: How you envision it working
- **Alternatives**: Other approaches you've considered
- **Impact**: How this affects user privacy/security

### Pull Requests

1. **Fork the Repository**
   ```bash
   git clone https://github.com/online-picket-line/opl-for-android.git
   cd opl-for-android
   git checkout -b feature/your-feature-name
   ```

2. **Make Your Changes**
   - Follow the existing code style
   - Add tests for new functionality
   - Update documentation as needed
   - Keep changes focused and atomic

3. **Test Your Changes**
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```

4. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "Brief description of changes"
   ```
   
   Good commit messages:
   - Use present tense ("Add feature" not "Added feature")
   - Be specific and concise
   - Reference issues when applicable (#123)

5. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   ```
   
   Then create a Pull Request on GitHub with:
   - Clear title and description
   - Reference to related issues
   - Screenshots/videos for UI changes
   - Test results

## Development Guidelines

### Code Style

- **Language**: Kotlin is preferred for new code
- **Formatting**: Follow Kotlin coding conventions
- **Naming**: Use descriptive names for variables and functions
- **Comments**: Explain "why" not "what" in comments

Example:
```kotlin
// Good
fun findDisputeForDomain(domain: String): LaborDispute? {
    // Check allowed list first to avoid unnecessary lookups
    if (isAllowedDomain(domain)) {
        return null
    }
    return cachedDisputes.firstOrNull { it.matchesDomain(domain) }
}

// Avoid
fun find(d: String): LaborDispute? {
    // Find dispute
    if (allowed(d)) {
        return null
    }
    return disputes.firstOrNull { it.matches(d) }
}
```

### Architecture

The app follows clean architecture principles:

```
Presentation Layer (UI) → Domain Layer (Use Cases) → Data Layer (Repository, API)
```

- **UI Components**: Activities, Fragments, Adapters
- **ViewModels**: Handle UI logic and state
- **Repository**: Single source of truth for data
- **API Client**: Network communication
- **Models**: Data classes

### Testing

- Write unit tests for business logic
- Test edge cases and error conditions
- Aim for meaningful test coverage, not 100% coverage
- Mock external dependencies

Example test structure:
```kotlin
@Test
fun testMatchesDomain_subdomain() {
    // Arrange
    val dispute = createTestDispute(domain = "example.com")
    
    // Act
    val matches = dispute.matchesDomain("www.example.com")
    
    // Assert
    assertTrue(matches)
}
```

### Security Considerations

When contributing, keep in mind:

1. **User Privacy**
   - Never log sensitive user data
   - Don't send browsing data to external servers
   - All filtering must happen locally

2. **Data Validation**
   - Validate all API responses
   - Sanitize user inputs
   - Handle malformed data gracefully

3. **Permissions**
   - Request only necessary permissions
   - Explain permission usage to users
   - Handle permission denials gracefully

4. **Dependencies**
   - Keep dependencies up to date
   - Audit for known vulnerabilities
   - Use reputable libraries only

## Project Areas for Contribution

### High Priority
- Improve VPN packet parsing accuracy
- Optimize battery usage
- Enhance notification system
- Add support for more dispute types

### Medium Priority
- Implement filtering statistics
- Add data export functionality
- Create widget for quick toggle
- Improve UI/UX

### Low Priority
- Add theme customization
- Support multiple languages
- Create onboarding tutorial

## Getting Help

- **Questions**: Open a [Discussion](https://github.com/online-picket-line/opl-for-android/discussions)
- **Chat**: Join our community chat (if available)
- **Documentation**: Check README.md and BUILDING.md

## Recognition

Contributors will be:
- Listed in the project contributors
- Mentioned in release notes for significant contributions
- Forever appreciated for supporting workers' rights!

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

Thank you for helping build a tool that supports labor movements! ✊
