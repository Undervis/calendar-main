package com.example.addtocalendar

object MonthAdapter {
    private val monthArray = arrayOf("Месяц", "Январь", "Февраль", "Март",
        "Апрель", "Май", "Июнь", "Июль", "Август",
        "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")

    fun getMonthByInt(month: Int) : String {
        return monthArray[month]
    }

    fun getMonthArray() : Array<String> {
        return monthArray
    }
}