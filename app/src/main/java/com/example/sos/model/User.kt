package com.example.sos.model



data class User(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val govId: String = "",
    val address: Address = Address(),
    val emergencyContact: String = "",
    val password: String = "",
    val medicalReport: MedicalInfo = MedicalInfo(),
    val profileImage: String = "https://res.cloudinary.com/your-cloud/image/upload/default-profile.jpg"
)
