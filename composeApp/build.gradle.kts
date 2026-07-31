import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.util.Properties
import javax.inject.Inject

abstract class GenerateRuntimeConfigsTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Optional
    @get:InputFile
    abstract val localPropertiesFile: RegularFileProperty

    @get:Input
    abstract val appVersionName: Property<String>

    @get:Input
    abstract val appVersionCode: Property<Int>

    @get:Input
    abstract val supabaseUrl: Property<String>

    @get:Input
    abstract val supabaseAnonKey: Property<String>

    @get:Input
    abstract val nuvioSupabaseUrl: Property<String>

    @get:Input
    abstract val nuvioSupabaseAnonKey: Property<String>

    @get:Input
    abstract val supabaseFallbackUrl: Property<String>

    @get:Input
    abstract val privateSupabaseUrl: Property<String>

    @get:Input
    abstract val privateSupabaseAnonKey: Property<String>

    @get:Input
    abstract val usePrivateSupabase: Property<Boolean>

    @get:Input
    abstract val realtimeSyncEnabled: Property<Boolean>

    @get:Input
    abstract val syncBackendManifestUrl: Property<String>

    @get:Input
    abstract val desktopAppVersionName: Property<String>

    @get:Input
    abstract val desktopAppVersionCode: Property<Int>

    @get:Optional
    @get:OutputFile
    abstract val xcconfigFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val props = Properties()
        localPropertiesFile.asFile.orNull?.takeIf { it.exists() }?.inputStream()?.use { props.load(it) }

        val outDir = outputDir.get().asFile
        outDir.resolve("com/nuvio/app/core/network").apply {
            mkdirs()
            resolve("SupabaseConfig.kt").writeText(
                """
                |package com.nuvio.app.core.network
                |
                |object SupabaseConfig {
                |    const val URL = "${supabaseUrl.get()}"
                |    const val ANON_KEY = "${supabaseAnonKey.get()}"
                |    const val NUVIO_URL = "${nuvioSupabaseUrl.get()}"
                |    const val NUVIO_ANON_KEY = "${nuvioSupabaseAnonKey.get()}"
                |    const val FALLBACK_URL = "${supabaseFallbackUrl.get()}"
                |    const val PRIVATE_URL = "${privateSupabaseUrl.get()}"
                |    const val PRIVATE_ANON_KEY = "${privateSupabaseAnonKey.get()}"
                |    const val USE_PRIVATE = ${usePrivateSupabase.get()}
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/core/sync").apply {
            mkdirs()
            resolve("RealtimeSyncConfig.kt").writeText(
                """
                |package com.nuvio.app.core.sync
                |
                |object RealtimeSyncConfig {
                |    const val ENABLED = ${realtimeSyncEnabled.get()}
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/tmdb/TmdbConfig.kt").delete()

        outDir.resolve("com/nuvio/app/features/trakt").apply {
            mkdirs()
            resolve("TraktConfig.kt").writeText(
                """
                |package com.nuvio.app.features.trakt
                |
                |object TraktConfig {
                |    const val CLIENT_ID = "${props.getProperty("TRAKT_CLIENT_ID", "")}" 
                |    const val CLIENT_SECRET = "${props.getProperty("TRAKT_CLIENT_SECRET", "")}" 
                |    const val REDIRECT_URI = "${props.getProperty("TRAKT_REDIRECT_URI", "nuvio://auth/trakt")}" 
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/player/skip").apply {
            mkdirs()
            resolve("IntroDbConfig.kt").writeText(
                """
                |package com.nuvio.app.features.player.skip
                |
                |object IntroDbConfig {
                |    const val URL = "${props.getProperty("INTRODB_API_URL", "")}" 
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/details").apply {
            mkdirs()
            resolve("ImdbEpisodeRatingsConfig.kt").writeText(
                """
                |package com.nuvio.app.features.details
                |
                |object ImdbEpisodeRatingsConfig {
                |    const val IMDB_RATINGS_API_BASE_URL = "${props.getProperty("IMDB_RATINGS_API_BASE_URL", "")}" 
                |    const val IMDB_TAPFRAME_API_BASE_URL = "${props.getProperty("IMDB_TAPFRAME_API_BASE_URL", "")}" 
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/mal").apply {
            mkdirs()
            resolve("MalConfig.kt").writeText(
                """
                |package com.nuvio.app.features.mal
                |
                |object MalConfig {
                |    const val CLIENT_ID = "${props.getProperty("MAL_CLIENT_ID", "")}" 
                |    const val CLIENT_SECRET = "${props.getProperty("MAL_CLIENT_SECRET", "")}" 
                |    const val REDIRECT_URI = "${props.getProperty("MAL_REDIRECT_URI", "nuvio://auth/mal")}" 
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/anilist").apply {
            mkdirs()
            resolve("AniListConfig.kt").writeText(
                """
                |package com.nuvio.app.features.anilist
                |
                |object AniListConfig {
                |    const val CLIENT_ID = "${props.getProperty("ANILIST_CLIENT_ID", "")}" 
                |    const val CLIENT_SECRET = "${props.getProperty("ANILIST_CLIENT_SECRET", "")}" 
                |    const val REDIRECT_URI = "${props.getProperty("ANILIST_REDIRECT_URI", "nuvio://auth/anilist")}" 
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/kitsu").apply {
            mkdirs()
            resolve("KitsuConfig.kt").writeText(
                """
                |package com.nuvio.app.features.kitsu
                |
                |object KitsuConfig {
                |    const val CLIENT_ID = "${props.getProperty("KITSU_CLIENT_ID", "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd")}" 
                |    const val CLIENT_SECRET = "${props.getProperty("KITSU_CLIENT_SECRET", "")}" 
                |    const val REDIRECT_URI = "${props.getProperty("KITSU_REDIRECT_URI", "nuvio://auth/kitsu")}" 
                |}
                """.trimMargin()
            )
        }

        outDir.resolve("com/nuvio/app/features/simkl").apply {
            mkdirs()
            resolve("SimklConfig.kt").writeText(
                """
                |package com.nuvio.app.features.simkl
                |
                |object SimklConfig {
                |    const val CLIENT_ID = "${props.getProperty("SIMKL_CLIENT_ID") ?: System.getenv("SIMKL_CLIENT_ID") ?: ""}" 
                |    const val CLIENT_SECRET = "${props.getProperty("SIMKL_CLIENT_SECRET") ?: System.getenv("SIMKL_CLIENT_SECRET") ?: ""}"
                |    const val REDIRECT_URI = "${props.getProperty("SIMKL_REDIRECT_URI", "nuvio://auth/simkl")}"
                |}
                """.trimMargin()
            )
        }
    }
}

// Configurazione dei plugin e dell'applicazione Android
android {
    // Gestione dello split per architetture (Genera i 5 APK separati)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}
