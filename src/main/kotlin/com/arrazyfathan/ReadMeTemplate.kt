package com.arrazyfathan

fun createReadMe(
    githubActivity: List<ActivityItem>
): String {
    return """
![Ar Razy Fathan Rabbani Banner](https://github.com/arrazyfathan/arrazyfathan/blob/main/media/banner-dark.png#gh-dark-mode-only)
![Ar Razy Fathan Rabbani Banner](https://github.com/arrazyfathan/arrazyfathan/blob/main/media/banner-light.png#gh-light-mode-only)

<table><tr><td valign="top" width="100%">    

## GitHub Activity

${githubActivity.joinToString("\n\n") { it.toString() }}
                
<sub><a href="https://github.com/ZacSweers/ZacSweers/">Inspired by Zac Sweeners's auto-updating profile README with Kotlin Implementation.</a></sub>
</table>

<!--START_SECTION:waka-->
<!--END_SECTION:waka-->

---
© 2024 — Ar Razy Fathan Rabbani
  """.trimIndent()
}