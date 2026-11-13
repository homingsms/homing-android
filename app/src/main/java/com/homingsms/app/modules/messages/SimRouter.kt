package com.homingsms.app.modules.messages

import com.homingsms.core.model.SimSelectionMode
import com.homingsms.core.telephony.DeviceHealthReader
import kotlin.random.Random

data class SimCandidate(val slot: Int, val phoneNumber: String?, val sentToday: Int, val inService: Boolean = true)

object SimRouter {
    fun select(
        mode: SimSelectionMode,
        candidates: List<SimCandidate>,
        defaultSlot: Int?,
        messageKey: Long,
        recipient: String?,
        failover: Boolean,
        random: Random = Random.Default
    ): Int? {
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedBy { it.slot }
        val chosen = when (mode) {
            SimSelectionMode.OSDefault -> sorted.firstOrNull { it.slot == defaultSlot }
            SimSelectionMode.RoundRobin -> sorted[Math.floorMod(messageKey, sorted.size.toLong()).toInt()]
            SimSelectionMode.Random -> sorted[random.nextInt(sorted.size)]
            SimSelectionMode.LeastUsed -> sorted.minWith(compareBy<SimCandidate> { it.sentToday }.thenBy { it.slot })
            SimSelectionMode.CarrierMatch -> carrierMatch(sorted, recipient) ?: sorted.firstOrNull { it.slot == defaultSlot }
        }
        if (!failover || chosen == null || chosen.inService) return chosen?.slot
        return sorted.filter { it.inService && it.slot != chosen.slot }.minByOrNull { it.sentToday }?.slot ?: chosen.slot
    }

    fun carrierMatch(candidates: List<SimCandidate>, recipient: String?): SimCandidate? {
        val target = digits(recipient)
        if (target.isEmpty()) return null
        return candidates
            .map { it to commonPrefix(digits(it.phoneNumber), target) }
            .filter { it.second >= MIN_PREFIX }
            .maxWithOrNull(compareBy<Pair<SimCandidate, Int>> { it.second }.thenByDescending { it.first.slot })
            ?.first
    }

    private fun digits(value: String?): String = value.orEmpty().filter { it.isDigit() }

    private fun commonPrefix(a: String, b: String): Int {
        var index = 0
        while (index < a.length && index < b.length && a[index] == b[index]) index++
        return index
    }

    private const val MIN_PREFIX = 4
}

class SimRoutingInputs(private val sentTodayPerSim: suspend () -> Map<Int, Int>, private val healthReader: DeviceHealthReader) {
    suspend fun candidates(slots: List<Int>, withUsage: Boolean, withHealth: Boolean): List<SimCandidate> {
        val sentToday = if (withUsage) sentTodayPerSim() else emptyMap()
        val health = if (withHealth) healthReader.read(sentToday, 0).sims.associateBy { it.slot } else emptyMap()
        return slots.map { slot ->
            val sim = health[slot]
            SimCandidate(
                slot = slot,
                phoneNumber = sim?.phoneNumber,
                sentToday = sentToday[slot] ?: 0,
                inService = sim?.let { it.inService && it.signalLevel != 0 } ?: true
            )
        }
    }
}
