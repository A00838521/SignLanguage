package com.signlearn.app.ui.util

private val webRegex = Regex("\\bWeb\\b", RegexOption.IGNORE_CASE)

fun sanitizeTitle(raw: String?): String {
    if (raw == null) return ""
    val noWeb = raw.replace(webRegex, "").replace('_', ' ')
    return noWeb.replace("  +".toRegex(), " ").trim()
}

// Palabras conocidas para segmentar categorías concatenadas
private val knownTokens = listOf(
    "palabras","comunes","frases","basico","avanzado","intermedio","numeros","colores","dias","meses","saludos","familia","animales","comida","transporte","emociones","profesiones","casa","escuela","tecnologia","preguntas","conectores","expresiones","cotidianas","tiempo","clima","cuerpo","ropa","lugares","verbos","sustantivos","adjetivos","pronombres","preposiciones","adverbios","utiles","diarias"
)

private val qualifierTokens = setOf("comunes","basico","avanzado","intermedio","utiles","diarias","cotidianas")

fun formatCategory(raw: String): String {
    if (raw.isBlank()) return raw
    var work = raw.trim()
    // Normalizar separadores explícitos a espacios primero
    work = work.replace('_', ' ').replace('-', ' ').replace('+', ' ').replace(Regex("\\s*/\\s*"), " / ")
    // Si contiene barras ya es multi-tema: normalizar espacios y capitalizar
    if (work.contains('/')) return normalizeSpaces(work).split("/").joinToString(" / ") { it.trim().replaceFirstChar { c -> c.titlecase() } }

    // Dividir camelCase
    val camelSplit = work.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    if (camelSplit.contains(' ')) {
        val tokens = camelSplit.split(Regex("\\s+")).map { it.lowercase() }
        return joinTokens(tokens)
    }

    // Todo minúsculas sin separadores: máximo ajuste por diccionario de tokens conocidos
    val lower = work.lowercase()
    val segments = mutableListOf<String>()
    var idx = 0
    while (idx < lower.length) {
        var match: String? = null
        // longest-match primero
        for (len in (lower.length - idx) downTo 2) {
            val sub = lower.substring(idx, idx + len)
            if (knownTokens.contains(sub)) { match = sub; break }
        }
        if (match != null) {
            segments += match
            idx += match.length
        } else {
            // agrupar letras sueltas hasta próximo token conocido
            segments += lower[idx].toString()
            idx++
        }
    }
    // Unir letras sueltas contiguas como una palabra
    val merged = mutableListOf<String>()
    var buffer = StringBuilder()
    for (s in segments) {
        if (s.length == 1 && !knownTokens.contains(s)) {
            buffer.append(s)
        } else {
            if (buffer.isNotEmpty()) { merged += buffer.toString(); buffer = StringBuilder() }
            merged += s
        }
    }
    if (buffer.isNotEmpty()) merged += buffer.toString()
    return joinTokens(merged)
}

private fun normalizeSpaces(s: String) = s.replace("  +".toRegex(), " ").trim()

private fun joinTokens(tokensRaw: List<String>): String {
    val tokens = tokensRaw.filter { it.isNotBlank() }
    if (tokens.isEmpty()) return ""
    val hasQual = tokens.any { qualifierTokens.contains(it) }
    val sep = if (!hasQual && tokens.size > 1) " / " else " "
    return tokens.joinToString(sep) { it.replaceFirstChar { c -> c.titlecase() } }.let { normalizeSpaces(it) }
}

