package com.ericsson.mts.asn1.util

import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.*

fun <E> Stack<E>.peekOrNull(): E? {
    return if (this.isEmpty()) {
        null
    } else {
        this.peek()
    }
}

fun <T> bestFuzzyMatch(query: String, candidates: List<T>, distanceThreshold: Int, transform: (T) -> String): String? {
    val ld = LevenshteinDistance(distanceThreshold)
    val candidatesToString = candidates.map(transform)
    val matching = candidatesToString.mapNotNull { candidate ->
        val distance = ld.apply(query, candidate)

        if (distance == -1) {
            return@mapNotNull null
        }

        val totalLength = (query.length + candidate.length).toDouble()
        var weightedScore = (totalLength - distance).div(totalLength)

        val longer: String
        val shorter: String
        if (query.length > candidate.length) {
            longer = query
            shorter = candidate
        } else {
            longer = candidate
            shorter = query
        }

        if (longer.contains(shorter)) {
            // boost partial match
            val boost = 1 + (shorter.length / longer.length.toDouble())
            weightedScore *= boost
        }

        weightedScore to candidate
    }
    return matching.maxByOrNull { it.first }?.second
}


