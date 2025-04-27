package com.example.sos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sos.model.Address
import com.example.sos.model.MedicalInfo
import com.example.sos.model.User
import com.example.sos.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application): AndroidViewModel(application) {
    private val repo = UserRepository(application)

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    // User data state
    var user = User()
    var address = Address()
    var medicalInfo = MedicalInfo()
    var emergencyContact = ""
    var emergencyContactName = ""
    var password = ""
    var confirmPassword = ""
    var termsAccepted = false

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        data class Success(val user: User) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    fun register() {
        if (!termsAccepted) {
            _registrationState.value = RegistrationState.Error("Please accept terms and conditions")
            return
        }

        if (!isRegistrationDataValid()) {
            _registrationState.value = RegistrationState.Error("Please fill all required fields correctly")
            return
        }

        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                // Combine all data
                val completeUser = user.copy(
                    address = address,
                    medicalReport = medicalInfo,
                    emergencyContact = emergencyContact,
                    password = password
                )

                // Call repository to register
                val response = repo.register(
                    email = completeUser.email,
                    name = completeUser.name,
                    password = completeUser.password
                )

                if (response.isSuccessful) {
                    _registrationState.value = RegistrationState.Success(completeUser)
                } else {
                    _registrationState.value = RegistrationState.Error(
                        response.message() ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(
                    e.message ?: "An error occurred during registration"
                )
            }
        }
    }

    private fun isRegistrationDataValid(): Boolean {
        return user.name.isNotEmpty() &&
                user.email.isNotEmpty() && user.email.contains("@") &&
                user.phone.isNotEmpty() && user.phone.length >= 10 &&
                user.govId.isNotEmpty() && user.govId.length >= 12 &&
                password.length >= 6 && password == confirmPassword &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() } &&
                address.line1.isNotEmpty() &&
                address.pinCode != null && address.pinCode.toString().length == 6 &&
                address.city.isNotEmpty() &&
                address.state.isNotEmpty() &&
                emergencyContact.isNotEmpty() && emergencyContact.length >= 10 &&
                emergencyContactName.isNotEmpty() &&
                isMedicalInfoValid(medicalInfo)
    }

    internal fun isMedicalInfoValid(medicalInfo: MedicalInfo): Boolean {
        return medicalInfo.dob?.isNotEmpty() == true &&
                medicalInfo.gender?.isNotEmpty() == true &&
                medicalInfo.height != null &&
                medicalInfo.weight != null &&
                medicalInfo.bloodGroup?.isNotEmpty() == true &&
                medicalInfo.allergies?.isNotEmpty() == true &&
                medicalInfo.medicalConditions?.isNotEmpty() == true &&
                medicalInfo.medications?.isNotEmpty() == true &&
                medicalInfo.surgeries?.isNotEmpty() == true &&
                (medicalInfo.gender?.lowercase() != "female" || medicalInfo.pregnancyStatus?.isNotEmpty() == true)
    }


    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resp = repo.login(email, password)
            onResult(resp != null)
        }
    }

    fun sendSos(latitude: Double, longitude: Double) {
        viewModelScope.launch {

            repo.sendSos(latitude, longitude)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onComplete()
        }
    }
}
/*abc*/