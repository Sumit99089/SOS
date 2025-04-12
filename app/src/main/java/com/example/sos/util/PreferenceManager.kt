package com.example.sos.util


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sos_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val IS_REGISTERED = booleanPreferencesKey("is_registered")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val PHONE = stringPreferencesKey("phone")
        val DOB = stringPreferencesKey("dob")
        val HEIGHT = stringPreferencesKey("height")
        val WEIGHT = stringPreferencesKey("weight")
        val ALLERGIES = stringPreferencesKey("allergies")
        val PREGNANCY_STATUS = stringPreferencesKey("pregnancy_status")
        val MEDICATIONS = stringPreferencesKey("medications")
        val ADDRESS = stringPreferencesKey("address")
    }

    val isRegistered: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_REGISTERED] ?: false
        }

    suspend fun saveUserData(
        name: String,
        email: String,
        phone: String,
        dob: String,
        height: String,
        weight: String,
        allergies: String,
        pregnancyStatus: String,
        medications: String,
        address: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[NAME] = name
            preferences[EMAIL] = email
            preferences[PHONE] = phone
            preferences[DOB] = dob
            preferences[HEIGHT] = height
            preferences[WEIGHT] = weight
            preferences[ALLERGIES] = allergies
            preferences[PREGNANCY_STATUS] = pregnancyStatus
            preferences[MEDICATIONS] = medications
            preferences[ADDRESS] = address
            preferences[IS_REGISTERED] = true
        }
    }

    suspend fun logOut() {
        context.dataStore.edit { preferences ->
            preferences[IS_REGISTERED] = false
        }
    }

    fun getUserData(): Flow<UserData> {
        return context.dataStore.data.map { preferences ->
            UserData(
                name = preferences[NAME] ?: "",
                email = preferences[EMAIL] ?: "",
                phone = preferences[PHONE] ?: "",
                dob = preferences[DOB] ?: "",
                height = preferences[HEIGHT] ?: "",
                weight = preferences[WEIGHT] ?: "",
                allergies = preferences[ALLERGIES] ?: "",
                pregnancyStatus = preferences[PREGNANCY_STATUS] ?: "",
                medications = preferences[MEDICATIONS] ?: "",
                address = preferences[ADDRESS] ?: ""
            )
        }
    }
}

data class UserData(
    val name: String,
    val email: String,
    val phone: String,
    val dob: String,
    val height: String,
    val weight: String,
    val allergies: String,
    val pregnancyStatus: String,
    val medications: String,
    val address: String
)
