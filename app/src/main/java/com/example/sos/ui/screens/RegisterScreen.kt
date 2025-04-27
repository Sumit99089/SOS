import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.sos.R
import com.example.sos.model.Address
import com.example.sos.model.MedicalInfo
import com.example.sos.model.User
import com.example.sos.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavHostController) {
    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    // Observe registration state
    LaunchedEffect(viewModel) {
        viewModel.registrationState.collectLatest { state ->
            when (state) {
                is AuthViewModel.RegistrationState.Success -> {
                    Toast.makeText(
                        context,
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate("home") {
                        popUpTo("registration") { inclusive = true }
                    }
                }
                is AuthViewModel.RegistrationState.Error -> {
                    Toast.makeText(
                        context,
                        state.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Your Account") },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress indicator
            RegistrationProgress(currentStep, totalSteps)

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> PersonalInfoStep(
                            user = viewModel.user,
                            onUserChange = { viewModel.user = it },
                            password = viewModel.password,
                            onPasswordChange = { viewModel.password = it },
                            confirmPassword = viewModel.confirmPassword,
                            onConfirmPasswordChange = { viewModel.confirmPassword = it }
                        )
                        2 -> AddressStep(
                            address = viewModel.address,
                            onAddressChange = { viewModel.address = it }
                        )
                        3 -> EmergencyContactStep(
                            name = viewModel.emergencyContactName,
                            onNameChange = { viewModel.emergencyContactName = it },
                            phone = viewModel.emergencyContact,
                            onPhoneChange = { viewModel.emergencyContact = it }
                        )
                        4 -> MedicalInfoStep(
                            medicalInfo = viewModel.medicalInfo,
                            onMedicalInfoChange = { viewModel.medicalInfo = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    Button(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }

                if (currentStep < totalSteps) {
                    Button(
                        onClick = { currentStep++ },
                        enabled = isStepValid(
                            currentStep,
                            viewModel.user,
                            viewModel.password,
                            viewModel.confirmPassword,
                            viewModel.address
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    val isLoading = viewModel.registrationState.value is AuthViewModel.RegistrationState.Loading

                    Button(
                        onClick = { viewModel.register() },
                        enabled = viewModel.isMedicalInfoValid(viewModel.medicalInfo) &&
                                viewModel.termsAccepted && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Complete Registration")
                        }
                    }
                }
            }

            if (currentStep == totalSteps) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = viewModel.termsAccepted,
                        onCheckedChange = { viewModel.termsAccepted = it }
                    )
                    Text(
                        text = "I agree to the Terms of Service and Privacy Policy",
                        modifier = Modifier.clickable { viewModel.termsAccepted = !viewModel.termsAccepted }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { /* Open help center */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("NEED HELP?")
                }
            }
        }
    }
}

@Composable
fun RegistrationProgress(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(totalSteps) { step ->
            val stepNumber = step + 1
            val isCurrentStep = stepNumber == currentStep
            val isCompleted = stepNumber < currentStep

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            isCurrentStep -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> Color.LightGray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalInfoStep(
    user: User,
    onUserChange: (User) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text("Please provide your basic details to get started", modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = user.name,
            onValueChange = { onUserChange(user.copy(name = it)) },
            label = { Text("Full Name *") },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = user.name.isEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = user.email,
            onValueChange = { onUserChange(user.copy(email = it)) },
            label = { Text("Email Address *") },
            placeholder = { Text("your.email@example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = user.email.isEmpty() || !user.email.contains("@")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = user.phone,
            onValueChange = { onUserChange(user.copy(phone = it)) },
            label = { Text("Phone Number *") },
            placeholder = { Text("(123) 456-7890") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = user.phone.isEmpty() || user.phone.length < 10
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = user.govId,
            onValueChange = { onUserChange(user.copy(govId = it)) },
            label = { Text("Aadhaar Number *") },
            placeholder = { Text("XXXX-XXXX-XXXX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = user.govId.isEmpty() || user.govId.length < 12
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Password fields
        PasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password *",
            placeholder = "Create a secure password"
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm Password *",
            placeholder = "Confirm your password"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password requirements
        PasswordRequirements(password = password)
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    painter = painterResource(
                        if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    ),
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        isError = isError
    )
}

@Composable
fun PasswordRequirements(password: String) {
    val requirements = listOf(
        "At least 6 characters long" to (password.length >= 6),
        "At least one uppercase letter (A-Z)" to password.any { it.isUpperCase() },
        "At least one lowercase letter (a-z)" to password.any { it.isLowerCase() },
        "At least one number (0-9)" to password.any { it.isDigit() },
        "At least one special character (!@#\$%^&*)" to password.any { !it.isLetterOrDigit() }
    )

    Column {
        Text("Password Requirements:", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        requirements.forEach { (requirement, met) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (met) Color.Green else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = requirement,
                    color = if (met) Color.Green else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AddressStep(
    address: Address,
    onAddressChange: (Address) -> Unit
) {
    Column {
        Text(
            text = "Address & Emergency Contact",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text("This information helps us during emergency response", modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = address.line1,
            onValueChange = { onAddressChange(address.copy(line1 = it)) },
            label = { Text("Line 1 *") },
            placeholder = { Text("Enter your address line 1") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = address.line1.isEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address.line2,
            onValueChange = { onAddressChange(address.copy(line2 = it)) },
            label = { Text("Line 2") },
            placeholder = { Text("Enter your address line 2") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address.landmark,
            onValueChange = { onAddressChange(address.copy(landmark = it)) },
            label = { Text("Landmark") },
            placeholder = { Text("Enter a landmark") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address.pinCode?.toString() ?: "",
            onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    onAddressChange(address.copy(pinCode = it.toIntOrNull()))
                }
            },
            label = { Text("ZIP/Postal Code *") },
            placeholder = { Text("ZIP code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = address.pinCode == null || address.pinCode.toString().length != 6
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address.city,
            onValueChange = { onAddressChange(address.copy(city = it)) },
            label = { Text("City *") },
            placeholder = { Text("Your city") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = address.city.isEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address.state,
            onValueChange = { onAddressChange(address.copy(state = it)) },
            label = { Text("State/Province *") },
            placeholder = { Text("Your state") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = address.state.isEmpty()
        )
    }
}

@Composable
fun EmergencyContactStep(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Emergency Contact Information",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text("This contact will be notified in case of emergencies. They should not be at the same location as you.",
            modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Emergency Contact Name *") },
            placeholder = { Text("Contact person's name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = name.isEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) onPhoneChange(it) },
            label = { Text("Emergency Contact Phone *") },
            placeholder = { Text("Contact person's phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phone.isEmpty() || phone.length < 10
        )
    }
}

@Composable
fun MedicalInfoStep(
    medicalInfo: MedicalInfo,
    onMedicalInfoChange: (MedicalInfo) -> Unit
) {
    Column {
        Text(
            text = "Medical Information",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Date of Birth
        // In a real app, you would use a date picker
        OutlinedTextField(
            value = medicalInfo.dob ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(dob = it)) },
            label = { Text("Date of Birth *") },
            placeholder = { Text("MM/DD/YYYY") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = medicalInfo.dob.isNullOrEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Gender
        // In a real app, you would use radio buttons or a dropdown
        OutlinedTextField(
            value = medicalInfo.gender ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(gender = it)) },
            label = { Text("Gender *") },
            placeholder = { Text("male/female/other") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = medicalInfo.gender.isNullOrEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Height
        OutlinedTextField(
            value = medicalInfo.height?.toString() ?: "",
            onValueChange = {
                if (it.isEmpty() || it.toDoubleOrNull() != null) {
                    onMedicalInfoChange(medicalInfo.copy(height = it.toDoubleOrNull()))
                }
            },
            label = { Text("Height (cm) *") },
            placeholder = { Text("Enter your height in cm") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = medicalInfo.height == null
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Weight
        OutlinedTextField(
            value = medicalInfo.weight?.toString() ?: "",
            onValueChange = {
                if (it.isEmpty() || it.toDoubleOrNull() != null) {
                    onMedicalInfoChange(medicalInfo.copy(weight = it.toDoubleOrNull()))
                }
            },
            label = { Text("Weight (kg) *") },
            placeholder = { Text("Enter your weight in kg") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = medicalInfo.weight == null
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Blood Group
        OutlinedTextField(
            value = medicalInfo.bloodGroup ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(bloodGroup = it)) },
            label = { Text("Blood Group *") },
            placeholder = { Text("A+, A-, B+, etc.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = medicalInfo.bloodGroup.isNullOrEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Allergies
        OutlinedTextField(
            value = medicalInfo.allergies ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(allergies = it)) },
            label = { Text("Allergies *") },
            placeholder = { Text("List any allergies") },
            modifier = Modifier.fillMaxWidth(),
            isError = medicalInfo.allergies.isNullOrEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Medical Conditions
        OutlinedTextField(
            value = medicalInfo.medicalConditions ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(medicalConditions = it)) },
            label = { Text("Medical Conditions *") },
            placeholder = { Text("List any medical conditions") },
            modifier = Modifier.fillMaxWidth(),
            isError = medicalInfo.medicalConditions.isNullOrEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Medications
        OutlinedTextField(
            value = medicalInfo.medications ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(medications = it)) },
            label = { Text("Medications *") },
            placeholder = { Text("List current medications") },
            modifier = Modifier.fillMaxWidth(),
            isError = medicalInfo.medications.isNullOrEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Surgeries
        OutlinedTextField(
            value = medicalInfo.surgeries ?: "",
            onValueChange = { onMedicalInfoChange(medicalInfo.copy(surgeries = it)) },
            label = { Text("Surgeries *") },
            placeholder = { Text("List any past surgeries") },
            modifier = Modifier.fillMaxWidth(),
            isError = medicalInfo.surgeries.isNullOrEmpty()
        )

        // Pregnancy status (only shown if gender is female)
        if (medicalInfo.gender?.lowercase() == "female") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = medicalInfo.pregnancyStatus ?: "",
                onValueChange = { onMedicalInfoChange(medicalInfo.copy(pregnancyStatus = it)) },
                label = { Text("Pregnancy Status *") },
                placeholder = { Text("pregnant/not pregnant") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = medicalInfo.pregnancyStatus.isNullOrEmpty()
            )
        }
    }
}

// Helper functions to validate steps
private fun isStepValid(
    step: Int,
    user: User,
    password: String,
    confirmPassword: String,
    address: Address
): Boolean {
    return when (step) {
        1 -> user.name.isNotEmpty() &&
                user.email.isNotEmpty() && user.email.contains("@") &&
                user.phone.isNotEmpty() && user.phone.length >= 10 &&
                user.govId.isNotEmpty() && user.govId.length >= 12 &&
                password.length >= 6 && password == confirmPassword &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() }

        2 -> address.line1.isNotEmpty() &&
                address.pinCode != null && address.pinCode.toString().length == 6 &&
                address.city.isNotEmpty() &&
                address.state.isNotEmpty()

        3 -> true // Emergency contact validation happens in the next step

        else -> true
    }
}





