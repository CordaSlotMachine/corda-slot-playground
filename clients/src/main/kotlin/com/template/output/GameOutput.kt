package com.template.output
//{"reels":[1,6,5],"prize":null,"success":true,"balance":96,"day_winnings":0,"lifetime_winnings":25}
data class GameOutput(val reels: List<Int>, val prize: Long, val success: Boolean, val balance: Long, val dayWinnings: Long = 0, val lifeWinnings:Long = 0) {
}