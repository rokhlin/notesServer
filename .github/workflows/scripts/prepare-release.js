// Release preparation & Changelog extraction for notesServer
const fs = require('fs');
const path = require('path');

function bumpSemver(version, bumpType) {
  const parts = version.replace(/^v/, '').split('.').map(n => parseInt(n, 10) || 0);
  while (parts.length < 3) parts.push(0);
  let [major, minor, patch] = parts;

  if (bumpType === 'major') {
    major += 1;
    minor = 0;
    patch = 0;
  } else if (bumpType === 'minor') {
    minor += 1;
    patch = 0;
  } else {
    patch += 1;
  }
  return `${major}.${minor}.${patch}`;
}

function run() {
  const rootDir = process.cwd();
  const gradlePropsPath = path.join(rootDir, 'gradle.properties');
  const changelogPath = path.join(rootDir, 'CHANGELOG.md');
  const githubOutputPath = process.env.GITHUB_OUTPUT;

  const inputVersion = process.env.INPUT_RELEASE_VERSION?.trim();
  const bumpType = process.env.INPUT_BUMP_TYPE?.trim() || 'patch';

  // 1. Read gradle.properties
  let gradleProps = fs.readFileSync(gradlePropsPath, 'utf8');
  const currentVersionMatch = gradleProps.match(/app\.version=([^\r\n]+)/);

  const currentVersion = currentVersionMatch ? currentVersionMatch[1].trim() : '0.0.1';

  let releaseVersion;
  if (inputVersion && inputVersion !== '') {
    releaseVersion = inputVersion.replace(/^v/, '');
  } else {
    releaseVersion = bumpSemver(currentVersion, bumpType);
  }

  console.log(`Current server version: ${currentVersion}`);
  console.log(`Target release version: ${releaseVersion}`);

  // 2. Parse CHANGELOG.md
  let changelog = fs.readFileSync(changelogPath, 'utf8');
  const today = new Date().toISOString().slice(0, 10);

  // Extract Unreleased notes
  const unreleasedRegex = /##\s*\[Unreleased\]([\s\S]*?)(?=##\s*\[|\Z)/i;
  const match = changelog.match(unreleasedRegex);
  let releaseNotes = '';

  if (match && match[1].trim()) {
    releaseNotes = match[1].trim();
    // Update changelog with new release section
    const newSection = `## [Unreleased]\n\n## [${releaseVersion}] - ${today}\n${releaseNotes}\n`;
    changelog = changelog.replace(unreleasedRegex, newSection);
  } else {
    const versionRegex = new RegExp(`##\\s*\\[v?${releaseVersion}\\][\\s\\S]*?(?=##\\s*\\[|\\Z)`, 'i');
    const versionMatch = changelog.match(versionRegex);
    if (versionMatch) {
      releaseNotes = versionMatch[0].trim();
    } else {
      releaseNotes = `### Added\n- Release version ${releaseVersion}`;
      const newSection = `## [Unreleased]\n\n## [${releaseVersion}] - ${today}\n${releaseNotes}\n\n`;
      changelog = changelog.replace(/##\s*\[Unreleased\]/i, newSection);
    }
  }

  // 3. Update gradle.properties with the release version
  gradleProps = gradleProps.replace(/app\.version=[^\r\n]+/, `app.version=${releaseVersion}`);

  fs.writeFileSync(gradlePropsPath, gradleProps, 'utf8');
  fs.writeFileSync(changelogPath, changelog, 'utf8');

  // 4. Write release notes file for GitHub Release
  const releaseNotesPath = path.join(rootDir, 'RELEASE_NOTES.md');
  fs.writeFileSync(releaseNotesPath, `## Notes Server v${releaseVersion} (${today})\n\n${releaseNotes}\n`, 'utf8');

  console.log(`RELEASE_NOTES.md generated successfully.`);

  // 5. Output to GitHub Actions step outputs
  if (githubOutputPath && fs.existsSync(githubOutputPath)) {
    fs.appendFileSync(githubOutputPath, `version=${releaseVersion}\n`);
    fs.appendFileSync(githubOutputPath, `tag=v${releaseVersion}\n`);
    fs.appendFileSync(githubOutputPath, `release_notes_path=${releaseNotesPath}\n`);
  }
}

try {
  run();
} catch (err) {
  console.error('Error during release preparation:', err);
  process.exit(1);
}
