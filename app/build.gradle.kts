import java.time.Instant

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shuyah.shuplay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shuyah.shuplay"
        minSdk = 24                      // Android 7.0
        targetSdk = 35
        // versionCode : entier qui augmente à chaque version ; versionName : numéro affiché dans l'application (et envoyé à SAWALI)
        versionCode = 1
        versionName = "1.0"
        // Date et heure de compilation (UTC) : affichée sous la forme « Version 1.0 · déployée le JJ/MM/AAAA HH:MM »
        buildConfigField("String", "DATE_BUILD", "\"${Instant.now()}\"")
        // Numéro de lot (règle 1) : numéro de la PR fusionnée, relevé à la main à chaque livraison
        buildConfigField("String", "LOT", "\"1\"")
    }

    // Signature de la version de publication. AUCUNE clé ni mot de passe dans le dépôt : les 4 valeurs viennent de variables d'environnement
    // (ou de propriétés Gradle -P…) saisies par le propriétaire : ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD.
    // Sans elles, -PsignerAvecCleLocale=true signe avec la clé de TEST de l'environnement (livraison à la main).
    fun valeurSignature(nom: String): String? = ((project.findProperty(nom) as String?) ?: System.getenv(nom))?.takeIf { it.isNotBlank() }
    val cheminCle = valeurSignature("ANDROID_KEYSTORE_FILE")
    val motDePasseFichier = valeurSignature("ANDROID_KEYSTORE_PASSWORD")
    val aliasCle = valeurSignature("ANDROID_KEY_ALIAS")
    val motDePasseCle = valeurSignature("ANDROID_KEY_PASSWORD")
    val signatureDisponible = cheminCle != null && file(cheminCle).exists() && motDePasseFichier != null && aliasCle != null && motDePasseCle != null

    signingConfigs {
        if (signatureDisponible) {
            create("release") {
                storeFile = file(cheminCle!!)
                storePassword = motDePasseFichier
                keyAlias = aliasCle
                keyPassword = motDePasseCle
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signatureDisponible) signingConfig = signingConfigs.getByName("release")
            else if ((project.findProperty("signerAvecCleLocale") as String?) == "true") signingConfig = signingConfigs.getByName("debug")
        }
        debug { isMinifyEnabled = false }
    }

    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Lecture : ExoPlayer (moteur), session média (notification, casque, écran verrouillé), interface du lecteur
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    // Réseau : signal de présence à SAWALI (règle 4)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
