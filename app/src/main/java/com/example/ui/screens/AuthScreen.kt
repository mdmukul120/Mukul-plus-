package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.MediaRepository
import com.example.ui.components.MukulPlusLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotStatusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Big Mukul Plus Logo
        MukulPlusLogo(iconSize = 48, textSize = 28)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "প্রিমিয়াম ওটিটি স্ট্রিমিং প্ল্যাটফর্ম",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mode switch tabs: Login / Register
        Surface(
            color = CinemaSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Surface(
                    onClick = { isRegisterMode = false },
                    color = if (!isRegisterMode) BrandRed else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "লগইন (Login)",
                            color = if (!isRegisterMode) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Surface(
                    onClick = { isRegisterMode = true },
                    color = if (isRegisterMode) BrandRed else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "রেজিস্ট্রেশন (Register)",
                            color = if (isRegisterMode) Color.White else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Registration Name Field
        if (isRegisterMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("আপনার নাম (Full Name)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল অ্যাড্রেস (Email)") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড (Password)") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = TextMuted
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        // Confirm Password if Register
        if (isRegisterMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("পাসওয়ার্ড নিশ্চিত করুন (Confirm Password)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = authFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Forgot password link
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = {
                    forgotEmail = email
                    showForgotDialog = true
                }) {
                    Text(
                        text = "পাসওয়ার্ড ভুলে গেছেন? (Forgot Password)",
                        color = CyanAccent,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Submit Button
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "অনুগ্রহ করে ইমেইল ও পাসওয়ার্ড লিখুন", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isRegisterMode && password != confirmPassword) {
                    Toast.makeText(context, "পাসওয়ার্ড দুটি মেলেনি", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                coroutineScope.launch {
                    if (isRegisterMode) {
                        val result = authRepository.registerWithEmail(name, email, password)
                        if (result.isSuccess) {
                            Toast.makeText(context, "রেজিস্ট্রেশন সফল হয়েছে! ভেরিফিকেশন ইমেইল পাঠানো হয়েছে।", Toast.LENGTH_LONG).show()
                            onAuthSuccess()
                        } else {
                            Toast.makeText(context, "ত্রুটি: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val result = authRepository.loginWithEmail(email, password)
                        if (result.isSuccess) {
                            Toast.makeText(context, "স্বাগতম ${result.getOrNull()?.displayName}!", Toast.LENGTH_SHORT).show()
                            onAuthSuccess()
                        } else {
                            Toast.makeText(context, "লগইন ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    }
                    isLoading = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = if (isRegisterMode) "অ্যাকাউন্ট তৈরি করুন (Create Account)" else "লগইন করুন (Sign In)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = CinemaBorder)
            Text(
                text = "অথবা",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = CinemaBorder)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign In Button
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    authRepository.loginWithGoogle("user.google@gmail.com", "Google User")
                    Toast.makeText(context, "গুগল অ্যাকাউন্ট দিয়ে সাইন ইন সম্পন্ন!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, CinemaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = GoldRating,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("গুগল দিয়ে সাইন ইন (Sign in with Google)", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue as Guest / Skip
        TextButton(
            onClick = {
                authRepository.continueAsGuest()
                Toast.makeText(context, "অতিথি হিসেবে যুক্ত হয়েছেন!", Toast.LENGTH_SHORT).show()
                onAuthSuccess()
            }
        ) {
            Text(
                text = "অতিথি হিসেবে ব্রাউজ করুন (Skip / Continue as Guest) >",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Forgot Password Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = CinemaSurface,
            title = {
                Text(
                    text = "পাসওয়ার্ড রিসেট (Reset Password)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "আপনার রেজিস্টার্ড ইমেইল দিন। পাসওয়ার্ড রিসেট কোড এবং ভেরিফিকেশন লিংক পাঠানো হবে।",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("ইমেইল অ্যাড্রেস") },
                        colors = authFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (forgotStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = forgotStatusMessage!!,
                            color = GreenSuccess,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmail.isBlank()) {
                            Toast.makeText(context, "ইমেইল লিখুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        coroutineScope.launch {
                            val res = authRepository.sendPasswordResetEmail(forgotEmail)
                            forgotStatusMessage = res.getOrDefault("রিসেট লিংক পাঠানো হয়েছে!")
                            Toast.makeText(context, forgotStatusMessage, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("কোড পাঠান (Send Code)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("বন্ধ করুন (Close)", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ProfileScreen(
    user: UserProfile,
    authRepository: AuthRepository,
    mediaRepository: MediaRepository,
    onSelectMovie: (Long) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by mediaRepository.favorites.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = BrandRed,
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.displayName.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (user.isGuest) CinemaSurfaceVariant else BrandRedDark,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (user.isGuest) "GUEST VIEWER" else "MUKUL PLUS VIP MEMBER",
                            color = if (user.isGuest) GoldRating else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Stats summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("প্রিয় মুভি (Favorites)", color = TextMuted, fontSize = 11.sp)
                    Text("${favorites.size} টি", color = GoldRating, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("স্ট্রিমিং কোয়ালিটি", color = TextMuted, fontSize = 11.sp)
                    Text("1080p Ultra HD", color = CyanAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Favorites / My List Section
        Text(
            text = "আমার পছন্দের তালিকা (My Watchlist)",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (favorites.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এখনো কোনো মুভি তালিকায় যোগ করেননি।\nহোম বা মুভি পেজ থেকে লাভ আইকনে চাপ দিন।",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (favId in favorites) {
                    Surface(
                        onClick = { onSelectMovie(favId) },
                        color = CinemaSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = BrandRed)
                                Text("Movie #$favId", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { mediaRepository.toggleFavorite(favId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = {
                authRepository.logout()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CinemaSurfaceVariant),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFFF5252))
            Spacer(modifier = Modifier.width(8.dp))
            Text("লগআউট করুন (Log Out)", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CinemaSurface,
    unfocusedContainerColor = CinemaSurface,
    focusedBorderColor = BrandRed,
    unfocusedBorderColor = CinemaBorder,
    focusedLabelColor = BrandRed,
    unfocusedLabelColor = TextMuted,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
