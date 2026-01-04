package dev.tjpal.nodes.template

/**
 * TemplateEngine simplified for named-arguments-only tool invocations and replacement of static variables.
 *
 * Following operations are supported:
 * - Variable substitution: {{variableName}}
 * - Tool invocation with named arguments: {{toolName(key1 = value1, key2 = value2)}}
 *
 * The engine is suspendable so tool invokers can perform IO. No escaping beyond basic backslash in quoted strings.
 */
class TemplateEngine {
    // Regex captures either a variable reference or a tool invocation.
    // group(1) = name (variable or tool), group(2) = args content (optional, present for tool invocations)
    private val placeholderRegex = Regex("\\{\\{\\s*([A-Za-z0-9_]+)\\s*(?:\\((.*?)\\))?\\s*\\}\\}", RegexOption.DOT_MATCHES_ALL)

    /**
     * Render the template.
     *
     * @param template input template containing placeholders
     * @param variables variable map; missing variable causes IllegalArgumentException
     * @param llmToolInvoker adapter to invoke global LLM tools (suspend)
     * @param staticTools node-local static tools (suspend)
     */
    suspend fun render(
        template: String,
        variables: Map<String, Any?>,
        llmToolInvoker: suspend (toolName: String, positionalArgs: List<Any?>, namedArgs: Map<String, Any?>) -> String,
        staticTools: Map<String, suspend (positionalArgs: List<Any?>, namedArgs: Map<String, Any?>, variables: Map<String, Any?>) -> String> = emptyMap()
    ): String {
        // Pre-check for unclosed placeholders: ensure every '{{' has a matching '}}'
        var scanIdx = 0
        while (true) {
            val open = template.indexOf("{{", scanIdx)
            if (open == -1) break
            val close = template.indexOf("}}", open + 2)
            if (close == -1) {
                throw IllegalArgumentException("Unclosed placeholder starting at position $open")
            }
            scanIdx = close + 2
        }

        val stringBuilder = StringBuilder()

        var lastIndex = 0
        val matcher = placeholderRegex.findAll(template)
        for (match in matcher) {
            val range = match.range
            // append literal text between last match and this one
            if (lastIndex < range.first) stringBuilder.append(template.substring(lastIndex, range.first))

            val name = match.groupValues[1]
            val argsText = match.groups[2]?.value

            if (argsText == null) {
                // variable substitution
                if (!variables.containsKey(name)) {
                    throw IllegalArgumentException("Missing variable: $name")
                }
                val value = variables[name]
                stringBuilder.append(value?.toString() ?: "")
            } else {
                // tool invocation - argsText is the raw content between parentheses
                val named = parseNamedArgumentsFromRaw(argsText)

                val result: String = if (staticTools.containsKey(name)) {
                    val tool = staticTools[name]!!
                    tool(emptyList(), named, variables)
                } else {
                    llmToolInvoker(name, emptyList(), named)
                }
                stringBuilder.append(result)
            }

            lastIndex = range.last + 1
        }

        if (lastIndex < template.length) stringBuilder.append(template.substring(lastIndex))

        return stringBuilder.toString()
    }

    private fun parseNamedArgumentsFromRaw(argsText: String): Map<String, Any?> {
        val text = argsText.trim()
        if (text.isEmpty()) return emptyMap()

        val parts = splitArgs(text)

        // kvRegex matches: key = "quoted string"  OR  key = unquotedValue
        // group(1) = key, group(2) = quoted content (with escapes), group(3) = unquoted raw value
        val kvRegex = Regex("""^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?:"((?:\\.|[^\\"])*)"|([^\"]+?))\s*$""")

        val named = mutableMapOf<String, Any?>()
        for (part in parts) {
            val token = part.trim()
            if (token.isEmpty()) continue

            val m = kvRegex.matchEntire(token)
                ?: throw IllegalArgumentException("Only named arguments supported; invalid token: '$token'")

            val key = m.groupValues[1]
            if (!isValidIdentifier(key)) throw IllegalArgumentException("Invalid named-arg key: '$key'")
            if (named.containsKey(key)) throw IllegalArgumentException("Duplicate named argument: '$key'")

            val quoted = m.groups[2]?.value
            val rawUnquoted = m.groups[3]?.value

            val value: Any? = if (quoted != null) {
                // quoted contains the raw content of the quoted group with escapes preserved
                unescapeString(quoted)
            } else if (!rawUnquoted.isNullOrEmpty()) {
                parseValueToken(rawUnquoted.trim())
            } else {
                throw IllegalArgumentException("Empty argument value")
            }

            named[key] = value
        }

        return named
    }

    private fun isValidIdentifier(s: String): Boolean {
        if (s.isEmpty()) return false
        return s.all { it.isLetterOrDigit() || it == '_' }
    }

    /**
     * Split a comma-separated argument string into tokens, ignoring commas that occur inside double-quoted strings.
     *
     * This small helper exists to avoid a full parser while still supporting quoted string values that may contain
     * commas. It scans the input and tracks whether it is currently inside an unescaped double-quoted string; commas
     * seen while inside such a string are treated as literal characters and do not split the token.
     */
    private fun splitArgs(s: String): List<String> {
        val parts = mutableListOf<String>()
        var inQuote = false
        var last = 0
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '"') {
                // determine whether this quote is escaped by counting preceding backslashes
                var backslashes = 0
                var j = i - 1
                while (j >= 0 && s[j] == '\\') { backslashes++; j-- }
                if (backslashes % 2 == 0) {
                    inQuote = !inQuote
                }
                i++
                continue
            }
            if (c == ',' && !inQuote) {
                parts.add(s.substring(last, i))
                last = i + 1
            }
            i++
        }
        parts.add(s.substring(last))
        return parts
    }

    private fun parseValueToken(token: String): Any? {
        val t = token.trim()
        if (t.isEmpty()) throw IllegalArgumentException("Empty argument value")
        if (t.startsWith("\"") && t.endsWith("\"")) {
            return unescapeString(t.substring(1, t.length - 1))
        }
        val lower = t.lowercase()
        if (lower == "true") return true
        if (lower == "false") return false

        if (t.matches(Regex("^-?\\d+\\.\\d+[fF]$"))) {
            return t.substring(0, t.length - 1).toFloat()
        }
        if (t.matches(Regex("^-?\\d*\\.\\d+(?:[eE][-+]?\\d+)?$") ) || t.matches(Regex("^-?\\d+(?:[eE][-+]?\\d+)$"))) {
            return t.toDouble()
        }
        if (t.matches(Regex("^-?\\d+$"))) {
            return try { t.toInt() } catch (e: NumberFormatException) { t.toLong() }
        }

        // fallback: accept raw unquoted token as string
        return t
    }

    private fun unescapeString(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                sb.append(
                    when (n) {
                        '\\' -> '\\'
                        '"' -> '"'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> n
                    }
                )
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
