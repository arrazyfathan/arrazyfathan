package com.arrazyfathan

fun createReadMe(
    githubActivity: List<ActivityItem>
): String {
    return """
![Ar Razy Fathan Rabbani Banner](https://github.com/arrazyfathan/arrazyfathan/blob/main/media/banner-dark.png#gh-dark-mode-only)
![Ar Razy Fathan Rabbani Banner](https://github.com/arrazyfathan/arrazyfathan/blob/main/media/banner-light.png#gh-light-mode-only)

### <img width="30" alt="about" src="https://github.com/arrazyfathan/arrazyfathan/blob/main/media/about.png"> More about me

```kotlin
val Razy = human {
    about {
        name = "Ar Razy Fathan Rabbani"
        role = "Android Engineer"
    }

    tech {
        day("Kotlin", "Android")
    }

    links {
        twitter = "@arrazyfathann"
        website = "arrazyfathan.com"
        linkedin = "linkedin.com/in/arrazyfathan"
        email = "arrazy.rabbani266@gmail.com"
        mastodon = "@arrazyfathan@androiddev.social"
        instagram = "arrazyfathan"
    }

    more {
      listOf(
        "- 🔭 I’m currently working on Android/Kotlin ",
        "- 👯 I’m looking to collaborate on Android projects ",
        "- 💬 Ask me about Android and Kotlin ",
        "- 📫 How to reach me: arrazy.rabbani266@gmail.com"
      )
    }
}
```


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