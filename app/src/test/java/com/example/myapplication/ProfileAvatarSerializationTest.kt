package com.example.myapplication

import com.example.myapplication.network.UpdateMasterProfileRequest
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileAvatarSerializationTest {
    @Test
    fun profileUpdateIncludesExistingAvatar() {
        val avatar = "/media/masters/master1.jpg"
        val request = UpdateMasterProfileRequest(avatar, "Самара", "Опыт работы", listOf(1L))
        val body = JsonParser.parseString(Gson().toJson(request)).asJsonObject
        assertEquals(avatar, body.get("avatarUrl").asString)
    }
}
