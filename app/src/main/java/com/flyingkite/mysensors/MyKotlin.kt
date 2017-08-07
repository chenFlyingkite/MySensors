package com.flyingkite.mysensors

class MyKotlin {

    fun main(vararg args: String) {
        
    }

    fun f(tag: String?, msg: String?) {
        var n: String = "tag = $tag, msg = $msg"
        var a: Int = tag?.length ?: 0
        val b: Int = msg?.length ?: 0
        a++
        //b++ //val cannot be reassigned


    }
}