package app.miogram.core.ai

/**
 * What may leave the device. Cloud tasks receive text that passed through
 * [redact]; the policy is intentionally conservative and mechanical so it is
 * testable without judgement calls.
 *
 * This is a best-effort scrubber, not a security boundary — the real boundary
 * is the task allow-list (no raw history dumps, no contact graphs). It
 * protects against the common accident: forwarding a message containing
 * credentials/phone numbers into a cloud summarizer.
 */
object CloudPrivacyPolicy {

    private val PHONE = Regex(
        """(?<![\d])(?:\+?\d[\d\s\-()]{7,17}\d)(?![\d])"""
    )

    private val CARD_LIKE = Regex(
        """(?<![\d])(?:\d[ \-]?){13,19}(?![\d])"""
    )

    private val EMAIL = Regex(
        """[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"""
    )

    /** Long hex/base64-ish blobs: tokens, keys, session identifiers. */
    private val SECRET_BLOB = Regex(
        """(?i)(?<![a-z0-9])(?:[a-f0-9]{32,}|[a-z0-9+/=_-]{40,})(?![a-z0-9])"""
    )

    data class Report(val redactedText: String, val replacements: Int)

    fun redact(text: String): Report {
        var count = 0

        var out = text
        out = CARD_LIKE.replace(out) { count++; "[card]" }
        // Phones after cards so long card numbers are not double-matched.
        out = PHONE.replace(out) { count++; "[phone]" }
        out = SECRET_BLOB.replace(out) { count++; "[secret]" }
        out = EMAIL.replace(out) { count++; "[email]" }

        return Report(out, count)
    }
}
