package com.example.pi_03_equipe_01

data class HistoryItem(
    val id: String,
    val date: String = "",
    val status: Status
)
 {
    enum class Status(val texto: String, val cor: Int, val icone: Int) {
        FINALIZADO("Finalizado", R.drawable.ic_circle_green, R.drawable.ic_info_green),
        EM_ANDAMENTO("Em andamento", R.drawable.ic_circle_orange, R.drawable.ic_info_orange),
        NAO_INICIADO("Não iniciado", R.drawable.ic_circle_gray, R.drawable.ic_info_gray)
    }
}
