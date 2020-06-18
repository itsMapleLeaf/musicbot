import org.unbescape.html.HtmlEscape

fun String.markdownEscape(): String =
    HtmlEscape.unescapeHtml(this).replace(Regex("[_~*]")) { "\\${it.value}" }
