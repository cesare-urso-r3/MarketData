package com.marketdata.states

import net.corda.core.contracts.ContractState
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties


// turn a map of name value pairs into a 2 column table
data class Table (val title: String, private val data : Map<String,String>) {

    var maxName = 0
    var maxVal = 0
    val maxLength get() = maxName + maxVal

    val corner = "+"
    val line = "-"
    val side = "|"

    private fun renderLine() :String {
        return corner + line.repeat(maxLength + 5)  + corner + '\n'
    }

    private fun renderTitle(title : String) :String {
        return "$side $title${" ".repeat(maxLength - title.length + 3)} $side\n"
    }

    private fun renderDataLine(nameVal : Pair<String,String>) : String {
        val key = nameVal.first
        val value = nameVal.second
        return "$side ${key}${" ".repeat(maxName - key.length)} $side $value${" ".repeat(maxVal - value.length)} $side\n"
    }

    init {
        data.forEach{
            maxName = maxOf(maxName, it.key.length)
            maxVal = maxOf(maxVal, it.value.length)
        }
    }

    fun toTable() : String {

        var ret = renderLine()
        ret += renderTitle(title)
        ret += renderLine()

        data.forEach {
            ret += renderDataLine(it.toPair())
        }
        ret+= renderLine()
        return ret
    }

    override fun toString(): String {
        return toTable()
    }
}

fun ContractState.toTable() : String {

    val fields = this::class.declaredMemberProperties
            .filter {
                it.visibility == KVisibility.PUBLIC
            }
            .sortedBy {
                it.name
            }
            .map {
                it.name to it.getter.call(this).toString()
            }.toMap()

    return Table(
                this.javaClass.name.substringAfterLast('.'),
                fields
            ).toTable()
}
