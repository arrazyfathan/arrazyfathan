package com.arrazyfathan

fun createReadMe(
    githubActivity: List<ActivityItem>
): String {
    return """
![Ar Razy Fathan Rabbani Banner](https://github.com/arrazyfathan/arrazyfathan/blob/main/media/banner-dark.png#gh-dark-mode-only)
![Ar Razy Fathan Rabbani Banner](https://github.com/arrazyfathan/arrazyfathan/blob/main/media/banner-light.png#gh-light-mode-only)

## Recent GitHub Activity

${githubActivity.joinToString("\n\n") { it.toString() }}
                
<sub>Auto-updated from GitHub activity. Inspired by <a href="https://github.com/ZacSweers/ZacSweers/">Zac Sweers' auto-updating profile README</a>.</sub>

## Coding Activity

<!--START_SECTION:waka-->
<!--END_SECTION:waka-->

---
© 2026 — Ar Razy Fathan Rabbani
  """.trimIndent()
}
