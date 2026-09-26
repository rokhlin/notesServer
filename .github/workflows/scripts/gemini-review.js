// AI PR Code Review using Google Gemini API
const fs = require('fs');
const { execSync } = require('child_process');

async function run() {
  const apiKey = process.env.GEMINI_API_KEY;
  const githubToken = process.env.GITHUB_TOKEN;
  const repo = process.env.GITHUB_REPOSITORY;
  const prNumber = process.env.PR_NUMBER;
  const baseRef = process.env.BASE_REF || 'main';
  const stepSummaryFile = process.env.GITHUB_STEP_SUMMARY;

  function appendSummary(markdown) {
    if (stepSummaryFile && fs.existsSync(stepSummaryFile)) {
      fs.appendFileSync(stepSummaryFile, markdown + '\n\n');
    }
  }

  if (!apiKey) {
    console.log('GEMINI_API_KEY is not set. Skipping AI review.');
    appendSummary(`### 🤖 Gemini AI Code Review: Skipped
> [!NOTE]
> To enable automated AI PR code reviews, add \`GEMINI_API_KEY\` to your repository Secrets (*Settings > Secrets and variables > Actions*).`);
    return;
  }

  if (!prNumber) {
    console.log('No PR_NUMBER found. Skipping.');
    return;
  }

  console.log(`Analyzing PR #${prNumber} against origin/${baseRef}...`);

  let diff = '';
  try {
    diff = execSync(`git diff origin/${baseRef}...HEAD -- . ':!*.lock' ':!gradlew*' ':!*.gradle.cache'`, {
      encoding: 'utf8',
      maxBuffer: 10 * 1024 * 1024
    });
  } catch (err) {
    console.error('Failed to get git diff:', err.message);
    diff = execSync(`git diff HEAD~1...HEAD -- . ':!*.lock' ':!gradlew*'`, {
      encoding: 'utf8',
      maxBuffer: 10 * 1024 * 1024
    });
  }

  if (!diff.trim()) {
    console.log('Diff is empty. Nothing to review.');
    appendSummary('### 🤖 Gemini AI Code Review\nNo significant code changes found in diff.');
    return;
  }

  const maxDiffLength = 50000;
  let truncatedDiff = diff;
  if (diff.length > maxDiffLength) {
    truncatedDiff = diff.slice(0, maxDiffLength) + '\n\n... [Diff truncated due to size limits]';
  }

  const systemInstructions = `You are a Senior Kotlin Server Engineer (Ktor / Coroutines / REST APIs) and Cloud Security Specialist reviewing a Pull Request.
Provide a concise, constructive, and actionable code review of the git diff provided below.

Structure your review with the following sections:
1. 📋 **Summary**: 1-2 sentence overview of the changes and intent.
2. 💡 **Quality & Architecture**: Adherence to idiomatic Kotlin, Ktor server idioms, clean routing, error handling, and separation of concerns.
3. ⚠️ **Bugs, Edge Cases & Concurrency**: Async coroutines safety, unhandled exceptions, resource leaks, data serialization issues.
4. 🔒 **Security & Vulnerabilities**: Injection vulnerabilities, unauthenticated routes, CORS misconfigurations, sensitive data leaks.
5. 🛠️ **Actionable Recommendations**: Specific code improvement suggestions (with before/after diffs where helpful).

Be direct, constructive, and professional. If everything looks clean, state that clearly and highlight what was done well.`;

  const requestBody = {
    contents: [
      {
        parts: [
          { text: systemInstructions },
          { text: `### Pull Request Diff:\n\`\`\`diff\n${truncatedDiff}\n\`\`\`` }
        ]
      }
    ],
    generationConfig: {
      temperature: 0.2,
      maxOutputTokens: 3000
    }
  };

  console.log('Calling Google Gemini API...');
  const geminiEndpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey}`;

  let response;
  try {
    response = await fetch(geminiEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    });
  } catch (networkErr) {
    console.error('Network error calling Gemini API:', networkErr);
    appendSummary(`### 🤖 Gemini AI Code Review: Error\nFailed to connect to Gemini API: ${networkErr.message}`);
    return;
  }

  if (!response.ok) {
    const errorText = await response.text();
    console.error(`Gemini API returned status ${response.status}: ${errorText}`);
    appendSummary(`### 🤖 Gemini AI Code Review: API Error\nGemini API returned status \`${response.status}\`:\n\`\`\`\n${errorText}\n\`\`\``);
    return;
  }

  const responseData = await response.json();
  const reviewContent = responseData.candidates?.[0]?.content?.parts?.[0]?.text;

  if (!reviewContent) {
    console.error('No review content returned from Gemini:', JSON.stringify(responseData));
    return;
  }

  const commentBody = `## 🤖 Gemini AI Code Review (PR #${prNumber})

${reviewContent}

---
*Generated automatically by Google Gemini in GitHub Actions.*`;

  // Post comment to PR
  if (githubToken && repo && prNumber) {
    console.log(`Posting comment to PR #${prNumber} in ${repo}...`);
    try {
      const commentRes = await fetch(`https://api.github.com/repos/${repo}/issues/${prNumber}/comments`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${githubToken}`,
          'Accept': 'application/vnd.github.v3+json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ body: commentBody })
      });

      if (!commentRes.ok) {
        console.error('Failed to post comment to GitHub PR:', await commentRes.text());
      } else {
        console.log('Review comment posted successfully to PR.');
      }
    } catch (commentErr) {
      console.error('Error posting comment to PR:', commentErr);
    }
  }

  appendSummary(commentBody);
}

run().catch(err => {
  console.error('Unhandled script error:', err);
  process.exit(1);
});
