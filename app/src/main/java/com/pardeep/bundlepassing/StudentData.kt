package com.pardeep.bundlepassing

import com.google.firebase.database.Exclude

data class StudentData(
    var id : String?="",
    var name : String?="",
    var age : Int?=0,
    var image : String?=null
){
    @Exclude
    fun toMap() : Map<String,Any?>{
        return mapOf(
            "id" to id,
            "name" to name,
            "age" to age,
            "image" to image
        )
    }
}
