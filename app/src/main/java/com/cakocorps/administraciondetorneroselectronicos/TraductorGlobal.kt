package com.cakocorps.administraciondetorneroselectronicos

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

object TraductorGlobal {

    private var traductor: Translator? = null
    // Detectamos el idioma del celular del usuario
    private val idiomaDispositivo = Locale.getDefault().language
    private var estaListo = false

    fun prepararTraductor() {
        // descargar solo si habla español
        if (idiomaDispositivo == "es") return

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.SPANISH)
            .setTargetLanguage(TranslateLanguage.ENGLISH) // Lo pasamos a inglés
            .build()

        traductor = Translation.getClient(options)


        val conditions = DownloadConditions.Builder().requireWifi().build()

        traductor?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener { estaListo = true }
            ?.addOnFailureListener { estaListo = false }
    }

    // traducti
    fun traducir(textoEnEspanol: String, alTerminar: (String) -> Unit) {

        // devolver texto original en caso de error
        if (idiomaDispositivo == "es" || traductor == null || !estaListo) {
            alTerminar(textoEnEspanol)
            return
        }

        // traducir
        traductor?.translate(textoEnEspanol)
            ?.addOnSuccessListener { textoTraducido ->
                alTerminar(textoTraducido)
            }
            ?.addOnFailureListener {
                alTerminar(textoEnEspanol) // Si hay un error mostramos el original
            }
    }
}